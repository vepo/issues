import { Injectable, NgZone, inject } from '@angular/core';
import { map, Observable, Subject } from 'rxjs';
import { NotificationApi } from '../generated/api/notification.service';
import { UserNotificationEvent } from '../generated/model/userNotificationEvent';
import { UpdateNotificationStatusReadRequest } from '../generated/model/updateNotificationStatusReadRequest';
import { asLoaded, Loaded } from '../core/required-types';
import { ServerSideEventsClient } from './sse.client';

export type UserNotification = Loaded<UserNotificationEvent>;

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly zone = inject(NgZone);
  private readonly api = inject(NotificationApi);
  private readonly sseClient = inject(ServerSideEventsClient);
  private readonly eventSubject = new Subject<UserNotification>();

  connect(channel?: string): void {
    this.disconnect();
    const url = channel ? `/api/notifications/register?channel=${channel}` : '/api/notifications/register';
    this.sseClient.connect(url).subscribe(data => {
      if (data) {
        this.zone.run(() => this.eventSubject.next(data.data as UserNotification));
      }
    });
  }

  listen(): Observable<UserNotification> {
    return this.eventSubject.asObservable();
  }

  markAsRead(id: number): Observable<UserNotification> {
    return this.api.updateNotificationRead(id, { read: true } as UpdateNotificationStatusReadRequest).pipe(map(asLoaded));
  }

  markAsUnread(id: number): Observable<UserNotification> {
    return this.api.updateNotificationRead(id, { read: false } as UpdateNotificationStatusReadRequest).pipe(map(asLoaded));
  }

  disconnect(): void {
    this.sseClient.close();
  }

  reconnect(channel?: string): void {
    this.disconnect();
    this.connect(channel);
  }
}
