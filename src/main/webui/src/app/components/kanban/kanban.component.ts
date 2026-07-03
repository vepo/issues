import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project, ProjectsService, ProjectWorkflow } from '../../services/projects.service';
import { ProjectStatus } from '../../services/status.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { NormalizePipe } from '../pipes/normalize.pipe';

@Component({
  selector: 'app-kanban',
  templateUrl: './kanban.component.html',
  imports: [CommonModule, DragDropModule, RouterLink, NormalizePipe, MatButtonModule, MatIconModule],
  standalone: true
})
export class KanbanComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly ticketService = inject(TicketService);

  statuses: ProjectStatus[] = [];
  project: Project = { id: -1, name: '', prefix: '', description: '', workflow: '' } as Project;
  tickets: Ticket[] = [];
  workflow?: ProjectWorkflow;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ statuses, project, tickets }) => {
      this.project = project;
      this.tickets = (tickets as Ticket[]).map(t => this.fixLineBreak(t));
      this.statuses = statuses;
      this.projectsService.findWorkflowByProjectId(Number(project.id))
                          .subscribe(workflow => this.workflow = workflow);
    });
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
      category:ticket.category
    };
  }

  ticketsOf(statusId: number): Ticket[] {
    return this.tickets.filter(ticket => ticket.status == statusId);
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