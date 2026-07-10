import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Category } from '../../services/category.service';
import { Project, ProjectsService } from '../../services/projects.service';
import { CreateTicketRequest, TicketService } from '../../services/ticket.service';
import { TicketFormComponent, TicketFormDefaults } from '../ticket-form/ticket-form.component';

@Component({
  selector: 'app-create-ticket',
  imports: [TicketFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title" i18n>Novo ticket</h1>
          <p class="page-subtitle" i18n>Preencha os dados do ticket. Campos podem ser pré-preenchidos pelo template do projeto.</p>
        </div>
      </header>
      <section class="edit page-panel">
        <app-ticket-form
          [projects]="projects"
          [categories]="categories"
          [initialProjectId]="initialProjectId"
          [lockProject]="lockProject"
          [defaults]="formDefaults"
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

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ projects, categories, project }) => {
      this.projects = projects ?? [];
      this.categories = categories ?? [];
      const routeProjectId = this.activatedRoute.snapshot.paramMap.get('projectId');
      if (project?.id) {
        this.initialProjectId = project.id;
        this.lockProject = true;
        this.applyTemplateFromProject(project);
      } else if (routeProjectId) {
        this.initialProjectId = Number(routeProjectId);
        this.lockProject = true;
        this.projectsService.findById(this.initialProjectId).subscribe(p => this.applyTemplateFromProject(p));
      }
    });
  }

  onProjectSelected(projectId: number): void {
    const project = this.projects.find(p => p.id === projectId);
    if (project) {
      this.applyTemplateFromProject(project);
      return;
    }
    this.projectsService.findById(projectId).subscribe(p => this.applyTemplateFromProject(p));
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
      this.formDefaults = defaults;
    } else {
      this.formDefaults = { priority: 'MEDIUM' };
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
