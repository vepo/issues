import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Comment, CreateCommentRequest, TicketExpanded, TicketService } from '../../services/ticket.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';

@Component({
  selector: 'app-ticket-view',
  templateUrl: './ticket-view.component.html',
  imports: [DatePipe, NormalizePipe, FormsModule, RichTextEditorComponent, MatButtonModule, MatIconModule]
})
export class TicketViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);

  ticket?: TicketExpanded;
  comments: Comment[] = [];
  newComment: string = '';
  activeTab: 'history' | 'comments' = 'history';
  loadingComments = false;
  submittingComment = false;

  ngOnInit(): void {
    this.route.data.subscribe(({ ticket }) => {
      this.ticket = ticket;
      if (this.ticket) {
        this.loadComments();
      }
    });
  }

  loadComments(): void {
    if (!this.ticket) return;

    this.loadingComments = true;
    this.ticketService.getComments(this.ticket.id).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.loadingComments = false;
      },
      error: (error) => {
        console.error('Error loading comments:', error);
        this.loadingComments = false;
      }
    });
  }

  addComment(): void {
    if (!this.ticket || !this.newComment.trim()) return;

    this.submittingComment = true;
    const request: CreateCommentRequest = { content: this.newComment.trim() };

    this.ticketService.addComment(this.ticket.id, request).subscribe({
      next: (comment) => {
        this.comments.unshift(comment); // Add to beginning
        this.newComment = '';
        this.submittingComment = false;

        // Reload ticket to get updated history
        this.reloadTicket();
      },
      error: (error) => {
        console.error('Error adding comment:', error);
        this.submittingComment = false;
      }
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
        .subscribe(ticket => this.ticket = ticket);
    } else {
      this.ticketService.addSubscription(this.ticket?.id, this.authService.getAuthUserId())
        .subscribe(ticket => this.ticket = ticket);
    }
  }

  reloadTicket(): void {
    if (!this.ticket) return;

    this.ticketService.findExpandedById(this.ticket.id).subscribe({
      next: (updatedTicket) => {
        this.ticket = updatedTicket;
      },
      error: (error) => {
        console.error('Error reloading ticket:', error);
      }
    });
  }

  setActiveTab(tab: 'history' | 'comments'): void {
    this.activeTab = tab;
  }
} 