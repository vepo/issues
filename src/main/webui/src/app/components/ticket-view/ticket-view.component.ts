import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Comment, CreateCommentRequest, TicketExpanded, TicketService } from '../../services/ticket.service';
import { NotificationService } from '../../services/notification.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';
import { TicketActivityFeedComponent } from '../ticket-activity-feed/ticket-activity-feed.component';
import {
  ActivityFilter,
  ActivityItem,
  buildActivityFeed,
  filterActivity,
} from '../ticket-activity-feed/activity-feed.utils';

@Component({
  selector: 'app-ticket-view',
  templateUrl: './ticket-view.component.html',
  imports: [
    NormalizePipe,
    FormsModule,
    RichTextEditorComponent,
    TicketActivityFeedComponent,
    MatButtonModule,
    MatIconModule,
  ],
})
export class TicketViewComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroy$ = new Subject<void>();

  ticket?: TicketExpanded;
  comments: Comment[] = [];
  newComment: string = '';
  activeFilter: ActivityFilter = 'all';
  loadingComments = false;
  submittingComment = false;
  activityItems: ActivityItem[] = [];

  ngOnInit(): void {
    this.route.data.subscribe(({ ticket }) => {
      this.ticket = ticket;
      if (this.ticket) {
        this.loadComments();
        this.refreshActivity();
      }
    });

    this.notificationService.listen()
      .pipe(
        takeUntil(this.destroy$),
        filter(event => event.type === 'ticket-moved' && event.ticketId === this.ticket?.id),
      )
      .subscribe(() => this.reloadTicket());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadComments(): void {
    if (!this.ticket) return;

    this.loadingComments = true;
    this.ticketService.getComments(this.ticket.id).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.loadingComments = false;
        this.refreshActivity();
      },
      error: () => {
        this.loadingComments = false;
      },
    });
  }

  addComment(): void {
    if (!this.ticket || !this.newComment.trim()) return;

    this.submittingComment = true;
    const request: CreateCommentRequest = { content: this.newComment.trim() };

    this.ticketService.addComment(this.ticket.id, request).subscribe({
      next: (comment) => {
        this.comments.unshift(comment);
        this.newComment = '';
        this.submittingComment = false;
        this.refreshActivity();
        this.reloadTicket();
      },
      error: () => {
        this.submittingComment = false;
      },
    });
  }

  onCommentChange(content: string): void {
    this.newComment = content;
  }

  isSubscribed(): boolean {
    return this.ticket?.subscribers.findIndex(user => user.id == this.authService.getAuthUserId()) != -1;
  }

  toggle() {
    if (!this.ticket) return;

    if (this.isSubscribed()) {
      this.ticketService.removeSubscription(this.ticket?.id, this.authService.getAuthUserId())
        .subscribe(ticket => {
          this.ticket = ticket;
          this.refreshActivity();
        });
    } else {
      this.ticketService.addSubscription(this.ticket?.id, this.authService.getAuthUserId())
        .subscribe(ticket => {
          this.ticket = ticket;
          this.refreshActivity();
        });
    }
  }

  reloadTicket(): void {
    if (!this.ticket) return;

    this.ticketService.findExpandedByIdentifier(this.ticket.identifier).subscribe({
      next: (updatedTicket) => {
        this.ticket = updatedTicket;
        this.refreshActivity();
      },
    });
  }

  setActiveFilter(filter: ActivityFilter): void {
    this.activeFilter = filter;
  }

  filteredActivity(): ActivityItem[] {
    return filterActivity(this.activityItems, this.activeFilter);
  }

  private refreshActivity(): void {
    this.activityItems = buildActivityFeed(this.ticket?.history ?? [], this.comments);
  }
}
