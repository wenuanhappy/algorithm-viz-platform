import { AlgorithmId } from './algorithm.models';

export interface CompetitionQuestion {
  id: string;
  algorithm: AlgorithmId;
  title: string;
  prompt: string;
  options: string[];
  input: Record<string, unknown>;
  points: number;
}

export interface CompetitionPlayer {
  userId: number;
  displayName: string;
  ready: boolean;
  score: number;
  correctCount: number;
  submittedCount: number;
  finished: boolean;
  forfeited: boolean;
  totalTimeMs: number;
}

export interface CompetitionRoom {
  roomId: string;
  algorithm: AlgorithmId;
  status: 'waiting' | 'ready-check' | 'playing' | 'finished';
  createdAt: string;
  startedAt?: string;
  lastActivityAt: string;
  questionCount: number;
  players: CompetitionPlayer[];
  questions: CompetitionQuestion[];
}

export interface CreateCompetitionRoomRequest {
  algorithm: AlgorithmId;
  userId: number;
  displayName: string;
}

export interface JoinCompetitionRoomRequest {
  userId: number;
  displayName: string;
}

export interface CompetitionSubmitRequest {
  userId: number;
  questionId: string;
  answer: string;
}

export interface CompetitionSubmitResponse {
  correct: boolean;
  correctAnswer: string;
  awardedPoints: number;
  score: number;
  duplicate?: boolean;
}

export type CompetitionRealtimeMessage =
  | { type: 'ready'; userId: number; displayName: string }
  | { type: 'progress'; userId: number; questionIndex: number }
  | { type: 'submitted'; userId: number; questionIndex: number; correct: boolean }
  | { type: 'chat'; userId: number; displayName: string; content: string }
  | { type: 'opponent-left'; userId?: number };
