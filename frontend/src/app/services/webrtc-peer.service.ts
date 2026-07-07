import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { CompetitionRealtimeMessage } from '../models/competition.models';
import { environment } from '../../environments/environment';

type SignalingMessage =
  | { type: 'join-room'; roomId: string; userId: number; displayName: string }
  | { type: 'peer-present'; roomId: string; peerUserId: number }
  | { type: 'offer'; roomId: string; from: number; sdp: RTCSessionDescriptionInit }
  | { type: 'answer'; roomId: string; from: number; sdp: RTCSessionDescriptionInit }
  | { type: 'ice-candidate'; roomId: string; from: number; candidate: RTCIceCandidateInit }
  | { type: 'peer-left'; roomId: string };

@Injectable({ providedIn: 'root' })
export class WebrtcPeerService {
  readonly messages$ = new Subject<CompetitionRealtimeMessage>();
  readonly status$ = new Subject<string>();

  private socket: WebSocket | null = null;
  private peer: RTCPeerConnection | null = null;
  private channel: RTCDataChannel | null = null;
  private roomId = '';
  private userId = 0;
  private displayName = '';
  private pendingCandidates: RTCIceCandidateInit[] = [];
  private makingOffer = false;
  private peerUserId: number | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private connectionTimer: ReturnType<typeof setTimeout> | null = null;
  private signalingReconnectTimer: ReturnType<typeof setTimeout> | null = null;

  connect(roomId: string, userId: number, displayName: string): void {
    this.disconnect();
    this.roomId = roomId;
    this.userId = userId;
    this.displayName = displayName;
    this.peer = this.createPeer();
    this.connectSignaling();
  }

  send(message: CompetitionRealtimeMessage): void {
    if (this.channel?.readyState === 'open') {
      this.channel.send(JSON.stringify(message));
    }
  }

  disconnect(): void {
    const channel = this.channel;
    const peer = this.peer;
    const socket = this.socket;
    this.channel = null;
    this.peer = null;
    this.socket = null;
    this.roomId = '';
    this.userId = 0;
    this.displayName = '';
    channel?.close();
    peer?.close();
    socket?.close();
    this.pendingCandidates = [];
    this.makingOffer = false;
    this.peerUserId = null;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    if (this.connectionTimer) clearTimeout(this.connectionTimer);
    this.connectionTimer = null;
    if (this.signalingReconnectTimer) clearTimeout(this.signalingReconnectTimer);
    this.signalingReconnectTimer = null;
  }

  private connectSignaling(): void {
    if (!this.roomId || this.socket) return;
    const socket = new WebSocket(this.signalingUrl());
    this.socket = socket;

    socket.onopen = () => {
      if (this.socket !== socket) return;
      this.status$.next(this.channel?.readyState === 'open' ? 'datachannel-open' : 'signaling-connected');
      this.sendSignal({
        type: 'join-room',
        roomId: this.roomId,
        userId: this.userId,
        displayName: this.displayName,
      });
    };
    socket.onmessage = event => {
      if (this.socket !== socket) return;
      try {
        void this.handleSignal(JSON.parse(event.data) as SignalingMessage)
          .catch(() => this.status$.next('signaling-message-error'));
      } catch {
        this.status$.next('signaling-message-error');
      }
    };
    socket.onclose = () => {
      if (this.socket !== socket) return;
      this.socket = null;
      if (this.channel?.readyState !== 'open') this.status$.next('signaling-closed');
      this.scheduleSignalingReconnect();
    };
    socket.onerror = () => {
      if (this.socket === socket && this.channel?.readyState !== 'open') {
        this.status$.next('signaling-error');
      }
    };
  }

  private scheduleSignalingReconnect(): void {
    if (!this.roomId || this.signalingReconnectTimer) return;
    this.signalingReconnectTimer = setTimeout(() => {
      this.signalingReconnectTimer = null;
      this.connectSignaling();
    }, 1500);
  }

  private createPeer(): RTCPeerConnection {
    const peer = new RTCPeerConnection({
      iceServers: environment.rtcIceServers,
    });

    peer.onicecandidate = event => {
      if (this.peer !== peer) return;
      if (event.candidate) {
        this.sendSignal({
          type: 'ice-candidate',
          roomId: this.roomId,
          from: this.userId,
          candidate: event.candidate.toJSON(),
        });
      }
    };
    peer.onconnectionstatechange = () => {
      if (this.peer !== peer) return;
      this.status$.next(peer.connectionState);
      if (peer.connectionState === 'disconnected' || peer.connectionState === 'failed') {
        this.scheduleIceRestart();
      }
      if (peer.connectionState === 'connected' && this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
    };
    peer.ondatachannel = event => {
      if (this.peer === peer) this.attachChannel(event.channel);
    };
    return peer;
  }

  private async handleSignal(message: SignalingMessage): Promise<void> {
    if (!this.peer || message.roomId !== this.roomId) return;

    if (message.type === 'join-room') {
      if (message.userId !== this.userId) {
        await this.handlePeerAvailable(message.userId);
      }
      return;
    }

    if (message.type === 'peer-present') {
      await this.handlePeerAvailable(message.peerUserId);
      return;
    }

    if (message.type === 'offer') {
      await this.peer.setRemoteDescription(message.sdp);
      await this.flushPendingCandidates();
      const answer = await this.peer.createAnswer();
      await this.peer.setLocalDescription(answer);
      this.sendSignal({ type: 'answer', roomId: this.roomId, from: this.userId, sdp: answer });
      return;
    }

    if (message.type === 'answer') {
      await this.peer.setRemoteDescription(message.sdp);
      await this.flushPendingCandidates();
      return;
    }

    if (message.type === 'ice-candidate') {
      if (this.peer.remoteDescription) {
        await this.peer.addIceCandidate(message.candidate);
      } else {
        this.pendingCandidates.push(message.candidate);
      }
      return;
    }

    if (message.type === 'peer-left') {
      this.messages$.next({ type: 'opponent-left' });
    }
  }

  private async createOffer(iceRestart = false): Promise<void> {
    if (!this.peer || this.makingOffer) return;
    this.makingOffer = true;
    try {
      if (!this.channel || this.channel.readyState === 'closed') {
        this.attachChannel(this.peer.createDataChannel('competition'));
      }
      const offer = await this.peer.createOffer({ iceRestart });
      await this.peer.setLocalDescription(offer);
      this.sendSignal({ type: 'offer', roomId: this.roomId, from: this.userId, sdp: offer });
    } finally {
      this.makingOffer = false;
    }
  }

  private async handlePeerAvailable(peerUserId: number): Promise<void> {
    this.peerUserId = peerUserId;
    if (this.channel?.readyState === 'open') {
      this.status$.next('datachannel-open');
      return;
    }
    this.status$.next('peer-joined');
    if (this.connectionTimer) clearTimeout(this.connectionTimer);
    this.connectionTimer = setTimeout(() => {
      if (this.channel?.readyState !== 'open') this.status$.next('webrtc-timeout');
    }, 12000);
    if (this.userId < peerUserId) {
      await this.createOffer();
    }
  }

  private scheduleIceRestart(): void {
    if (this.reconnectTimer || this.peerUserId === null || this.userId > this.peerUserId) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.peer?.connectionState === 'disconnected' || this.peer?.connectionState === 'failed') {
        void this.createOffer(true).catch(() => this.status$.next('ice-restart-error'));
      }
    }, 3000);
  }

  private attachChannel(channel: RTCDataChannel): void {
    if (this.channel && this.channel !== channel && this.channel.readyState !== 'closed') {
      this.channel.close();
    }
    this.channel = channel;
    channel.onopen = () => {
      if (this.channel !== channel) return;
      if (this.connectionTimer) clearTimeout(this.connectionTimer);
      this.connectionTimer = null;
      this.status$.next('datachannel-open');
    };
    channel.onclose = () => {
      if (this.channel === channel) this.status$.next('datachannel-closed');
    };
    channel.onmessage = event => {
      if (this.channel !== channel) return;
      try {
        if (typeof event.data !== 'string' || event.data.length > 4096) {
          this.status$.next('datachannel-message-error');
          return;
        }
        this.messages$.next(JSON.parse(event.data) as CompetitionRealtimeMessage);
      } catch {
        this.status$.next('datachannel-message-error');
      }
    };
  }

  private async flushPendingCandidates(): Promise<void> {
    if (!this.peer?.remoteDescription) return;
    const candidates = this.pendingCandidates.splice(0);
    for (const candidate of candidates) {
      await this.peer.addIceCandidate(candidate);
    }
  }

  private sendSignal(message: SignalingMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }

  private signalingUrl(): string {
    const apiUrl = environment.apiUrl.startsWith('http')
      ? environment.apiUrl
      : `${window.location.origin}${environment.apiUrl}`;
    const url = new URL(apiUrl);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    url.pathname = '/ws/signaling';
    url.search = '';
    return url.toString();
  }
}
