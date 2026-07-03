import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProjectMembersService } from '../../services/project-members.service';
import { Category, CategoryService } from '../../services/category.service';
import { ProjectStatus, StatusService } from '../../services/status.service';
import { User } from '../../services/users.service';
import { Comment, CreateCommentRequest, TicketExpanded, TicketService, UpdateTicketRequest } from '../../services/ticket.service';
import { Version, VersionService } from '../../services/version.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';

@Component({
  selector: 'app-ticket-view',
  templateUrl: './ticket-view.component.html',
  imports: [
    DatePipe,
    NormalizePipe,
    FormsModule,
    ReactiveFormsModule,
    RichTextEditorComponent,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    RouterLink
  ]
})
export class TicketViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);
  private readonly membersService = inject(ProjectMembersService);
  private readonly categoryService = inject(CategoryService);
  private readonly statusService = inject(StatusService);
  private readonly versionService = inject(VersionService);
  private readonly phaseService = inject(PhaseService);
  private readonly dialog = inject(MatDialog);
  private readonly formBuilder = inject(FormBuilder);

  ticket?: TicketExpanded;
  comments: Comment[] = [];
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

  editForm: FormGroup = this.formBuilder.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.minLength(5)]],
    categoryId: [null as number | null, Validators.required],
    priority: ['MEDIUM', Validators.required],
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
        this.loadProjectStatuses();
        this.loadProjectVersions();
        this.loadAssignablePhases();
        this.populateEditForm();
      }
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
      observedVersionId: this.ticket.observedVersionId ?? null,
      targetVersionId: this.ticket.targetVersionId ?? null,
      phaseId: this.ticket.phaseId ?? null
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

  saveTicket(): void {
    if (!this.ticket || this.editForm.invalid) {
      return;
    }
    this.isSaving = true;
    const value = this.editForm.value;
    this.ticketService.update(this.ticket.id, {
      title: value.title,
      description: value.description,
      categoryId: value.categoryId,
      priority: value.priority as UpdateTicketRequest['priority'],
      planningFields: {
        phaseId: value.phaseId ?? null,
        observedVersionId: value.observedVersionId ?? null,
        targetVersionId: value.targetVersionId ?? null
      }
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

  moveTicket(): void {
    if (!this.ticket || this.selectedStatusId == null || this.selectedStatusId === this.currentStatusId()) {
      return;
    }
    this.ticketService.move(this.ticket.id, this.selectedStatusId)
                      .subscribe(() => this.reloadTicket());
  }

  canDelete(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }

  confirmDelete(): void {
    if (!this.ticket) {
      return;
    }
    const confirmed = this.dialog.open(TicketDeleteDialogComponent);
    confirmed.afterClosed().subscribe(result => {
      if (result && this.ticket) {
        this.ticketService.delete(this.ticket.id).subscribe({
          next: () => this.router.navigate(['/project', this.ticket!.project.id, 'kanban'])
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
        this.populateEditForm();
      }
    });
  }

  setActiveTab(tab: 'history' | 'comments'): void {
    this.activeTab = tab;
  }
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
