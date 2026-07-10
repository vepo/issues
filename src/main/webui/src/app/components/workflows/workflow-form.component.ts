import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FinishOutcome } from '../../generated/model/finishOutcome';
import { Workflow } from '../../services/workflow.service';

export type WorkflowFormMode = 'create' | 'edit';

export interface WorkflowFormValue {
  name: string;
  statuses: string[];
  start: string;
  phaseStart: string | null;
  transitions: { from: string; to: string }[];
  finishStatuses: { status: string; outcome: FinishOutcome }[];
  wipLimits: { status: string; wipLimit: number }[];
}

@Component({
  selector: 'app-workflow-form',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './workflow-form.component.html',
  styleUrl: './workflow-form.component.scss'
})
export class WorkflowFormComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);

  @Input() mode: WorkflowFormMode = 'create';
  @Input() initialWorkflow: Workflow | null = null;
  @Input() isSaving = false;
  @Output() submitted = new EventEmitter<WorkflowFormValue>();
  @Output() cancelled = new EventEmitter<void>();

  workflowForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(5)]],
    statuses: this.formBuilder.array([
      this.formBuilder.control('TODO', Validators.required),
      this.formBuilder.control('In Progress', Validators.required)
    ]),
    statusWips: this.formBuilder.array([
      this.formBuilder.control<number | null>(null),
      this.formBuilder.control<number | null>(null)
    ]),
    start: ['TODO', Validators.required],
    phaseStart: [null as string | null],
    transitions: this.formBuilder.array([
      this.createTransitionGroup('TODO', 'In Progress')
    ]),
    finishStatuses: this.formBuilder.array([])
  });

  readonly finishOutcomes: FinishOutcome[] = ['DONE', 'CANCELED'];

  ngOnInit(): void {
    if (this.mode === 'edit' && this.initialWorkflow) {
      this.patchWorkflow(this.initialWorkflow);
    }
  }

  get statuses(): FormArray {
    return this.workflowForm.get('statuses') as FormArray;
  }

  get statusWips(): FormArray {
    return this.workflowForm.get('statusWips') as FormArray;
  }

  get transitions(): FormArray {
    return this.workflowForm.get('transitions') as FormArray;
  }

  get finishStatuses(): FormArray {
    return this.workflowForm.get('finishStatuses') as FormArray;
  }

  statusNames(): string[] {
    if (this.mode === 'edit' && this.initialWorkflow?.statuses) {
      return this.initialWorkflow.statuses;
    }
    return this.statuses.controls
      .map(control => (control.value as string)?.trim())
      .filter(name => !!name);
  }

  wipControlAt(index: number): FormControl<number | null> {
    return this.statusWips.at(index) as FormControl<number | null>;
  }

  createTransitionGroup(from = '', to = ''): FormGroup {
    return this.formBuilder.group({
      from: [from, Validators.required],
      to: [to, Validators.required]
    });
  }

  createFinishStatusGroup(status = '', outcome: FinishOutcome = 'DONE'): FormGroup {
    return this.formBuilder.group({
      status: [status, Validators.required],
      outcome: [outcome, Validators.required]
    });
  }

  addStatus(): void {
    this.statuses.push(this.formBuilder.control('', Validators.required));
    this.statusWips.push(this.formBuilder.control<number | null>(null));
  }

  removeStatus(index: number): void {
    if (this.statuses.length > 2) {
      this.statuses.removeAt(index);
      this.statusWips.removeAt(index);
    }
  }

  addTransition(): void {
    this.transitions.push(this.createTransitionGroup());
  }

  removeTransition(index: number): void {
    this.transitions.removeAt(index);
  }

  addFinishStatus(): void {
    this.finishStatuses.push(this.createFinishStatusGroup());
  }

  removeFinishStatus(index: number): void {
    this.finishStatuses.removeAt(index);
  }

  submit(): void {
    if (this.workflowForm.invalid) {
      return;
    }
    const value = this.workflowForm.value;
    const names = this.mode === 'edit' ? this.initialWorkflow!.statuses! : value.statuses;
    const wipLimits = (names as string[])
      .map((status, index) => {
        const raw = this.statusWips.at(index)?.value;
        const wipLimit = raw === null || raw === undefined || raw === '' ? null : Number(raw);
        return { status, wipLimit };
      })
      .filter((entry): entry is { status: string; wipLimit: number } =>
        entry.wipLimit != null && !Number.isNaN(entry.wipLimit) && entry.wipLimit >= 1);

    this.submitted.emit({
      name: value.name,
      statuses: names,
      start: value.start,
      phaseStart: value.phaseStart || undefined,
      transitions: value.transitions,
      finishStatuses: value.finishStatuses ?? [],
      wipLimits
    });
  }

  private patchWorkflow(workflow: Workflow): void {
    this.workflowForm.patchValue({
      name: workflow.name,
      start: workflow.start,
      phaseStart: workflow.phaseStart ?? null
    });
    this.statuses.clear();
    this.statusWips.clear();
    const wipByStatus = new Map(
      (workflow.wipLimits ?? []).map(wip => [wip.status ?? '', wip.wipLimit ?? null])
    );
    (workflow.statuses ?? []).forEach(status => {
      this.statuses.push(this.formBuilder.control({ value: status, disabled: true }, Validators.required));
      this.statusWips.push(this.formBuilder.control<number | null>(wipByStatus.get(status) ?? null));
    });
    this.transitions.clear();
    (workflow.transitions ?? []).forEach(transition => {
      this.transitions.push(this.createTransitionGroup(transition.from ?? '', transition.to ?? ''));
    });
    if (this.transitions.length === 0) {
      this.addTransition();
    }
    this.finishStatuses.clear();
    (workflow.finishStatuses ?? []).forEach(finishStatus => {
      this.finishStatuses.push(this.createFinishStatusGroup(finishStatus.status ?? '', finishStatus.outcome ?? 'DONE'));
    });
  }
}
