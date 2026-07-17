import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UpdateWorkflowRequest, Workflow, WorkflowService } from '../../services/workflow.service';
import { WorkflowFormComponent, WorkflowFormValue } from './workflow-form.component';

@Component({
  selector: 'app-workflow-edit',
  imports: [TranslocoPipe, WorkflowFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">{{ 'migration.workflow-edit.a8f4c39c35f3' | transloco }}</h1>
          <p class="page-subtitle">{{ 'migration.workflow-edit.87e4b6a5adfa' | transloco }}</p>
        </div>
      </header>
      <section class="edit page-panel">
        @if (loading) {
          <div class="loading">{{ 'migration.workflow-edit.b5aabad5bf28' | transloco }}</div>
        } @else if (error) {
          <div class="error" role="alert">{{ error }}</div>
        } @else if (workflow) {
          <app-workflow-form
            mode="edit"
            [initialWorkflow]="workflow"
            [isSaving]="isSaving"
            (submitted)="save($event)"
            (cancelled)="cancel()" />
        }
      </section>
    </div>
  `
})
export class WorkflowEditComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly workflowService = inject(WorkflowService);
  private readonly router = inject(Router);

  workflow: Workflow | null = null;
  loading = true;
  error = '';
  isSaving = false;

  ngOnInit(): void {
    const workflowId = Number(this.activatedRoute.snapshot.paramMap.get('workflowId'));
    this.workflowService.findById(workflowId).subscribe({
      next: workflow => {
        this.workflow = workflow;
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar o processo.';
        this.loading = false;
      }
    });
  }

  save(value: WorkflowFormValue): void {
    if (!this.workflow?.id) {
      return;
    }
    const request: UpdateWorkflowRequest = {
      name: value.name,
      statuses: value.statuses,
      start: value.start,
      phaseStart: value.phaseStart ?? undefined,
      transitions: value.transitions,
      finishStatuses: value.finishStatuses,
      wipLimits: value.wipLimits,
      statusReplacements: value.statusReplacements?.length ? value.statusReplacements : undefined
    };
    this.isSaving = true;
    this.workflowService.update(this.workflow.id, request).subscribe({
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
