import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CompetitionPlayer,
  CompetitionRoom,
  CompetitionSubmitRequest,
  CompetitionSubmitResponse,
  CreateCompetitionRoomRequest,
  JoinCompetitionRoomRequest,
} from '../models/competition.models';

@Injectable({ providedIn: 'root' })
export class CompetitionService {
  private readonly base = environment.apiUrl + '/competition';

  constructor(private http: HttpClient) {}

  createRoom(request: CreateCompetitionRoomRequest): Observable<CompetitionRoom> {
    return this.http.post<CompetitionRoom>(`${this.base}/rooms`, request);
  }

  joinRoom(roomId: string, request: JoinCompetitionRoomRequest): Observable<CompetitionRoom> {
    return this.http.post<CompetitionRoom>(`${this.base}/rooms/${roomId}/join`, request);
  }

  ready(roomId: string, userId: number): Observable<CompetitionRoom> {
    return this.http.post<CompetitionRoom>(`${this.base}/rooms/${roomId}/ready`, { userId });
  }

  getRoom(roomId: string): Observable<CompetitionRoom> {
    return this.http.get<CompetitionRoom>(`${this.base}/rooms/${roomId}`);
  }

  leaveRoom(roomId: string, userId: number): Observable<CompetitionRoom> {
    return this.http.post<CompetitionRoom>(`${this.base}/rooms/${roomId}/leave`, { userId });
  }

  submit(roomId: string, request: CompetitionSubmitRequest): Observable<CompetitionSubmitResponse> {
    return this.http.post<CompetitionSubmitResponse>(`${this.base}/rooms/${roomId}/submit`, request);
  }

  result(roomId: string): Observable<{ rankings: CompetitionPlayer[] }> {
    return this.http.get<{ rankings: CompetitionPlayer[] }>(`${this.base}/rooms/${roomId}/result`);
  }
}
