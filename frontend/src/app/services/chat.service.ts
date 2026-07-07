import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatSession, ChatMessage } from '../models/algorithm.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly base = environment.apiUrl + '/chat';

  constructor(private http: HttpClient) {}

  createSession(userId: number, title: string): Observable<ChatSession> {
    return this.http.post<ChatSession>(`${this.base}/sessions`, { userId, title });
  }

  getUserSessions(userId: number): Observable<ChatSession[]> {
    return this.http.get<ChatSession[]>(`${this.base}/sessions?userId=${userId}`);
  }

  getSessionMessages(sessionId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base}/sessions/${sessionId}/messages`);
  }

  deleteSession(sessionId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/sessions/${sessionId}`);
  }
}
