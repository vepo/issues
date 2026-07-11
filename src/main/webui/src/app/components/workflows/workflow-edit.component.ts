import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UpdateWorkflowRequest, Workflow, WorkflowService } from '../../services/workflow.service';
import { WorkflowFormComponent, WorkflowFormValue } from './workflow-form.component';

@Component({
  selector: 'app-workflow-edit',
  imports: [WorkflowFormComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title" i18n>Editar processo</h1>
          <p class="page-subtitle" i18n>Altere nome, status, transições e remapeie tickets se necessário</p>
        </div>
      </header>
      <section class="edit page-panel">
        @if (loading) {
          <div class="loading" i18n>Carregando processo...</div>
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
