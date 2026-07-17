import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';
import { BacklogService, BacklogTicket } from '../../services/backlog.service';
import { Project } from '../../services/projects.service';
import { NormalizePipe } from '../pipes/normalize.pipe';

@Component({
  selector: 'app-backlog',
  imports: [TranslocoPipe, RouterLink, DragDropModule, MatButtonModule, MatIconModule, NormalizePipe],
  templateUrl: './backlog.component.html',
  styleUrl: './backlog.component.scss'
})
export class BacklogComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly backlogService = inject(BacklogService);
  private readonly authService = inject(AuthService);

  project?: Project;
  tickets: BacklogTicket[] = [];
  page = 0;
  hasMore = true;
  loading = false;
  reordering = false;
  private readonly pageSize = 20;

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
      if (project?.id) {
        this.loadPage(0, true);
      }
    });
  }

  canReorder(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
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

  drop(event: CdkDragDrop<BacklogTicket[]>): void {
    if (!this.canReorder() || !this.project?.id || event.previousIndex === event.currentIndex) {
      return;
    }
    const previous = [...this.tickets];
    moveItemInArray(this.tickets, event.previousIndex, event.currentIndex);
    const moved = this.tickets[event.currentIndex];
    const before = this.tickets[event.currentIndex + 1];
    this.reordering = true;
    this.backlogService.reorder(this.project.id, {
      ticketId: moved.id,
      beforeTicketId: before?.id
    }).subscribe({
      next: () => {
        this.reordering = false;
        this.loadPage(0, true);
      },
      error: () => {
        this.tickets = previous;
        this.reordering = false;
      }
    });
  }

  loadPage(page: number, reset: boolean): void {
    if (!this.project?.id || this.loading) {
      return;
    }
    this.loading = true;
    this.backlogService.list(this.project.id, page, this.pageSize).subscribe({
      next: response => {
        const items = (response.items ?? []) as BacklogTicket[];
        this.tickets = reset ? items : [...this.tickets, ...items];
        this.page = response.page ?? page;
        this.hasMore = response.hasMore === true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
