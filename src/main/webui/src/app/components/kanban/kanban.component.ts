import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project, ProjectsService, ProjectWorkflow } from '../../services/projects.service';
import { ProjectStatus } from '../../services/status.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { PhaseService } from '../../services/phase.service';
import { NormalizePipe } from '../pipes/normalize.pipe';

type PhaseFilter = 'all' | 'active' | 'unplanned';

@Component({
  selector: 'app-kanban',
  templateUrl: './kanban.component.html',
  imports: [CommonModule, DragDropModule, RouterLink, NormalizePipe, MatButtonModule, MatIconModule, MatFormFieldModule, MatSelectModule],
  standalone: true
})
export class KanbanComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly ticketService = inject(TicketService);
  private readonly phaseService = inject(PhaseService);

  statuses: ProjectStatus[] = [];
  project: Project = { id: -1, name: '', prefix: '', description: '', workflow: { id: -1, name: '' }, ticketTemplate: { enabled: false }, phaseTemplate: { deliverables: [] } };
  tickets: Ticket[] = [];
  workflow?: ProjectWorkflow;
  phaseFilter: PhaseFilter = 'all';
  activePhaseId: number | null = null;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ statuses, project, tickets }) => {
      this.project = project;
      this.tickets = (tickets as Ticket[]).map(t => this.fixLineBreak(t));
      this.statuses = statuses;
      this.projectsService.findWorkflowByProjectId(Number(project.id))
                          .subscribe(workflow => this.workflow = workflow);
      this.phaseService.list(Number(project.id)).subscribe(phases => {
        this.activePhaseId = phases.find(p => p.status === 'ACTIVE')?.id ?? null;
      });
    });
  }

  visibleTickets(): Ticket[] {
    switch (this.phaseFilter) {
      case 'active':
        return this.activePhaseId == null
          ? []
          : this.tickets.filter(ticket => ticket.phaseId === this.activePhaseId);
      case 'unplanned':
        return this.tickets.filter(ticket => ticket.phaseId == null);
      default:
        return this.tickets;
    }
  }

  fixLineBreak(ticket: Ticket): Ticket {
    return {
      id: ticket.id,
      identifier: ticket.identifier,
      title: ticket.title,
      description: ticket.description.replaceAll('\n', '<br/>'),
      author: ticket.author,
      project: ticket.project,
      status: ticket.status,
      assignee: ticket.assignee,
      category: ticket.category,
      categoryName: ticket.categoryName,
      categoryColor: ticket.categoryColor,
      priority: ticket.priority,
      finishedAt: ticket.finishedAt,
      observedVersionId: ticket.observedVersionId,
      observedVersionLabel: ticket.observedVersionLabel,
      targetVersionId: ticket.targetVersionId,
      targetVersionLabel: ticket.targetVersionLabel,
      phaseId: ticket.phaseId,
      phaseName: ticket.phaseName
    };
  }

  ticketsOf(statusId: number): Ticket[] {
    return this.visibleTickets().filter(ticket => ticket.status == statusId);
  }

  connectedTo(status: ProjectStatus): string[] {
    return status.moveable.map(id => 'column-' + id);
  }

  toColumnId(status: ProjectStatus): string {
    return `column-${status.id}`;
  }

  fromColumnId(columnId: string): number {
    return Number(columnId.replace('column-', ''));
  }

  drop(evnt: CdkDragDrop<any>) {
    const ticket = (evnt.previousContainer.data[evnt.previousIndex] as Ticket);
    const statusId = this.fromColumnId(evnt.container.id);
    if (ticket.status != statusId) {
      this.ticketService.move(ticket.id, statusId)
                        .subscribe(ticket => this.tickets[this.tickets.findIndex(t => t.id == ticket.id)] =  this.fixLineBreak(ticket));
    }
  }
} 