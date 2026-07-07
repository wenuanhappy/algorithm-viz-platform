import { Injectable, computed, signal } from '@angular/core';
import { AuthStore } from './auth.store';
import { AlgorithmStore } from './algorithm.store';
import { AlgorithmId } from '../models/algorithm.models';
import {
  CompetitionPlayer,
  CompetitionRealtimeMessage,
  CompetitionRoom,
  CompetitionSubmitResponse,
} from '../models/competition.models';
import { CompetitionService } from '../services/competition.service';
import { WebrtcPeerService } from '../services/webrtc-peer.service';
import { ALGORITHM_GROUPS } from '../data/algorithm-catalog';

@Injectable({ providedIn: 'root' })
export class CompetitionStore {
  readonly algorithmGroups = ALGORITHM_GROUPS.filter(group => group.category !== 'Web3D');
  selectedAlgorithm = signal<AlgorithmId>('quick-sort');
  room = signal<CompetitionRoom | null>(null);
  joinCode = signal('');
  currentQuestionIndex = signal(0);
  selectedAnswer = signal('');
  lastSubmit = signal<CompetitionSubmitResponse | null>(null);
  rankings = signal<CompetitionPlayer[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  connectionStatus = signal('offline');
  opponentStatus = signal('等待对手加入');
  chatDraft = signal('');
  messages = signal<string[]>([]);
  roomCodeCopied = signal(false);
  showLeaveConfirmation = signal(false);

  private refreshTimer: ReturnType<typeof setInterval> | null = null;
  private resultLoading = false;

  currentQuestion = computed(() => {
    const room = this.room();
    return room?.questions[this.currentQuestionIndex()] ?? null;
  });

  isRoomOwner = computed(() => this.room()?.players[0]?.userId === this.auth.currentUser()?.id);
  me = computed(() => this.room()?.players.find(player => player.userId === this.auth.currentUser()?.id) ?? null);
  opponent = computed(() => this.room()?.players.find(player => player.userId !== this.auth.currentUser()?.id) ?? null);
  hasRoom = computed(() => this.room() !== null);
  isFinished = computed(() => this.room()?.status === 'finished' || this.rankings().length > 0);
  questionCount = computed(() => this.room()?.questionCount ?? 0);
  myProgress = computed(() => this.me()?.submittedCount ?? 0);
  opponentProgress = computed(() => this.opponent()?.submittedCount ?? 0);
  completedAllQuestions = computed(() => this.questionCount() > 0 && this.myProgress() >= this.questionCount());
  waitingForOpponentResult = computed(() => {
    const room = this.room();
    return room?.status === 'playing' && this.completedAllQuestions() && this.rankings().length === 0;
  });
  roomStatusLabel = computed(() => {
    const labels: Record<CompetitionRoom['status'], string> = {
      waiting: '等待对手',
      'ready-check': '等待双方准备',
      playing: '比赛进行中',
      finished: '比赛已结束',
    };
    return this.room() ? labels[this.room()!.status] : '';
  });
  connectionStatusLabel = computed(() => {
    const status = this.connectionStatus();
    if (status === 'datachannel-open' || status === 'connected') return '实时连接正常';
    if (status === 'webrtc-timeout' || status === 'failed' || status.includes('error')) {
      return '点对点连接失败（答题不受影响）';
    }
    if (status === 'peer-joined' || status === 'connecting') return '已发现对手，正在协商连接';
    if (status === 'signaling-connected') return '信令已连接，等待对手';
    if (status === 'offline' || status === 'closed' || status.endsWith('-closed')) return '实时连接已断开';
    return '正在建立实时连接';
  });
  canChat = computed(() => this.connectionStatus() === 'datachannel-open');
  algorithmLabel = computed(() => {
    return ALGORITHM_GROUPS
      .flatMap(group => group.items)
      .find(item => item.id === this.selectedAlgorithm())?.label ?? this.selectedAlgorithm();
  });

  constructor(
    private competitionService: CompetitionService,
    private webRtc: WebrtcPeerService,
    private auth: AuthStore,
    private algorithmStore: AlgorithmStore,
  ) {
    this.webRtc.messages$.subscribe(message => this.handleRealtimeMessage(message));
    this.webRtc.status$.subscribe(status => {
      this.connectionStatus.set(status);
      if (status === 'peer-joined') {
        this.refreshRoom();
      }
    });
    this.restoreActiveRoom();
  }

  createRoom(): void {
    const user = this.requireUser();
    if (!user) return;

    this.isLoading.set(true);
    this.error.set(null);
    this.competitionService.createRoom({
      algorithm: this.selectedAlgorithm(),
      userId: user.id,
      displayName: user.displayName,
    }).subscribe({
      next: room => this.enterRoom(room),
      error: err => this.fail(err, '创建房间失败'),
    });
  }

  joinRoom(): void {
    const user = this.requireUser();
    if (!user) return;
    const roomId = this.joinCode().trim().toUpperCase();
    if (roomId.length !== 6) {
      this.error.set('请输入 6 位房间号');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);
    this.competitionService.joinRoom(roomId, {
      userId: user.id,
      displayName: user.displayName,
    }).subscribe({
      next: room => this.enterRoom(room),
      error: err => this.fail(err, '加入房间失败'),
    });
  }

  updateJoinCode(value: string): void {
    this.joinCode.set(value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 6));
    this.error.set(null);
  }

  selectCompetitionAlgorithm(algorithm: AlgorithmId): void {
    if (this.room() || algorithm === 'data-structure-3d') return;
    this.selectedAlgorithm.set(algorithm);
    this.algorithmStore.setAlgorithm(algorithm);
    this.error.set(null);
  }

  ready(): void {
    const room = this.room();
    const user = this.requireUser();
    if (!room || !user || this.me()?.ready) return;

    this.isLoading.set(true);
    this.competitionService.ready(room.roomId, user.id).subscribe({
      next: updated => {
        this.room.set(updated);
        this.isLoading.set(false);
        this.webRtc.send({ type: 'ready', userId: user.id, displayName: user.displayName });
      },
      error: err => this.fail(err, '准备失败'),
    });
  }

  chooseAnswer(answer: string): void {
    if (this.lastSubmit()) return;
    this.selectedAnswer.set(answer);
  }

  submitCurrentAnswer(): void {
    const room = this.room();
    const question = this.currentQuestion();
    const user = this.requireUser();
    if (!room || !question || !user || !this.selectedAnswer()) return;

    this.isLoading.set(true);
    this.error.set(null);
    this.competitionService.submit(room.roomId, {
      userId: user.id,
      questionId: question.id,
      answer: this.selectedAnswer(),
    }).subscribe({
      next: result => {
        this.lastSubmit.set(result);
        this.isLoading.set(false);
        this.webRtc.send({
          type: 'submitted',
          userId: user.id,
          questionIndex: this.currentQuestionIndex(),
          correct: result.correct,
        });
        this.refreshRoom();
      },
      error: err => this.fail(err, '提交失败'),
    });
  }

  nextQuestion(): void {
    const room = this.room();
    const user = this.auth.currentUser();
    if (!room || !user) return;

    if (this.currentQuestionIndex() >= room.questionCount - 1) {
      this.selectedAnswer.set('');
      this.lastSubmit.set(null);
      this.refreshRoom();
      this.opponentStatus.set('已完成全部题目，等待对手完成');
      return;
    }

    this.currentQuestionIndex.update(index => index + 1);
    this.selectedAnswer.set('');
    this.lastSubmit.set(null);
    this.webRtc.send({
      type: 'progress',
      userId: user.id,
      questionIndex: this.currentQuestionIndex(),
    });
  }

  sendChat(): void {
    const user = this.auth.currentUser();
    const content = this.chatDraft().trim().slice(0, 300);
    if (!user || !content || !this.canChat()) return;

    this.appendMessage(`${user.displayName}: ${content}`);
    this.webRtc.send({ type: 'chat', userId: user.id, displayName: user.displayName, content });
    this.chatDraft.set('');
  }

  requestLeave(): void {
    if (this.room()?.status === 'playing') {
      this.showLeaveConfirmation.set(true);
      return;
    }
    this.leaveRoom();
  }

  leaveRoom(): void {
    const room = this.room();
    const user = this.auth.currentUser();
    if (room && user) {
      this.competitionService.leaveRoom(room.roomId, user.id).subscribe({ error: () => {} });
    }
    this.clearLocalRoom();
  }

  cancelLeave(): void {
    this.showLeaveConfirmation.set(false);
  }

  playAgain(): void {
    this.clearLocalRoom();
    this.createRoom();
  }

  async copyRoomCode(): Promise<void> {
    const roomId = this.room()?.roomId;
    if (!roomId) return;
    try {
      await navigator.clipboard.writeText(roomId);
      this.roomCodeCopied.set(true);
      setTimeout(() => this.roomCodeCopied.set(false), 1800);
    } catch {
      this.error.set('复制失败，请手动选择房间号。');
    }
  }

  refreshRoom(): void {
    const room = this.room();
    if (!room) return;
    const previousStatus = room.status;

    this.competitionService.getRoom(room.roomId).subscribe({
      next: updated => {
        this.room.set(updated);
        if (previousStatus !== 'playing' && updated.status === 'playing') {
          this.currentQuestionIndex.set(Math.min(this.myProgress(), updated.questionCount - 1));
          this.selectedAnswer.set('');
          this.lastSubmit.set(null);
        }
        if (updated.status === 'finished') {
          this.loadResult();
        }
      },
      error: err => this.handleRefreshError(err),
    });
  }

  private enterRoom(room: CompetitionRoom): void {
    const user = this.requireUser();
    if (!user) return;

    this.room.set(room);
    this.selectedAlgorithm.set(room.algorithm);
    const me = room.players.find(player => player.userId === user.id);
    this.currentQuestionIndex.set(Math.min(me?.submittedCount ?? 0, Math.max(0, room.questionCount - 1)));
    this.selectedAnswer.set('');
    this.lastSubmit.set(null);
    this.rankings.set([]);
    this.messages.set([]);
    this.isLoading.set(false);
    this.resultLoading = false;
    sessionStorage.setItem(this.activeRoomKey(user.id), room.roomId);
    this.webRtc.connect(room.roomId, user.id, user.displayName);
    this.startRoomPolling();
    if (room.status === 'finished') {
      this.loadResult();
    }
  }

  private loadResult(): void {
    const room = this.room();
    if (!room || this.resultLoading || this.rankings().length > 0) return;

    this.resultLoading = true;
    this.competitionService.result(room.roomId).subscribe({
      next: result => {
        this.rankings.set(result.rankings);
        this.resultLoading = false;
        this.stopRoomPolling();
      },
      error: err => {
        this.resultLoading = false;
        this.fail(err, '获取结果失败');
      },
    });
  }

  private startRoomPolling(): void {
    this.stopRoomPolling();
    this.refreshTimer = setInterval(() => this.refreshRoom(), 750);
  }

  private stopRoomPolling(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private restoreActiveRoom(): void {
    const user = this.auth.currentUser();
    if (!user) return;
    const roomId = sessionStorage.getItem(this.activeRoomKey(user.id));
    if (!roomId) return;

    this.isLoading.set(true);
    this.competitionService.getRoom(roomId).subscribe({
      next: room => {
        if (!room.players.some(player => player.userId === user.id)) {
          this.clearLocalRoom();
          return;
        }
        this.algorithmStore.setActivePanel('competition');
        this.enterRoom(room);
      },
      error: () => {
        this.clearLocalRoom();
        this.error.set('之前的竞赛房间已失效，请重新创建或加入房间。');
      },
    });
  }

  private handleRefreshError(err: unknown): void {
    const response = err as { status?: number };
    if (response.status === 400 || response.status === 404) {
      this.clearLocalRoom();
      this.error.set('竞赛房间已失效，请重新创建或加入房间。');
    }
  }

  private clearLocalRoom(): void {
    const user = this.auth.currentUser();
    if (user) sessionStorage.removeItem(this.activeRoomKey(user.id));
    this.stopRoomPolling();
    this.webRtc.disconnect();
    this.room.set(null);
    this.rankings.set([]);
    this.messages.set([]);
    this.currentQuestionIndex.set(0);
    this.selectedAnswer.set('');
    this.lastSubmit.set(null);
    this.connectionStatus.set('offline');
    this.opponentStatus.set('等待对手加入');
    this.showLeaveConfirmation.set(false);
    this.resultLoading = false;
    this.isLoading.set(false);
  }

  private activeRoomKey(userId: number): string {
    return `algorithm-viz-active-competition-${userId}`;
  }

  private handleRealtimeMessage(message: CompetitionRealtimeMessage): void {
    if (message.type === 'ready') {
      this.opponentStatus.set(`${this.opponent()?.displayName ?? '对手'} 已准备`);
      this.refreshRoom();
      return;
    }

    if (message.type === 'progress') {
      const questionIndex = this.safeQuestionIndex(message.questionIndex);
      this.opponentStatus.set(`对手进入第 ${questionIndex + 1} 题`);
      return;
    }

    if (message.type === 'submitted') {
      const questionIndex = this.safeQuestionIndex(message.questionIndex);
      this.opponentStatus.set(`对手提交了第 ${questionIndex + 1} 题`);
      this.refreshRoom();
      return;
    }

    if (message.type === 'chat') {
      if (typeof message.content !== 'string') return;
      const displayName = this.opponent()?.displayName ?? '对手';
      this.appendMessage(`${displayName}: ${message.content.slice(0, 300)}`);
      return;
    }

    if (message.type === 'opponent-left') {
      this.opponentStatus.set('对手已离开');
    }
  }

  private requireUser() {
    const user = this.auth.currentUser();
    if (!user) {
      this.error.set('请先登录');
      return null;
    }
    return user;
  }

  private appendMessage(message: string): void {
    this.messages.update(items => [...items, message].slice(-100));
  }

  private safeQuestionIndex(value: number): number {
    if (!Number.isFinite(value)) return 0;
    return Math.max(0, Math.min(Math.trunc(value), Math.max(0, this.questionCount() - 1)));
  }

  private fail(err: unknown, fallback: string): void {
    const response = err as { error?: { message?: string } };
    this.error.set(response.error?.message ?? fallback);
    this.isLoading.set(false);
  }
}
