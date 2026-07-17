import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FinishOutcome } from '../../generated/model/finishOutcome';
import { Workflow } from '../../services/workflow.service';
import { CustomFieldAdminComponent } from '../custom-fields/custom-field-admin.component';

export type WorkflowFormMode = 'create' | 'edit';

export interface StatusReplacementValue {
  from: string;
  to: string;
}

export interface WorkflowFormValue {
  name: string;
  statuses: string[];
  start: string;
  phaseStart: string | null;
  transitions: { from: string; to: string }[];
  finishStatuses: { status: string; outcome: FinishOutcome }[];
  wipLimits: { status: string; wipLimit: number }[];
  statusReplacements: StatusReplacementValue[];
}

@Component({
  selector: 'app-workflow-form',
  imports: [
    TranslocoPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    CustomFieldAdminComponent
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

  /** Explicit replacements collected when removing a status that existed on the workflow. */
  statusReplacements: StatusReplacementValue[] = [];

  pendingRemoval: { index: number; name: string; replacement: string } | null = null;

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
    return this.statuses.controls
      .map(control => (control.value as string)?.trim())
      .filter(name => !!name);
  }

  replacementCandidates(removedName: string): string[] {
    return this.statusNames().filter(name => name !== removedName);
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
    if (this.statuses.length <= 2 || this.pendingRemoval) {
      return;
    }
    const name = (this.statuses.at(index).value as string)?.trim();
    const wasExisting =
      this.mode === 'edit' &&
      !!name &&
      (this.initialWorkflow?.statuses ?? []).includes(name);
    if (wasExisting) {
      const candidates = this.replacementCandidates(name);
      this.pendingRemoval = {
        index,
        name,
        replacement: candidates[0] ?? ''
      };
      return;
    }
    this.applyStatusRemoval(index, name);
  }

  confirmPendingRemoval(): void {
    if (!this.pendingRemoval?.replacement) {
      return;
    }
    const { index, name, replacement } = this.pendingRemoval;
    this.statusReplacements = [
      ...this.statusReplacements.filter(r => r.from !== name),
      { from: name, to: replacement }
    ];
    this.pendingRemoval = null;
    this.applyStatusRemoval(index, name);
  }

  cancelPendingRemoval(): void {
    this.pendingRemoval = null;
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
    if (this.workflowForm.invalid || this.pendingRemoval) {
      return;
    }
    const value = this.workflowForm.getRawValue();
    const names = (value.statuses as string[]).map(s => s?.trim()).filter(Boolean);
    const wipLimits = names
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
      wipLimits,
      statusReplacements: this.buildStatusReplacements(names)
    });
  }

  private buildStatusReplacements(currentNames: string[]): StatusReplacementValue[] {
    const initial = this.initialWorkflow?.statuses ?? [];
    if (this.mode !== 'edit' || initial.length === 0) {
      return [];
    }
    const removed = initial.filter(name => !currentNames.includes(name));
    const added = currentNames.filter(name => !initial.includes(name));
    const explicit = [...this.statusReplacements];
    if (removed.length === 1 && added.length === 1 && !explicit.some(r => r.from === removed[0])) {
      explicit.push({ from: removed[0], to: added[0] });
    }
    return explicit.filter(r => removed.includes(r.from) && currentNames.includes(r.to));
  }

  private applyStatusRemoval(index: number, removedName: string | undefined): void {
    this.statuses.removeAt(index);
    this.statusWips.removeAt(index);
    if (!removedName) {
      return;
    }
    for (let i = this.transitions.length - 1; i >= 0; i--) {
      const group = this.transitions.at(i) as FormGroup;
      if (group.value.from === removedName || group.value.to === removedName) {
        this.transitions.removeAt(i);
      }
    }
    for (let i = this.finishStatuses.length - 1; i >= 0; i--) {
      const group = this.finishStatuses.at(i) as FormGroup;
      if (group.value.status === removedName) {
        this.finishStatuses.removeAt(i);
      }
    }
    const start = this.workflowForm.get('start')?.value;
    if (start === removedName) {
      this.workflowForm.patchValue({ start: this.statusNames()[0] ?? '' });
    }
    const phaseStart = this.workflowForm.get('phaseStart')?.value;
    if (phaseStart === removedName) {
      this.workflowForm.patchValue({ phaseStart: null });
    }
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
      this.statuses.push(this.formBuilder.control(status, Validators.required));
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
    this.statusReplacements = [];
    this.pendingRemoval = null;
  }
}
