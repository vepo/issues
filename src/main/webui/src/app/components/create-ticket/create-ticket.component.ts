import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Observable, of } from 'rxjs';
import { Category } from '../../services/category.service';
import { Project, ProjectsService } from '../../services/projects.service';
import {
  CloneTicketPrefill,
  CreateTicketRequest,
  TicketService,
  TicketType,
} from '../../services/ticket.service';
import { TicketFormComponent, TicketFormDefaults } from '../ticket-form/ticket-form.component';

@Component({
  selector: 'app-create-ticket',
  imports: [TranslocoPipe, TicketFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">{{ 'ticket.create.title' | transloco }}</h1>
          <p class="page-subtitle">{{ 'ticket.create.subtitle' | transloco }}</p>
        </div>
      </header>
      @if (cloneWarnings.length > 0) {
        <aside class="form-hint" role="alert">
          <ul>
            @for (warning of cloneWarnings; track warning) {
              <li>{{ warning }}</li>
            }
          </ul>
        </aside>
      }
      <section class="edit page-panel">
        <app-ticket-form
          [projects]="projects"
          [categories]="categories"
          [initialProjectId]="initialProjectId"
          [lockProject]="lockProject"
          [defaults]="formDefaults"
          [labels]="{
            project: ('ticket.create.project' | transloco),
            title: ('ticket.create.ticketTitle' | transloco),
            create: ('ticket.create.createAction' | transloco)
          }"
          [isSaving]="isSaving"
          (projectSelected)="onProjectSelected($event)"
          (submitted)="save($event)"
          (cancelled)="cancel()" />
      </section>
    </div>
  `
})
export class CreateTicketComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly projectsService = inject(ProjectsService);

  projects: Project[] = [];
  categories: Category[] = [];
  initialProjectId: number | null = null;
  lockProject = false;
  formDefaults: TicketFormDefaults | null = null;
  isSaving = false;
  cloneWarnings: string[] = [];
  private cloneSourceId: number | null = null;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ projects, categories, project }) => {
      this.projects = projects ?? [];
      this.categories = categories ?? [];
      if (!this.activatedRoute.queryParamMap) {
        this.applyRouteProject(project);
        return;
      }
      this.activatedRoute.queryParamMap.subscribe(queryParams => {
        const sourceTicketId = Number(queryParams.get('cloneFrom'));
        const targetProjectId = Number(queryParams.get('targetProjectId'));
        if (sourceTicketId > 0 && targetProjectId > 0) {
          this.initializeClone(sourceTicketId, targetProjectId);
          return;
        }

        this.applyRouteProject(project);
      });
    });
  }

  private applyRouteProject(project?: Project): void {
    const routeProjectId = this.activatedRoute.snapshot.paramMap.get('projectId');
    if (project?.id) {
      this.initialProjectId = project.id;
      this.lockProject = true;
      this.applyTemplateFromProject(project);
    } else if (routeProjectId) {
      this.initialProjectId = Number(routeProjectId);
      this.lockProject = true;
      this.applyDefaultsForProject(this.initialProjectId);
    }
  }

  onProjectSelected(projectId: number): void {
    this.applyDefaultsForProject(projectId);
  }

  private initializeClone(sourceTicketId: number, targetProjectId: number): void {
    this.cloneSourceId = sourceTicketId;
    this.initialProjectId = targetProjectId;
    this.lockProject = false;
    this.projectsService.listWritable().subscribe(writableProjects => {
      this.projects = writableProjects;
      this.applyDefaultsForProject(targetProjectId);
    });
  }

  private applyDefaultsForProject(projectId: number): void {
    this.findProject(projectId).subscribe(project => {
      this.applyTemplateFromProject(project);
      if (this.cloneSourceId) {
        this.loadClonePrefill(projectId);
      }
    });
  }

  private findProject(projectId: number): Observable<Project> {
    const project = this.projects.find(candidate => candidate.id === projectId);
    return project ? of(project) : this.projectsService.findById(projectId);
  }

  private loadClonePrefill(targetProjectId: number): void {
    if (!this.cloneSourceId) {
      return;
    }
    this.ticketService.getClonePrefill(this.cloneSourceId, targetProjectId)
      .subscribe(prefill => this.applyClonePrefill(prefill));
  }

  private applyClonePrefill(prefill: CloneTicketPrefill): void {
    const customFields = new Map(
      (this.formDefaults?.customFieldDefaults ?? [])
        .filter(value => !!value.key)
        .map(value => [value.key!, value]),
    );
    for (const value of prefill.customFields ?? []) {
      if (value.key) {
        customFields.set(value.key, value);
      }
    }
    this.formDefaults = {
      ...this.formDefaults,
      title: prefill.title,
      description: prefill.description,
      categoryId: prefill.categoryId,
      priority: prefill.priority as CreateTicketRequest['priority'],
      ticketType: prefill.ticketType as TicketType,
      customFieldDefaults: Array.from(customFields.values()),
    };
    this.cloneWarnings = prefill.warnings ?? [];
  }

  private applyTemplateFromProject(project: Project): void {
    const template = project.ticketTemplate;
    if (template?.enabled) {
      const defaults: TicketFormDefaults = {};
      if (template.title?.trim()) {
        defaults.title = template.title.trim();
      }
      if (template.description?.trim()) {
        defaults.description = template.description.trim();
      }
      if (template.categoryId != null && template.categoryId > 0) {
        defaults.categoryId = template.categoryId;
      }
      if (template.priority) {
        defaults.priority = template.priority;
      }
      defaults.customFieldDefaults = template.customFieldDefaults ?? [];
      this.formDefaults = defaults;
    } else {
      this.formDefaults = { priority: 'MEDIUM', customFieldDefaults: [] };
    }
  }

  save(request: CreateTicketRequest): void {
    this.isSaving = true;
    this.ticketService.createTicket(request).subscribe({
      next: ticket => {
        this.isSaving = false;
        void this.router.navigate(['/', 'ticket', ticket.identifier]);
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }

  cancel(): void {
    if (this.lockProject && this.initialProjectId) {
      void this.router.navigate(['/project', this.initialProjectId, 'kanban']);
      return;
    }
    void this.router.navigate(['/']);
  }
}
