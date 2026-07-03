import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CreateWorkflowRequest, WorkflowService } from '../../services/workflow.service';
import { WorkflowFormComponent, WorkflowFormValue } from './workflow-form.component';

@Component({
  selector: 'app-workflow-create',
  imports: [WorkflowFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title" i18n>Novo processo</h1>
          <p class="page-subtitle" i18n>Defina status, início e transições permitidas</p>
        </div>
      </header>
      <section class="edit page-panel">
        <app-workflow-form
          mode="create"
          [isSaving]="isSaving"
          (submitted)="save($event)"
          (cancelled)="cancel()" />
      </section>
    </div>
  `
})
export class WorkflowCreateComponent {
  private readonly workflowService = inject(WorkflowService);
  private readonly router = inject(Router);

  isSaving = false;

  save(value: WorkflowFormValue): void {
    const request: CreateWorkflowRequest = {
      name: value.name,
      statuses: value.statuses,
      start: value.start,
      transitions: value.transitions
    };
    this.isSaving = true;
    this.workflowService.create(request).subscribe({
      next: async () => {
        this.isSaving = false;
        await this.router.navigate(['/workflows']);
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }

  cancel(): void {
    void this.router.navigate(['/workflows']);
  }
}
