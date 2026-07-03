import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Workflow } from '../../services/workflow.service';

export type WorkflowFormMode = 'create' | 'edit';

export interface WorkflowFormValue {
  name: string;
  statuses: string[];
  start: string;
  transitions: { from: string; to: string }[];
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
    start: ['TODO', Validators.required],
    transitions: this.formBuilder.array([
      this.createTransitionGroup('TODO', 'In Progress')
    ])
  });

  ngOnInit(): void {
    if (this.mode === 'edit' && this.initialWorkflow) {
      this.patchWorkflow(this.initialWorkflow);
    }
  }

  get statuses(): FormArray {
    return this.workflowForm.get('statuses') as FormArray;
  }

  get transitions(): FormArray {
    return this.workflowForm.get('transitions') as FormArray;
  }

  statusNames(): string[] {
    if (this.mode === 'edit' && this.initialWorkflow?.statuses) {
      return this.initialWorkflow.statuses;
    }
    return this.statuses.controls
      .map(control => (control.value as string)?.trim())
      .filter(name => !!name);
  }

  createTransitionGroup(from = '', to = ''): FormGroup {
    return this.formBuilder.group({
      from: [from, Validators.required],
      to: [to, Validators.required]
    });
  }

  addStatus(): void {
    this.statuses.push(this.formBuilder.control('', Validators.required));
  }

  removeStatus(index: number): void {
    if (this.statuses.length > 2) {
      this.statuses.removeAt(index);
    }
  }

  addTransition(): void {
    this.transitions.push(this.createTransitionGroup());
  }

  removeTransition(index: number): void {
    this.transitions.removeAt(index);
  }

  submit(): void {
    if (this.workflowForm.invalid) {
      return;
    }
    const value = this.workflowForm.value;
    this.submitted.emit({
      name: value.name,
      statuses: this.mode === 'edit' ? this.initialWorkflow!.statuses! : value.statuses,
      start: value.start,
      transitions: value.transitions
    });
  }

  private patchWorkflow(workflow: Workflow): void {
    this.workflowForm.patchValue({
      name: workflow.name,
      start: workflow.start
    });
    this.statuses.clear();
    (workflow.statuses ?? []).forEach(status => {
      this.statuses.push(this.formBuilder.control({ value: status, disabled: true }, Validators.required));
    });
    this.transitions.clear();
    (workflow.transitions ?? []).forEach(transition => {
      this.transitions.push(this.createTransitionGroup(transition.from ?? '', transition.to ?? ''));
    });
    if (this.transitions.length === 0) {
      this.addTransition();
    }
  }
}
