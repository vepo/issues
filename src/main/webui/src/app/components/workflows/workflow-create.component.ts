import { TranslocoPipe } from '@jsverse/transloco';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CreateWorkflowRequest, WorkflowService } from '../../services/workflow.service';
import { WorkflowFormComponent, WorkflowFormValue } from './workflow-form.component';

@Component({
  selector: 'app-workflow-create',
  imports: [TranslocoPipe, WorkflowFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">{{ 'migration.workflow-create.c6cc29265f11' | transloco }}</h1>
          <p class="page-subtitle">{{ 'migration.workflow-create.ac5a2cf49ba6' | transloco }}</p>
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
      phaseStart: value.phaseStart ?? undefined,
      transitions: value.transitions,
      finishStatuses: value.finishStatuses,
      wipLimits: value.wipLimits
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
