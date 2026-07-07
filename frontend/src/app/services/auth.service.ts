import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthUser {
  id: number;
  username: string;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = environment.apiUrl + '/auth';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.base}/login`, { username, password });
  }

  register(username: string, displayName: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.base}/register`, { username, displayName, password });
  }
}
