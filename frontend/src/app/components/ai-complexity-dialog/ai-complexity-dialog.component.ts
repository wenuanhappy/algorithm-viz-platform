import { Component, effect, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlgorithmStore } from '../../store/algorithm.store';
import { AuthStore } from '../../store/auth.store';
import { AlgorithmService } from '../../services/algorithm.service';
import { ChatService } from '../../services/chat.service';
import { AlgorithmComplexityAnalysis, ChatSession } from '../../models/algorithm.models';

type ChatMessage = { role: 'user' | 'assistant', content: string };

@Component({
  selector: 'app-ai-complexity-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-complexity-dialog.component.html',
})
export class AiComplexityDialogComponent implements OnInit {
  algorithmCode = '';
  algorithmLanguage = 'pseudocode';
  algorithmCaseType = 'all';
  algorithmLoading = false;
  algorithmError: string | null = null;
  algorithmResult: AlgorithmComplexityAnalysis | null = null;

  conversation: ChatMessage[] = [];
  sessions: ChatSession[] = [];
  currentSessionId: number | null = null;
  sessionsLoading = false;
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  constructor(
    public store: AlgorithmStore,
    public auth: AuthStore,
    private algorithmService: AlgorithmService,
    private chatService: ChatService
  ) {
    effect(() => {
      if (this.store.aiDialogOpen()) {
        if (this.conversation.length === 0 && !this.algorithmCode && !this.currentSessionId) {
          this.algorithmCode = '你好！我该如何帮助你？';
        }
        this.loadSessions();
      }
    });
  }

  ngOnInit(): void {
    // 组件初始化时不做，因为 dialog 是通过 store 控制开关的，见 effect
  }

  loadSessions(): void {
    const user = this.auth.currentUser();
    if (!user) return;

    this.sessionsLoading = true;
    this.chatService.getUserSessions(user.id).subscribe({
      next: (sessions) => {
        this.sessions = sessions;
        this.sessionsLoading = false;
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Failed to load sessions:', err);
        this.sessionsLoading = false;
      }
    });
  }

  selectSession(session: ChatSession): void {
    this.currentSessionId = session.id;
    this.algorithmLoading = true;
    this.chatService.getSessionMessages(session.id).subscribe({
      next: (messages) => {
        this.conversation = messages.map(m => ({
          role: m.role as 'user' | 'assistant',
          content: m.content
        }));
        this.algorithmLoading = false;
        this.algorithmCode = '';
        this.algorithmResult = null;
        this.algorithmError = null; // 重置错误信息
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Failed to load messages:', err);
        this.algorithmLoading = false;
      }
    });
  }

  createNewChat(): void {
    this.currentSessionId = null;
    this.conversation = [];
    this.algorithmCode = ''; // 清空输入框
    this.algorithmResult = null;
    this.algorithmError = null;
  }

  deleteSession(event: Event, sessionId: number): void {
    event.stopPropagation();
    if (!confirm('确定要删除这个对话吗？')) return;

    this.chatService.deleteSession(sessionId).subscribe({
      next: () => {
        this.sessions = this.sessions.filter(s => s.id !== sessionId);
        if (this.currentSessionId === sessionId) {
          this.createNewChat();
        }
      }
    });
  }

  closeAlgorithmDialog(): void {
    this.store.closeAiComplexityDialog();
    // 不再自动清空，保留当前状态，除非用户手动点“新对话”
  }

  analyzeAlgorithmComplexity(): void {
    const code = this.algorithmCode.trim();
    if (!code) {
      this.algorithmError = '请输入内容';
      return;
    }
    this.algorithmError = null;

    const user = this.auth.currentUser();
    if (!user) {
      this.algorithmError = '请先登录';
      return;
    }

    // 如果没有当前会话，先创建一个
    if (!this.currentSessionId) {
      const title = code.length > 20 ? code.substring(0, 20) + '...' : code;
      this.chatService.createSession(user.id, title).subscribe({
        next: (session) => {
          this.currentSessionId = session.id;
          this.sessions.unshift(session);
          this.executeAnalysis(code);
        },
        error: () => {
          this.algorithmError = '创建会话失败';
        }
      });
    } else {
      this.executeAnalysis(code);
    }
  }

  private executeAnalysis(code: string): void {
    // 追加用户消息
    this.conversation.push({ role: 'user', content: code });
    this.algorithmLoading = true;
    this.scrollToBottom();

    this.algorithmService.analyzeAlgorithmComplexity({
      code: code, // 直接发当前内容，后端会处理上下文
      language: this.algorithmLanguage,
      caseType: this.algorithmCaseType,
      sessionId: this.currentSessionId || undefined
    }).subscribe({
      next: (result) => {
        this.algorithmResult = result;
        const answer = result?.rawText || '（无返回内容）';
        this.conversation.push({ role: 'assistant', content: answer });
        this.algorithmLoading = false;
        this.algorithmCode = '';
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('AI chat failed:', err);
        this.algorithmError = err?.error?.message || 'AI 对话失败，请稍后重试';
        this.algorithmLoading = false;
      },
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      try {
        const el = document.querySelector('.chat-messages-container');
        if (el) {
          el.scrollTop = el.scrollHeight;
        }
      } catch (err) {}
    }, 100);
  }

  // 后端现在负责保存对话，前端不需要手动拼接上下文发送了
}