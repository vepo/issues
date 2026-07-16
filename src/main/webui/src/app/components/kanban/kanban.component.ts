import { CdkDrag, CdkDragDrop, CdkDropList, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project, ProjectsService, ProjectWorkflow } from '../../services/projects.service';
import { ProjectMember, ProjectMembersService } from '../../services/project-members.service';
import { ProjectStatus } from '../../services/status.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { ContextHintComponent } from '../context-hint/context-hint.component';
import { phaseStatusLabel, priorityLabel } from '../../core/system-labels';

/** `all` | `unplanned` | `active` | `phase:{id}` */
type PhaseFilterValue = string;

export type SwimlaneMode = 'none' | 'assignee' | 'priority';

export interface Swimlane {
  key: string;
  label: string;
}

const PRIORITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

@Component({
  selector: 'app-kanban',
  templateUrl: './kanban.component.html',
  imports: [CommonModule, DragDropModule, RouterLink, NormalizePipe, MatButtonModule, MatIconModule, MatFormFieldModule, MatSelectModule, ContextHintComponent],
  standalone: true
})
export class KanbanComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly ticketService = inject(TicketService);
  private readonly phaseService = inject(PhaseService);
  private readonly membersService = inject(ProjectMembersService);

  statuses: ProjectStatus[] = [];
  project: Project = { id: -1, name: '', prefix: '', description: '', workflow: { id: -1, name: '' }, owner: { id: -1, name: '', email: '' }, ticketTemplate: { enabled: false }, phaseTemplate: { deliverables: [] }, securityLevel: 'INTERNAL', prefixLocked: false };
  tickets: Ticket[] = [];
  workflow?: ProjectWorkflow;
  phases: Phase[] = [];
  phaseFilter: PhaseFilterValue = 'all';
  activePhaseId: number | null = null;
  swimlaneMode: SwimlaneMode = 'none';
  members: ProjectMember[] = [];

  readonly canEnterColumn = (drag: CdkDrag<Ticket>, drop: CdkDropList<Ticket[]>): boolean => {
    const targetStatusId = this.statusIdFromListId(drop.id);
    const ticket = drag.data;
    if (ticket?.status === targetStatusId) {
      return true;
    }
    return !this.isWipFull(targetStatusId);
  };

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ statuses, project, tickets }) => {
      this.project = project;
      this.tickets = (tickets as Ticket[]).map(t => this.fixLineBreak(t));
      this.statuses = statuses;
      this.projectsService.findWorkflowByProjectId(Number(project.id))
                          .subscribe(workflow => this.workflow = workflow);
      this.phaseService.list(Number(project.id)).subscribe(phases => {
        this.phases = phases;
        this.activePhaseId = phases.find(p => p.status === 'ACTIVE')?.id ?? null;
      });
      this.membersService.listMembers(Number(project.id)).subscribe(members => {
        this.members = members;
      });
    });
  }

  phaseFilterValue(phaseId: number): PhaseFilterValue {
    return `phase:${phaseId}`;
  }

  statusLabel(status: Phase['status']): string {
    return phaseStatusLabel(status);
  }

  visibleTickets(): Ticket[] {
    if (this.phaseFilter === 'all') {
      return this.tickets;
    }
    if (this.phaseFilter === 'unplanned') {
      return this.tickets.filter(ticket => ticket.phaseId == null);
    }
    if (this.phaseFilter === 'active') {
      return this.activePhaseId == null
        ? []
        : this.tickets.filter(ticket => ticket.phaseId === this.activePhaseId);
    }
    if (this.phaseFilter.startsWith('phase:')) {
      const phaseId = Number(this.phaseFilter.slice('phase:'.length));
      return this.tickets.filter(ticket => ticket.phaseId === phaseId);
    }
    return this.tickets;
  }

  swimlanes(): Swimlane[] {
    if (this.swimlaneMode === 'none') {
      return [{ key: 'all', label: '' }];
    }
    if (this.swimlaneMode === 'assignee') {
      const ids = new Set<number>();
      let hasUnassigned = false;
      for (const ticket of this.visibleTickets()) {
        if (ticket.assignee == null) {
          hasUnassigned = true;
        } else {
          ids.add(Number(ticket.assignee));
        }
      }
      const lanes = [...ids]
        .sort((a, b) => this.assigneeLabel(a).localeCompare(this.assigneeLabel(b)))
        .map(id => ({ key: `assignee:${id}`, label: this.assigneeLabel(id) }));
      if (hasUnassigned || lanes.length === 0) {
        lanes.push({ key: 'assignee:none', label: 'Sem responsável' });
      }
      return lanes;
    }
    const priorities = new Set<string>();
    for (const ticket of this.visibleTickets()) {
      priorities.add((ticket.priority ?? 'MEDIUM').toUpperCase());
    }
    const ordered = PRIORITY_ORDER.filter(p => priorities.has(p));
    const extras = [...priorities].filter(p => !PRIORITY_ORDER.includes(p)).sort();
    return [...ordered, ...extras].map(priority => ({
      key: `priority:${priority}`,
      label: priorityLabel(priority)
    }));
  }

  ticketsOf(statusId: number, lane?: Swimlane): Ticket[] {
    return this.visibleTickets().filter(ticket => {
      if (ticket.status != statusId) {
        return false;
      }
      return this.ticketMatchesLane(ticket, lane);
    });
  }

  columnCount(statusId: number): number {
    return this.visibleTickets().filter(ticket => ticket.status == statusId).length;
  }

  wipLabel(status: ProjectStatus): string {
    const count = this.columnCount(status.id);
    if (status.wipLimit == null) {
      return String(count);
    }
    return `${count}/${status.wipLimit}`;
  }

  isWipFull(statusId: number): boolean {
    const status = this.statuses.find(s => s.id === statusId);
    if (status?.wipLimit == null) {
      return false;
    }
    return this.columnCount(statusId) >= status.wipLimit;
  }

  isWipOver(status: ProjectStatus): boolean {
    return status.wipLimit != null && this.columnCount(status.id) >= status.wipLimit;
  }

  connectedTo(status: ProjectStatus): string[] {
    const targetIds = status.moveable ?? [];
    if (this.swimlaneMode === 'none') {
      return targetIds.map(id => this.toColumnId({ id } as ProjectStatus));
    }
    return this.swimlanes().flatMap(lane =>
      targetIds.map(id => this.toCellId(id, lane.key))
    );
  }

  toColumnId(status: ProjectStatus): string {
    return `column-${status.id}`;
  }

  toCellId(statusId: number, laneKey: string): string {
    return `column-${statusId}__${laneKey}`;
  }

  listId(status: ProjectStatus, lane: Swimlane): string {
    return this.swimlaneMode === 'none'
      ? this.toColumnId(status)
      : this.toCellId(status.id, lane.key);
  }

  statusIdFromListId(listId: string): number {
    const base = listId.includes('__') ? listId.slice(0, listId.indexOf('__')) : listId;
    return Number(base.replace('column-', ''));
  }

  fromColumnId(columnId: string): number {
    return this.statusIdFromListId(columnId);
  }

  drop(evnt: CdkDragDrop<Ticket[]>) {
    const ticket = evnt.previousContainer.data[evnt.previousIndex] as Ticket;
    const statusId = this.statusIdFromListId(evnt.container.id);
    if (ticket.status != statusId) {
      if (this.isWipFull(statusId)) {
        return;
      }
      this.ticketService.move(ticket.id, statusId)
                        .subscribe(moved => this.tickets[this.tickets.findIndex(t => t.id == moved.id)] = this.fixLineBreak(moved));
    }
  }

  private ticketMatchesLane(ticket: Ticket, lane?: Swimlane): boolean {
    if (!lane || this.swimlaneMode === 'none' || lane.key === 'all') {
      return true;
    }
    if (this.swimlaneMode === 'assignee') {
      if (lane.key === 'assignee:none') {
        return ticket.assignee == null;
      }
      return Number(ticket.assignee) === Number(lane.key.slice('assignee:'.length));
    }
    const priority = (ticket.priority ?? 'MEDIUM').toUpperCase();
    return lane.key === `priority:${priority}`;
  }

  private assigneeLabel(userId: number): string {
    const member = this.members.find(m => m.id === userId);
    return member?.name ?? `Usuário ${userId}`;
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
      ticketType: ticket.ticketType,
      finishedAt: ticket.finishedAt,
      canceledAt: ticket.canceledAt,
      dueDate: ticket.dueDate,
      storyPoints: ticket.storyPoints,
      observedVersionId: ticket.observedVersionId,
      observedVersionLabel: ticket.observedVersionLabel,
      targetVersionId: ticket.targetVersionId,
      targetVersionLabel: ticket.targetVersionLabel,
      phaseId: ticket.phaseId,
      phaseName: ticket.phaseName,
      deleted: ticket.deleted,
      customFields: ticket.customFields ?? [],
    };
  }
}
