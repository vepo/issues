import { Component, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, switchMap, of } from 'rxjs';
import { Category } from '../../services/category.service';
import { CreateOrUpdateProjectRequest, Project, ProjectsService } from '../../services/projects.service';
import { User, UsersService } from '../../services/users.service';
import { Workflow } from '../../services/workflow.service';
import { AuthService } from '../../services/auth.service';
import { CustomFieldAdminComponent } from '../custom-fields/custom-field-admin.component';
import { CustomFieldFormSectionComponent } from '../custom-fields/custom-field-form-section.component';
import { CustomField, CustomFieldService } from '../../services/custom-field.service';
import { CustomFieldValueResponse } from '../../generated/model/customFieldValueResponse';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';
import { optionalPlainTextLengthValidator } from '../../core/plain-text-length';
import { GitProvider, GitService } from '../../services/git.service';
import { ToastService } from '../../services/toast.service';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-project-edit.component',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatIconModule,
    TranslocoPipe,
    CustomFieldAdminComponent,
    CustomFieldFormSectionComponent,
    RichTextEditorComponent,
  ],
  templateUrl: './project-edit.component.html'
})
export class ProjectEditComponent implements OnInit, OnDestroy {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly usersService = inject(UsersService);
  private readonly authService = inject(AuthService);
  private readonly customFieldService = inject(CustomFieldService);
  private readonly gitService = inject(GitService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  @ViewChild('templateCustomFields') templateCustomFields?: CustomFieldFormSectionComponent;

  private templateEnabledSubscription?: Subscription;

  editMode = false;
  projectId: number | null = null;
  prefixLocked = false;
  workflows: Workflow[] = [];
  categories: Category[] = [];
  projectManagers: User[] = [];
  showOwnerPicker = false;
  inScopeFields: CustomField[] = [];
  templateCustomDefaults: CustomFieldValueResponse[] = [];
  readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
  readonly securityLevels = [
    { value: 'PRIVATE' as const, label: 'Privado', help: 'só membros do projeto' },
    { value: 'INTERNAL' as const, label: 'Interno', help: 'qualquer usuário autenticado' },
    { value: 'PUBLIC' as const, label: 'Público', help: 'também visitante sem login' },
  ];
  readonly gitProviders: { value: GitProvider; label: string }[] = [
    { value: 'GITHUB', label: 'GitHub' },
    { value: 'GITLAB', label: 'GitLab' },
    { value: 'GITEA', label: 'Gitea' },
    { value: 'OTHER', label: 'Outro' },
  ];
  webhookUrl = '';
  hasGitSecret = false;
  displayedWebhookSecret: string | null = null;
  regeneratingSecret = false;

  projectForm = new FormGroup({
    name: new FormControl('', Validators.required),
    description: new FormControl('', Validators.required),
    prefix: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(10)]),
    workflow: new FormControl(-1, [Validators.required, Validators.min(1)]),
    securityLevel: new FormControl<'PRIVATE' | 'INTERNAL' | 'PUBLIC'>('INTERNAL', Validators.required),
    templateEnabled: new FormControl(false),
    templateTitle: new FormControl(''),
    templateDescription: new FormControl(''),
    templateCategoryId: new FormControl(-1),
    templatePriority: new FormControl<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | null>(null),
    phaseTemplateObjective: new FormControl(''),
    phaseTemplateDeliverables: new FormControl(''),
    ownerId: new FormControl<number | null>(null),
  });

  gitForm = new FormGroup({
    remoteUrl: new FormControl(''),
    provider: new FormControl<GitProvider>('GITHUB', Validators.required),
    defaultBranch: new FormControl(''),
  });

  ngOnInit(): void {
    this.templateEnabledSubscription = this.projectForm.controls.templateEnabled.valueChanges.subscribe(enabled => {
      this.updateTemplateValidators(enabled ?? false);
    });

    this.activatedRoute.data.subscribe(({ project, workflows, categories }) => {
      this.workflows = workflows ?? [];
      this.categories = categories ?? [];
      this.editMode = project != null;
      this.projectId = project?.id ?? null;

      if (project) {
        const template = project.ticketTemplate;
        const owner = (project as Project & { owner?: { id: number } }).owner;
        this.prefixLocked = project.prefixLocked === true;
        this.showOwnerPicker = this.canPickOwner(owner?.id);
        if (this.showOwnerPicker) {
          this.usersService.search({ name: '', email: '', roles: ['project-manager'] }).subscribe(users => {
            this.projectManagers = users;
          });
        }
        this.projectForm.patchValue({
          name: project.name,
          description: project.description ?? '',
          prefix: project.prefix,
          workflow: project.workflow?.id ?? -1,
          securityLevel: (project as Project & { securityLevel?: 'PRIVATE' | 'INTERNAL' | 'PUBLIC' }).securityLevel ?? 'INTERNAL',
          ownerId: owner?.id ?? null,
          templateEnabled: template?.enabled ?? false,
          templateTitle: template?.title ?? '',
          templateDescription: template?.description ?? '',
          templateCategoryId: template?.categoryId ?? -1,
          templatePriority: template?.priority ?? null,
          phaseTemplateObjective: project.phaseTemplate?.objective ?? '',
          phaseTemplateDeliverables: (project.phaseTemplate?.deliverables ?? []).join('\n'),
        });
        if (this.prefixLocked) {
          this.projectForm.controls.prefix.disable({ emitEvent: false });
        } else {
          this.projectForm.controls.prefix.enable({ emitEvent: false });
        }
        this.updateTemplateValidators(template?.enabled ?? false);
        this.templateCustomDefaults = template?.customFieldDefaults ?? [];
        this.loadInScopeFields(project.id);
        this.loadGitAssociation(project.id);
      }
    });
  }

  private loadGitAssociation(projectId: number): void {
    this.gitService.get(projectId).subscribe(repo => {
      if (!repo) {
        this.webhookUrl = '';
        this.hasGitSecret = false;
        return;
      }
      this.gitForm.patchValue({
        remoteUrl: repo.remoteUrl,
        provider: repo.provider,
        defaultBranch: repo.defaultBranch ?? '',
      });
      this.webhookUrl = repo.webhookUrl;
      this.hasGitSecret = repo.hasSecret;
    });
  }

  private loadInScopeFields(projectId: number): void {
    this.customFieldService.listInScope(projectId).subscribe(fields => {
      this.inScopeFields = fields;
    });
  }

  ngOnDestroy(): void {
    this.templateEnabledSubscription?.unsubscribe();
  }

  cancel(): void {
    if (this.projectId) {
      void this.router.navigate(['/', 'projects', this.projectId]);
      return;
    }
    void this.router.navigate(['/', 'projects']);
  }

  canPickOwner(currentOwnerId?: number): boolean {
    if (this.authService.hasRole('admin')) {
      return true;
    }
    return currentOwnerId != null && currentOwnerId === this.authService.getAuthUserId();
  }

  save(): void {
    if (this.projectForm.invalid) {
      return;
    }
    const {
      name,
      description,
      prefix,
      workflow,
      securityLevel,
      templateEnabled,
      templateTitle,
      templateDescription,
      templateCategoryId,
      templatePriority,
      phaseTemplateObjective,
      phaseTemplateDeliverables,
      ownerId,
    } = this.projectForm.getRawValue();

    if (!name || !description || !prefix || workflow == null || workflow < 1 || !securityLevel) {
      return;
    }

    const deliverables = (phaseTemplateDeliverables ?? '')
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0);

    if (templateEnabled && this.templateCustomFields && !this.templateCustomFields.isValid()) {
      return;
    }

    const request: CreateOrUpdateProjectRequest = {
      name,
      description,
      prefix,
      workflowId: workflow,
      securityLevel,
      ticketTemplate: templateEnabled
        ? {
            enabled: true,
            title: templateTitle?.trim() || undefined,
            description: templateDescription?.trim() || undefined,
            categoryId: templateCategoryId != null && templateCategoryId > 0 ? templateCategoryId : undefined,
            priority: templatePriority ?? undefined,
            customFieldDefaults: this.templateCustomFields?.toValueRequests() ?? [],
          }
        : { enabled: false },
      phaseTemplate: {
        objective: phaseTemplateObjective?.trim() || undefined,
        deliverables,
      },
      ownerId: ownerId ?? undefined,
    };

    if (this.projectId) {
      this.projectsService.update(this.projectId, request)
        .pipe(switchMap(() => this.persistGitAssociation(this.projectId!)))
        .subscribe(() => void this.router.navigate(['/', 'projects', this.projectId]));
    } else {
      this.projectsService.create(request)
        .subscribe(() => void this.router.navigate(['/', 'projects']));
    }
  }

  private persistGitAssociation(projectId: number) {
    const { remoteUrl, provider, defaultBranch } = this.gitForm.getRawValue();
    const trimmedUrl = remoteUrl?.trim();
    if (!trimmedUrl || !provider) {
      return of(undefined);
    }
    return this.gitService.put(projectId, {
      remoteUrl: trimmedUrl,
      provider,
      defaultBranch: defaultBranch?.trim() || undefined,
    }).pipe(switchMap(response => {
      this.webhookUrl = response.webhookUrl;
      this.hasGitSecret = response.hasSecret;
      if (response.webhookSecret) {
        this.displayedWebhookSecret = response.webhookSecret;
        this.toastService.success('Repositório Git salvo. Copie o segredo do webhook agora.');
      }
      return of(undefined);
    }));
  }

  regenerateWebhookSecret(): void {
    if (!this.projectId || this.regeneratingSecret) {
      return;
    }
    this.regeneratingSecret = true;
    this.gitService.regenerateSecret(this.projectId).subscribe({
      next: response => {
        this.regeneratingSecret = false;
        this.webhookUrl = response.webhookUrl;
        this.hasGitSecret = response.hasSecret;
        if (response.webhookSecret) {
          this.displayedWebhookSecret = response.webhookSecret;
          this.toastService.success('Segredo regenerado. Copie o novo valor agora.');
        }
      },
      error: () => {
        this.regeneratingSecret = false;
        this.toastService.error('Não foi possível regenerar o segredo.');
      },
    });
  }

  copyWebhookUrl(): void {
    if (!this.webhookUrl) {
      return;
    }
    navigator.clipboard.writeText(this.webhookUrl).then(
      () => this.toastService.success('URL copiada para a área de transferência.'),
      () => this.toastService.error('Não foi possível copiar.'),
    );
  }

  copyWebhookSecret(): void {
    if (!this.displayedWebhookSecret) {
      return;
    }
    navigator.clipboard.writeText(this.displayedWebhookSecret).then(
      () => this.toastService.success('Segredo copiado para a área de transferência.'),
      () => this.toastService.error('Não foi possível copiar.'),
    );
  }

  dismissWebhookSecret(): void {
    this.displayedWebhookSecret = null;
  }

  secretStatusLabel(): string {
    if (this.displayedWebhookSecret) {
      return this.displayedWebhookSecret;
    }
    if (this.hasGitSecret) {
      return 'configurado';
    }
    return '—';
  }

  private updateTemplateValidators(enabled: boolean): void {
    const title = this.projectForm.controls.templateTitle;
    const templateDescription = this.projectForm.controls.templateDescription;
    const categoryId = this.projectForm.controls.templateCategoryId;

    if (enabled) {
      title.setValidators([Validators.minLength(5), Validators.maxLength(255)]);
      templateDescription.setValidators([optionalPlainTextLengthValidator(5, 1200)]);
      categoryId.clearValidators();
    } else {
      title.clearValidators();
      templateDescription.clearValidators();
      categoryId.clearValidators();
    }

    title.updateValueAndValidity({ emitEvent: false });
    templateDescription.updateValueAndValidity({ emitEvent: false });
    categoryId.updateValueAndValidity({ emitEvent: false });
  }
}
