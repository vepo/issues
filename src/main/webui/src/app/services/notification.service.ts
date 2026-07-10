import { Injectable, NgZone, inject } from '@angular/core';
import { map, Observable, Subject, Subscription } from 'rxjs';
import { NotificationApi } from '../generated/api/notification.service';
import { NotificationPageResponse } from '../generated/model/notificationPageResponse';
import { UserNotificationEvent } from '../generated/model/userNotificationEvent';
import { UpdateNotificationStatusReadRequest } from '../generated/model/updateNotificationStatusReadRequest';
import { asLoaded, Loaded } from '../core/required-types';
import { ServerSideEventsClient } from './sse.client';

export type UserNotification = Loaded<UserNotificationEvent>;
export type NotificationPage = Loaded<NotificationPageResponse>;

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly zone = inject(NgZone);
  private readonly api = inject(NotificationApi);
  private readonly sseClient = inject(ServerSideEventsClient);
  private readonly eventSubject = new Subject<UserNotification>();
  private readonly reconnectedSubject = new Subject<void>();
  private sseSubscription?: Subscription;
  private reconnectSubscription?: Subscription;

  connect(channel?: string): void {
    this.disconnect();
    const url = channel ? `/api/notifications/register?channel=${channel}` : '/api/notifications/register';
    this.sseSubscription = this.sseClient.connect(url).subscribe(data => {
      if (data) {
        this.zone.run(() => this.eventSubject.next(data.data as UserNotification));
      }
    });
    this.reconnectSubscription = this.sseClient.reconnected().subscribe(() => {
      this.zone.run(() => this.reconnectedSubject.next());
    });
  }

  listen(): Observable<UserNotification> {
    return this.eventSubject.asObservable();
  }

  reconnected(): Observable<void> {
    return this.reconnectedSubject.asObservable();
  }

  list(page = 0, size = 20): Observable<NotificationPage> {
    return this.api.listNotifications(page, size).pipe(map(asLoaded));
  }

  markAsRead(id: number): Observable<UserNotification> {
    return this.api.updateNotificationRead(id, { read: true } as UpdateNotificationStatusReadRequest).pipe(map(asLoaded));
  }

  markAsUnread(id: number): Observable<UserNotification> {
    return this.api.updateNotificationRead(id, { read: false } as UpdateNotificationStatusReadRequest).pipe(map(asLoaded));
  }

  disconnect(): void {
    this.sseSubscription?.unsubscribe();
    this.reconnectSubscription?.unsubscribe();
    this.sseSubscription = undefined;
    this.reconnectSubscription = undefined;
    this.sseClient.close();
  }

  reconnect(channel?: string): void {
    this.disconnect();
    this.connect(channel);
  }
}
