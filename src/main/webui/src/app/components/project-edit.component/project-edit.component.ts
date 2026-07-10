import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Category } from '../../services/category.service';
import { CreateOrUpdateProjectRequest, Project, ProjectsService } from '../../services/projects.service';
import { User, UsersService } from '../../services/users.service';
import { Workflow } from '../../services/workflow.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-project-edit.component',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule, MatCheckboxModule],
  templateUrl: './project-edit.component.html'
})
export class ProjectEditComponent implements OnInit, OnDestroy {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly usersService = inject(UsersService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private templateEnabledSubscription?: Subscription;

  editMode = false;
  projectId: number | null = null;
  workflows: Workflow[] = [];
  categories: Category[] = [];
  projectManagers: User[] = [];
  showOwnerPicker = false;
  readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

  projectForm = new FormGroup({
    name: new FormControl('', Validators.required),
    description: new FormControl('', Validators.required),
    prefix: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(10)]),
    workflow: new FormControl(-1, [Validators.required, Validators.min(1)]),
    templateEnabled: new FormControl(false),
    templateTitle: new FormControl(''),
    templateDescription: new FormControl(''),
    templateCategoryId: new FormControl(-1),
    templatePriority: new FormControl<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | null>(null),
    phaseTemplateObjective: new FormControl(''),
    phaseTemplateDeliverables: new FormControl(''),
    ownerId: new FormControl<number | null>(null),
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
          ownerId: owner?.id ?? null,
          templateEnabled: template?.enabled ?? false,
          templateTitle: template?.title ?? '',
          templateDescription: template?.description ?? '',
          templateCategoryId: template?.categoryId ?? -1,
          templatePriority: template?.priority ?? null,
          phaseTemplateObjective: project.phaseTemplate?.objective ?? '',
          phaseTemplateDeliverables: (project.phaseTemplate?.deliverables ?? []).join('\n'),
        });
        this.updateTemplateValidators(template?.enabled ?? false);
      }
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
      templateEnabled,
      templateTitle,
      templateDescription,
      templateCategoryId,
      templatePriority,
      phaseTemplateObjective,
      phaseTemplateDeliverables,
      ownerId,
    } = this.projectForm.value;

    if (!name || !description || !prefix || workflow == null || workflow < 1) {
      return;
    }

    const deliverables = (phaseTemplateDeliverables ?? '')
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0);

    const request: CreateOrUpdateProjectRequest = {
      name,
      description,
      prefix,
      workflowId: workflow,
      ticketTemplate: templateEnabled
        ? {
            enabled: true,
            title: templateTitle?.trim() || undefined,
            description: templateDescription?.trim() || undefined,
            categoryId: templateCategoryId != null && templateCategoryId > 0 ? templateCategoryId : undefined,
            priority: templatePriority ?? undefined,
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
        .subscribe(() => void this.router.navigate(['/', 'projects', this.projectId]));
    } else {
      this.projectsService.create(request)
        .subscribe(() => void this.router.navigate(['/', 'projects']));
    }
  }

  private updateTemplateValidators(enabled: boolean): void {
    const title = this.projectForm.controls.templateTitle;
    const templateDescription = this.projectForm.controls.templateDescription;
    const categoryId = this.projectForm.controls.templateCategoryId;

    if (enabled) {
      title.setValidators([Validators.minLength(5), Validators.maxLength(255)]);
      templateDescription.setValidators([Validators.minLength(5), Validators.maxLength(1200)]);
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
