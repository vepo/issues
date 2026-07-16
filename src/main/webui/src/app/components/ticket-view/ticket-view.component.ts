import { DatePipe } from '@angular/common';
import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ProjectMembersService } from '../../services/project-members.service';
import { ProjectsService } from '../../services/projects.service';
import { Category, CategoryService } from '../../services/category.service';
import { ProjectStatus, StatusService } from '../../services/status.service';
import { User } from '../../services/users.service';
import {
  Attachment,
  Comment,
  CreateCommentRequest,
  Ticket,
  TicketExpanded,
  TicketLink,
  TicketLinkType,
  TicketService,
  TicketType,
  UpdateTicketRequest,
} from '../../services/ticket.service';
import { Version, VersionService } from '../../services/version.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';
import { CustomFieldFormSectionComponent } from '../custom-fields/custom-field-form-section.component';
import { plainTextLengthValidator } from '../../core/plain-text-length';
import { PEER_LINK_TYPE_OPTIONS, TICKET_TYPE_OPTIONS, ticketTypeLabel as systemTicketTypeLabel } from '../../core/system-labels';
import {
  ActivityItem,
  buildActivityFeed,
  commitAuthorLabel,
  formatActorLabel,
  shortCommitSha,
  trackActivityItem,
} from '../ticket-activity-feed/activity-feed.utils';

export interface LinkGroup {
  label: string;
  links: TicketLink[];
}

@Component({
  selector: 'app-ticket-view',
  templateUrl: './ticket-view.component.html',
  imports: [
    DatePipe,
    NormalizePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    RichTextEditorComponent,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    CustomFieldFormSectionComponent,
  ]
})
export class TicketViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);
  private readonly membersService = inject(ProjectMembersService);
  private readonly projectsService = inject(ProjectsService);
  private readonly categoryService = inject(CategoryService);
  private readonly statusService = inject(StatusService);
  private readonly versionService = inject(VersionService);
  private readonly phaseService = inject(PhaseService);
  private readonly dialog = inject(MatDialog);
  private readonly formBuilder = inject(FormBuilder);

  @ViewChild(CustomFieldFormSectionComponent) customFieldsSection?: CustomFieldFormSectionComponent;

  ticket?: TicketExpanded;
  comments: Comment[] = [];
  attachments: Attachment[] = [];
  selectedAttachmentFile: File | null = null;
  uploadingAttachment = false;
  attachmentError = '';
  newComment: string = '';
  activeTab: 'history' | 'comments' = 'history';
  loadingComments = false;
  submittingComment = false;
  isEditing = false;
  isSaving = false;
  categories: Category[] = [];
  users: User[] = [];
  projectStatuses: ProjectStatus[] = [];
  projectVersions: Version[] = [];
  assignablePhases: Phase[] = [];
  selectedStatusId: number | null = null;
  selectedAssigneeId: number | null = null;
  doneStatusNames = new Set<string>();

  readonly ticketTypes = TICKET_TYPE_OPTIONS;
  readonly peerLinkTypes = PEER_LINK_TYPE_OPTIONS;
  newLinkType: TicketLinkType = 'RELATES_TO';
  linkSearchTerm = '';
  linkSearchResults: Ticket[] = [];
  selectedLinkTarget: Ticket | null = null;
  linking = false;
  private readonly linkSearch$ = new Subject<string>();

  editForm: FormGroup = this.formBuilder.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, plainTextLengthValidator(5, 1200)]],
    categoryId: [null as number | null, Validators.required],
    priority: ['MEDIUM', Validators.required],
    ticketType: ['TASK' as TicketType, Validators.required],
    dueDate: [null as string | null],
    storyPoints: [null as number | null, Validators.min(0)],
    observedVersionId: [null as number | null],
    targetVersionId: [null as number | null],
    phaseId: [null as number | null]
  });

  readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  ngOnInit(): void {
    this.categoryService.findAll().subscribe(categories => {
      this.categories = categories;
      this.populateEditForm();
    });

    this.route.data.subscribe(({ ticket }) => {
      this.ticket = ticket;
      if (this.ticket) {
        this.loadProjectMembers();
        this.loadComments();
        this.loadAttachments();
        this.loadProjectStatuses();
        this.loadProjectVersions();
        this.loadAssignablePhases();
        this.loadDoneStatusNames();
        this.populateEditForm();
      }
    });

    this.linkSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        const trimmed = term.trim();
        if (trimmed.length < 2) {
          return of([] as Ticket[]);
        }
        return this.ticketService.search(trimmed, -1);
      })
    ).subscribe(results => {
      this.linkSearchResults = results.filter(t => t.id !== this.ticket?.id && !t.deleted);
    });
  }

  loadProjectMembers(): void {
    if (!this.ticket?.project?.id) {
      return;
    }
    this.membersService.listMembers(this.ticket.project.id).subscribe(members => {
      this.users = members.map(member => ({
        id: member.id,
        name: member.name,
        email: member.email,
        username: member.email,
        roles: []
      }));
    });
  }

  loadProjectStatuses(): void {
    if (!this.ticket?.project?.id) {
      return;
    }
    this.statusService.findProjectsStatuses(this.ticket.project.id)
                     .subscribe(statuses => {
                       this.projectStatuses = statuses;
                       this.selectedStatusId = this.currentStatusId();
                       this.selectedAssigneeId = this.ticket?.assignee?.id ?? null;
                     });
  }

  loadDoneStatusNames(): void {
    if (!this.ticket?.project?.id) {
      return;
    }
    this.projectsService.findWorkflowByProjectId(this.ticket.project.id).subscribe(workflow => {
      this.doneStatusNames = new Set(
        (workflow.finishStatuses ?? [])
          .filter(fs => fs.outcome === 'DONE')
          .map(fs => fs.status)
          .filter((name): name is string => !!name)
      );
    });
  }

  loadAssignablePhases(): void {
    if (!this.ticket?.project?.id) {
      return;
    }
    this.phaseService.list(this.ticket.project.id)
                     .subscribe(phases => {
                       this.assignablePhases = phases.filter(p => p.status === 'PLANNED' || p.status === 'ACTIVE');
                     });
  }

  loadProjectVersions(): void {
    if (!this.ticket?.project?.id) {
      return;
    }
    this.versionService.list(this.ticket.project.id)
                       .subscribe(versions => this.projectVersions = versions);
  }

  populateEditForm(): void {
    if (!this.ticket) {
      return;
    }
    const category = this.categories.find(c => c.name === this.ticket!.category);
    this.editForm.patchValue({
      title: this.ticket.title,
      description: this.ticket.description,
      categoryId: category?.id ?? null,
      priority: this.ticket.priority ?? 'MEDIUM',
      ticketType: (this.ticket.ticketType as TicketType) ?? 'TASK',
      dueDate: this.ticket.dueDate ?? null,
      storyPoints: this.ticket.storyPoints ?? null,
      observedVersionId: this.ticket.observedVersionId ?? null,
      targetVersionId: this.ticket.targetVersionId ?? null,
      phaseId: this.ticket.phaseId ?? null
    });
  }

  ticketTypeLabel(type?: string | null): string {
    return systemTicketTypeLabel(type);
  }

  isEpic(): boolean {
    return this.ticket?.ticketType === 'EPIC';
  }

  groupedLinks(): LinkGroup[] {
    const links = (this.ticket?.links ?? []).filter(link => !link.otherDeleted) as TicketLink[];
    const groups = new Map<string, TicketLink[]>();
    for (const link of links) {
      const label = link.displayLabel || link.linkType || '';
      const existing = groups.get(label) ?? [];
      existing.push(link);
      groups.set(label, existing);
    }
    return Array.from(groups.entries()).map(([label, groupLinks]) => ({ label, links: groupLinks }));
  }

  childLinks(): TicketLink[] {
    return ((this.ticket?.links ?? []) as TicketLink[]).filter(
      link => link.linkType === 'CHILD_OF' && link.direction === 'INBOUND' && !link.otherDeleted
    );
  }

  childrenProgressLabel(): string {
    const summary = this.ticket?.childrenSummary;
    const done = summary?.done ?? 0;
    const total = summary?.total ?? 0;
    return `${done}/${total} concluídas`;
  }

  onLinkSearchInput(term: string): void {
    this.linkSearchTerm = term;
    this.selectedLinkTarget = null;
    this.linkSearch$.next(term);
  }

  selectLinkTarget(ticket: Ticket): void {
    this.selectedLinkTarget = ticket;
    this.linkSearchTerm = `${ticket.identifier} — ${ticket.title}`;
    this.linkSearchResults = [];
  }

  createLink(): void {
    if (!this.ticket || !this.selectedLinkTarget || this.linking) {
      return;
    }
    this.linking = true;
    this.ticketService.createLink(this.ticket.id, {
      targetTicketId: this.selectedLinkTarget.id,
      linkType: this.newLinkType,
    }).subscribe({
      next: () => {
        this.linking = false;
        this.selectedLinkTarget = null;
        this.linkSearchTerm = '';
        this.linkSearchResults = [];
        this.reloadTicket();
      },
      error: () => {
        this.linking = false;
      }
    });
  }

  removeLink(link: TicketLink): void {
    if (!this.ticket) {
      return;
    }
    this.ticketService.deleteLink(this.ticket.id, link.id).subscribe({
      next: () => this.reloadTicket()
    });
  }

  openCreateChildDialog(): void {
    if (!this.ticket) {
      return;
    }
    const dialogRef = this.dialog.open(CreateChildTicketDialogComponent, {
      width: '480px',
      data: { epicIdentifier: this.ticket.identifier },
    });
    dialogRef.afterClosed().subscribe((result?: { title: string; description?: string }) => {
      if (!result || !this.ticket) {
        return;
      }
      this.ticketService.createChild(this.ticket.id, {
        title: result.title,
        description: result.description,
      }).subscribe({
        next: () => this.reloadTicket()
      });
    });
  }

  currentStatusId(): number | null {
    if (!this.ticket) {
      return null;
    }
    return this.projectStatuses.find(s => s.name === this.ticket!.status)?.id ?? null;
  }

  allowedTargetStatuses(): ProjectStatus[] {
    const currentId = this.currentStatusId();
    const current = this.projectStatuses.find(s => s.id === currentId);
    if (!current?.moveable) {
      return [];
    }
    return this.projectStatuses.filter(s => current.moveable!.includes(s.id!));
  }

  loadComments(): void {
    if (!this.ticket) return;

    this.loadingComments = true;
    this.ticketService.getComments(this.ticket.id).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.loadingComments = false;
      },
      error: () => {
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
        this.comments.unshift(comment);
        this.newComment = '';
        this.submittingComment = false;
        this.reloadTicket();
      },
      error: () => {
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

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (this.isEditing) {
      this.populateEditForm();
    }
  }

  formatCustomFieldValue(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    if (typeof value === 'boolean') {
      return value ? 'Sim' : 'Não';
    }
    return String(value);
  }

  actorLabel(name: string | undefined | null, viaAgent?: boolean | null): string {
    return formatActorLabel(name, viaAgent);
  }

  historyActionLabel(action: string | undefined | null): string {
    switch (action) {
      case 'LINK_ADDED':
        return 'Vínculo adicionado';
      case 'LINK_REMOVED':
        return 'Vínculo removido';
      case 'CREATED':
        return 'Criado';
      case 'FIELD_CHANGED':
        return 'Campo alterado';
      case 'STATUS_CHANGED':
        return 'Status alterado';
      case 'ASSIGNEE_CHANGED':
        return 'Responsável alterado';
      case 'SUBSCRIBED':
        return 'Observador adicionado';
      case 'UNSUBSCRIBED':
        return 'Observador removido';
      case 'DELETED':
        return 'Excluído';
      case 'RESTORED':
        return 'Restaurado';
      case 'ATTACHMENT_ADDED':
        return $localize`:@@history.attachmentAdded:Anexo adicionado`;
      case 'ATTACHMENT_REMOVED':
        return $localize`:@@history.attachmentRemoved:Anexo removido`;
      default:
        return action || '';
    }
  }

  saveTicket(): void {
    if (!this.ticket || this.editForm.invalid) {
      return;
    }
    if (this.customFieldsSection && !this.customFieldsSection.isValid()) {
      return;
    }
    this.isSaving = true;
    const value = this.editForm.value;
    this.ticketService.update(this.ticket.id, {
      title: value.title,
      description: value.description,
      categoryId: value.categoryId,
      priority: value.priority as UpdateTicketRequest['priority'],
      ticketType: value.ticketType as TicketType,
      dueDate: value.dueDate || undefined,
      storyPoints: value.storyPoints ?? undefined,
      planningFields: {
        phaseId: value.phaseId ?? null,
        observedVersionId: value.observedVersionId ?? null,
        targetVersionId: value.targetVersionId ?? null
      },
      customFields: this.customFieldsSection?.toValueRequests() ?? [],
    }).subscribe({
      next: () => {
        this.isSaving = false;
        this.isEditing = false;
        this.reloadTicket();
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }

  saveAssignee(): void {
    if (!this.ticket || this.selectedAssigneeId == null) {
      return;
    }
    this.ticketService.updateAssignee(this.ticket.id, this.selectedAssigneeId)
                      .subscribe(() => this.reloadTicket());
  }

  isMovingToDoneStatus(statusId: number | null): boolean {
    if (statusId == null) {
      return false;
    }
    const status = this.projectStatuses.find(s => s.id === statusId);
    return !!status?.name && this.doneStatusNames.has(status.name);
  }

  hasOpenChildren(): boolean {
    const summary = this.ticket?.childrenSummary;
    if (!summary) {
      return false;
    }
    return (summary.total ?? 0) > (summary.done ?? 0);
  }

  moveTicket(): void {
    if (!this.ticket || this.selectedStatusId == null || this.selectedStatusId === this.currentStatusId()) {
      return;
    }
    const doMove = () => {
      this.ticketService.move(this.ticket!.id, this.selectedStatusId!)
                        .subscribe(() => this.reloadTicket());
    };
    if (this.isEpic() && this.hasOpenChildren() && this.isMovingToDoneStatus(this.selectedStatusId)) {
      const confirmed = this.dialog.open(EpicDoneWarningDialogComponent, {
        data: {
          done: this.ticket.childrenSummary?.done ?? 0,
          total: this.ticket.childrenSummary?.total ?? 0,
        },
      });
      confirmed.afterClosed().subscribe(result => {
        if (result) {
          doMove();
        }
      });
      return;
    }
    doMove();
  }

  canDelete(): boolean {
    return !this.isDeleted() && (this.authService.hasRole('admin') || this.authService.hasRole('project-manager'));
  }

  canRestore(): boolean {
    return this.isDeleted() && (this.authService.hasRole('admin') || this.authService.hasRole('project-manager'));
  }

  isDeleted(): boolean {
    return this.ticket?.deleted === true;
  }

  cloneTicket(): void {
    if (!this.ticket?.project?.id || this.isDeleted()) {
      return;
    }
    void this.router.navigate(['/tickets/new'], {
      queryParams: {
        cloneFrom: this.ticket.id,
        targetProjectId: this.ticket.project.id,
      },
    });
  }

  confirmRestore(): void {
    if (!this.ticket) {
      return;
    }
    this.ticketService.restore(this.ticket.id).subscribe({
      next: () => this.reloadTicket()
    });
  }

  confirmDelete(): void {
    if (!this.ticket) {
      return;
    }
    const confirmed = this.dialog.open(TicketDeleteDialogComponent);
    confirmed.afterClosed().subscribe(result => {
      if (result && this.ticket) {
        this.ticketService.delete(this.ticket.id).subscribe({
          next: () => this.reloadTicket()
        });
      }
    });
  }

  reloadTicket(): void {
    if (!this.ticket) return;

    this.ticketService.findExpandedByIdentifier(this.ticket.identifier).subscribe({
      next: (updatedTicket) => {
        this.ticket = updatedTicket;
        this.loadProjectStatuses();
        this.loadProjectVersions();
        this.loadAssignablePhases();
        this.loadDoneStatusNames();
        this.loadAttachments();
        this.populateEditForm();
      }
    });
  }

  loadAttachments(): void {
    if (!this.ticket) {
      return;
    }
    this.ticketService.listAttachments(this.ticket.id).subscribe({
      next: attachments => {
        this.attachments = attachments;
      },
      error: () => {
        this.attachments = [];
      },
    });
  }

  formatAttachmentSize(sizeBytes: number | undefined | null): string {
    const size = sizeBytes ?? 0;
    if (size < 1024) {
      return `${size} B`;
    }
    if (size < 1024 * 1024) {
      return `${(size / 1024).toFixed(1)} KB`;
    }
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  onAttachmentFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedAttachmentFile = input.files?.[0] ?? null;
    this.attachmentError = '';
  }

  uploadAttachment(): void {
    if (!this.ticket || !this.selectedAttachmentFile || this.uploadingAttachment) {
      return;
    }
    this.uploadingAttachment = true;
    this.attachmentError = '';
    this.ticketService.uploadAttachment(this.ticket.id, this.selectedAttachmentFile).subscribe({
      next: () => {
        this.uploadingAttachment = false;
        this.selectedAttachmentFile = null;
        this.loadAttachments();
        this.reloadTicket();
      },
      error: err => {
        this.uploadingAttachment = false;
        this.attachmentError = err?.error?.message || $localize`:@@attachment.uploadFailed:Falha ao anexar o arquivo.`;
      },
    });
  }

  downloadAttachment(attachment: Attachment): void {
    if (!this.ticket) {
      return;
    }
    this.ticketService.downloadAttachment(this.ticket.id, attachment.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = attachment.originalFilename || 'attachment';
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  confirmDeleteAttachment(attachment: Attachment): void {
    if (!this.ticket) {
      return;
    }
    const confirmed = this.dialog.open(AttachmentDeleteDialogComponent, {
      data: { filename: attachment.originalFilename },
    });
    confirmed.afterClosed().subscribe(result => {
      if (!result || !this.ticket) {
        return;
      }
      this.ticketService.deleteAttachment(this.ticket.id, attachment.id).subscribe({
        next: () => {
          this.loadAttachments();
          this.reloadTicket();
        },
      });
    });
  }

  setActiveTab(tab: 'history' | 'comments'): void {
    this.activeTab = tab;
  }

  historyTabItems(): ActivityItem[] {
    if (!this.ticket) {
      return [];
    }
    return buildActivityFeed(this.ticket.history ?? [], [], this.ticket.linkedCommits ?? [])
      .filter(item => item.kind !== 'comment');
  }

  protected readonly trackActivityItem = trackActivityItem;
  protected readonly commitAuthorLabel = commitAuthorLabel;
  protected readonly shortCommitSha = shortCommitSha;
}

@Component({
  selector: 'app-ticket-delete-dialog',
  imports: [MatDialogModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButtonModule],
  template: `
    <h2 mat-dialog-title i18n>Excluir ticket</h2>
    <mat-dialog-content i18n>Deseja excluir este ticket? Esta ação não pode ser desfeita.</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button class="btn btn-secondary" matButton="outlined" mat-dialog-close i18n>Cancelar</button>
      <button class="btn btn-cancel" matButton="filled" [mat-dialog-close]="true" i18n>Excluir</button>
    </mat-dialog-actions>
  `
})
export class TicketDeleteDialogComponent {}

@Component({
  selector: 'app-attachment-delete-dialog',
  imports: [MatDialogModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButtonModule],
  template: `
    <h2 mat-dialog-title i18n>Excluir anexo</h2>
    <mat-dialog-content>
      <p i18n>Deseja excluir o anexo {{ data.filename }}?</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button class="btn btn-secondary" matButton="outlined" mat-dialog-close i18n>Cancelar</button>
      <button class="btn btn-cancel" matButton="filled" [mat-dialog-close]="true" i18n>Excluir</button>
    </mat-dialog-actions>
  `
})
export class AttachmentDeleteDialogComponent {
  readonly data = inject<{ filename: string }>(MAT_DIALOG_DATA);
}

@Component({
  selector: 'app-epic-done-warning-dialog',
  imports: [MatDialogModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButtonModule],
  template: `
    <h2 mat-dialog-title i18n>Épico com subtarefas abertas</h2>
    <mat-dialog-content>
      <p i18n>
        Este épico ainda tem {{ data.done }}/{{ data.total }} subtarefas concluídas.
        Deseja movê-lo para um status de conclusão mesmo assim?
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button class="btn btn-secondary" matButton="outlined" mat-dialog-close i18n>Cancelar</button>
      <button class="btn" matButton="filled" [mat-dialog-close]="true" i18n>Mover mesmo assim</button>
    </mat-dialog-actions>
  `
})
export class EpicDoneWarningDialogComponent {
  readonly data = inject<{ done: number; total: number }>(MAT_DIALOG_DATA);
}

@Component({
  selector: 'app-create-child-ticket-dialog',
  imports: [
    MatDialogModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    RichTextEditorComponent,
  ],
  template: `
    <h2 mat-dialog-title i18n>Nova subtarefa</h2>
    <mat-dialog-content>
      <p class="text-muted" i18n>Será criada no mesmo projeto do épico {{ data.epicIdentifier }}.</p>
      <form [formGroup]="form" class="edit">
        <mat-form-field class="form-field" appearance="outline">
          <mat-label i18n>Título</mat-label>
          <input matInput formControlName="title" required />
        </mat-form-field>
        <div class="form-field form-field--rich-text">
          <label class="form-label" i18n>Descrição (opcional)</label>
          <app-rich-text-editor formControlName="description" placeholder="Descrição opcional..."></app-rich-text-editor>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button class="btn btn-secondary" matButton="outlined" mat-dialog-close i18n>Cancelar</button>
      <button class="btn" matButton="filled" [disabled]="form.invalid" [mat-dialog-close]="form.value" i18n>Criar</button>
    </mat-dialog-actions>
  `
})
export class CreateChildTicketDialogComponent {
  readonly data = inject<{ epicIdentifier: string }>(MAT_DIALOG_DATA);
  private readonly formBuilder = inject(FormBuilder);

  form = this.formBuilder.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: [''],
  });
}
