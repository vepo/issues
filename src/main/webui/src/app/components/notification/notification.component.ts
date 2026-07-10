import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { NotificationService, UserNotification } from '../../services/notification.service';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification',
  imports: [MatButtonModule, MatIconModule, MatMenuModule, DatePipe],
  styleUrl: './notification.component.scss',
  templateUrl: './notification.component.html',
  standalone: true,
})
export class NotificationComponent implements OnInit, OnDestroy {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly subscriptions = new Subscription();

  events: UserNotification[] = [];
  page = 0;
  hasMore = true;
  loading = false;
  private readonly pageSize = 20;

  ngOnInit(): void {
    this.loadPage(0, true);
    this.notificationService.connect();
    this.subscriptions.add(
      this.notificationService.listen().subscribe(event => this.mergeLiveEvent(event)),
    );
    this.subscriptions.add(
      this.notificationService.reconnected().subscribe(() => this.loadPage(0, true)),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.notificationService.disconnect();
  }

  eventsUnread(): number {
    return this.events.filter(e => !e.read).length;
  }

  onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (!this.hasMore || this.loading) {
      return;
    }
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 48) {
      this.loadPage(this.page + 1, false);
    }
  }

  navigate(notification: UserNotification): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id).subscribe(updated => {
        this.mergeLiveEvent(updated);
        void this.router.navigate(['/', 'ticket', notification.ticketId]);
      });
    } else {
      void this.router.navigate(['/', 'ticket', notification.ticketId]);
    }
  }

  private loadPage(page: number, reset: boolean): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.notificationService.list(page, this.pageSize).subscribe({
      next: response => {
        const items = (response.items ?? []) as UserNotification[];
        this.events = reset ? items : this.mergeById(this.events, items);
        this.page = response.page ?? page;
        this.hasMore = response.hasMore === true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private mergeLiveEvent(event: UserNotification): void {
    this.events = this.mergeById([event], this.events);
    this.events.sort((a, b) => b.timestamp - a.timestamp);
  }

  private mergeById(primary: UserNotification[], secondary: UserNotification[]): UserNotification[] {
    const byId = new Map<number, UserNotification>();
    for (const item of [...primary, ...secondary]) {
      if (item?.id != null) {
        byId.set(item.id, item);
      }
    }
    return Array.from(byId.values()).sort((a, b) => b.timestamp - a.timestamp);
  }
}
