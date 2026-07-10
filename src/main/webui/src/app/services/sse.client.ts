import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, Subject, firstValueFrom } from 'rxjs';

import { AuthService } from './auth.service';

export interface ServerSideEvent {
  id: string;
  data: unknown;
}

@Injectable({
  providedIn: 'root'
})
export class ServerSideEventsClient {
  private readonly authService = inject(AuthService);

  private open = false;
  private readonly dataSubject = new Subject<ServerSideEvent>();
  private readonly reconnectedSubject = new Subject<void>();
  private abortController: AbortController | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private activeUrl: string | null = null;
  private currentEvent: ServerSideEvent | null = null;
  private contentType: string | null = null;

  connect(url: string): Observable<ServerSideEvent> {
    if (!this.authService.getToken()) {
      return EMPTY;
    }

    this.open = true;
    this.activeUrl = url;
    void this.startConnection(url, false);
    return this.dataSubject.asObservable();
  }

  reconnected(): Observable<void> {
    return this.reconnectedSubject.asObservable();
  }

  close(): void {
    this.open = false;
    this.activeUrl = null;
    this.clearReconnect();
    this.abortController?.abort();
    this.abortController = null;
  }

  private async startConnection(url: string, isReconnect: boolean): Promise<void> {
    this.clearReconnect();
    this.abortController?.abort();
    this.abortController = new AbortController();

    const token = await this.resolveAccessToken();
    if (!token || !this.open || this.activeUrl !== url) {
      return;
    }

    await this.connectWithFetchAPI(url, token, this.abortController.signal, isReconnect);
  }

  private async resolveAccessToken(): Promise<string | null> {
    const token = this.authService.getToken();
    if (token) {
      return token;
    }
    try {
      await firstValueFrom(this.authService.refreshToken());
      return this.authService.getToken();
    } catch {
      return null;
    }
  }

  private clearReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private scheduleReconnect(url: string): void {
    if (!this.open || this.reconnectTimeout) {
      return;
    }

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      if (this.open && this.activeUrl === url) {
        void this.startConnection(url, true);
      }
    }, 3000);
  }

  private async connectWithFetchAPI(
    url: string,
    token: string,
    signal: AbortSignal,
    isReconnect: boolean,
  ): Promise<void> {
    try {
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream'
        },
        signal,
        cache: 'no-store'
      });

      if (response.status === 401) {
        try {
          await firstValueFrom(this.authService.refreshToken());
        } catch {
          /* fall through to reconnect schedule */
        }
        if (this.open && !signal.aborted) {
          this.scheduleReconnect(url);
        }
        return;
      }

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status}`);
      }

      if (isReconnect) {
        this.reconnectedSubject.next();
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('SSE response has no body');
      }

      while (this.open && !signal.aborted) {
        const { value, done } = await reader.read();
        if (done) {
          break;
        }

        this.parseAndEmitSSEEvents(decoder.decode(value));
      }

      if (this.open && !signal.aborted) {
        this.scheduleReconnect(url);
      }
    } catch (error) {
      if (signal.aborted || !this.open) {
        return;
      }

      console.warn('SSE connection lost; retrying shortly.', error);
      this.scheduleReconnect(url);
    }
  }

  private parseAndEmitSSEEvents(data: string): void {
    const lines = data.split('\n');

    if (!this.currentEvent) {
      this.currentEvent = { id: '', data: null };
    }

    for (const line of lines) {
      if (line.startsWith('content-type:')) {
        this.contentType = line.substring(13).trim();
      } else if (line.startsWith('id:')) {
        this.currentEvent.id = line.substring(3).trim();
      } else if (line.startsWith('data:')) {
        try {
          switch (this.contentType) {
            case 'application/json':
            default:
              this.currentEvent.data = JSON.parse(line.substring(5).trim());
              break;
          }
          this.flush();
        } catch (e) {
          console.error('Error parsing SSE data:', e);
        }
      }

      this.flush();
    }
  }

  private flush(): void {
    if (
      this.currentEvent
      && this.currentEvent.data
      && typeof this.currentEvent.data === 'object'
      && Object.keys(this.currentEvent.data).length > 0
    ) {
      this.dataSubject.next(this.currentEvent);
      this.currentEvent = { id: '', data: null };
    }
  }
}
