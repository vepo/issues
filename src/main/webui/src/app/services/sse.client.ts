import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, Subject } from 'rxjs';

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
  private abortController: AbortController | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private activeUrl: string | null = null;
  private currentEvent: ServerSideEvent | null = null;
  private contentType: string | null = null;

  connect(url: string): Observable<ServerSideEvent> {
    const token = this.authService.getToken();
    if (!token) {
      return EMPTY;
    }

    this.open = true;
    this.activeUrl = url;
    this.startConnection(url, token);
    return this.dataSubject.asObservable();
  }

  close(): void {
    this.open = false;
    this.activeUrl = null;
    this.clearReconnect();
    this.abortController?.abort();
    this.abortController = null;
  }

  private startConnection(url: string, token: string): void {
    this.clearReconnect();
    this.abortController?.abort();
    this.abortController = new AbortController();
    void this.connectWithFetchAPI(url, token, this.abortController.signal);
  }

  private clearReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private scheduleReconnect(url: string, token: string): void {
    if (!this.open || this.reconnectTimeout) {
      return;
    }

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null;
      if (this.open && this.activeUrl === url) {
        this.startConnection(url, token);
      }
    }, 3000);
  }

  private async connectWithFetchAPI(url: string, token: string, signal: AbortSignal): Promise<void> {
    try {
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream'
        },
        signal,
        cache: 'no-store'
      });

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status}`);
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
        this.scheduleReconnect(url, token);
      }
    } catch (error) {
      if (signal.aborted || !this.open) {
        return;
      }

      console.warn('SSE connection lost; retrying shortly.', error);
      this.scheduleReconnect(url, token);
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
