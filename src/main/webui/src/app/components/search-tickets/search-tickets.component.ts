import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Project, ProjectsService } from '../../services/projects.service';
import { Status, StatusService } from '../../services/status.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { NormalizePipe } from '../pipes/normalize.pipe';

@Component({
  selector: 'app-search-tickets',
  templateUrl: './search-tickets.component.html',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, NormalizePipe]
})
export class SearchTicketsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly statusService = inject(StatusService);
  private readonly projectService = inject(ProjectsService);

  tickets: Ticket[] = [];
  statuses: Status[] = [];
  projects: Project[] = [];
  term = '';
  statusId = -1;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.term = params['q'] || '';
      this.statusId = Number(params['status'] || '-1');
      this.searchTickets();
    });
    this.statusService.findAll().subscribe(statuses => (this.statuses = statuses));
    this.projectService.findAll().subscribe(projects => (this.projects = projects));
  }

  statusName(statusId: number): string {
    const status = this.statuses.find(s => s.id == statusId);
    if (status) {
      return status.name
        .split('_')
        .map(i => i.substring(0, 1).toUpperCase() + i.substring(1).toLowerCase())
        .join(' ');
    }
    return '';
  }

  projectName(projectId: number): string {
    const project = this.projects.find(p => p.id == projectId);
    return project ? project.name : '';
  }

  isStatusActive(id: number): boolean {
    return this.statusId === id;
  }

  selectStatus(id: number): void {
    const params: Record<string, string | number> = {};
    if (this.term.trim().length > 0) {
      params['q'] = this.term.trim();
    }
    if (id !== -1) {
      params['status'] = id;
    }
    this.router.navigate(['/search'], { queryParams: params });
  }

  searchTickets() {
    this.ticketService.search(this.term, this.statusId).subscribe(tickets => (this.tickets = tickets));
  }
}
