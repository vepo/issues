import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CreateTicketRequest } from '../../services/ticket.service';
import { Category } from '../../services/category.service';
import { Project } from '../../services/projects.service';

export interface TicketFormValues {
  title: string;
  description: string;
  categoryId: number;
  projectId: number;
  priority: CreateTicketRequest['priority'];
}

export interface TicketFormDefaults {
  title?: string;
  description?: string;
  categoryId?: number;
  priority?: CreateTicketRequest['priority'];
}

@Component({
  selector: 'app-ticket-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
  templateUrl: './ticket-form.component.html'
})
export class TicketFormComponent implements OnInit {
  @Input() projects: Project[] = [];
  @Input() categories: Category[] = [];
  @Input() initialProjectId: number | null = null;
  @Input() lockProject = false;
  @Input() isSaving = false;

  @Output() submitted = new EventEmitter<CreateTicketRequest>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() projectSelected = new EventEmitter<number>();

  readonly priorities: CreateTicketRequest['priority'][] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  ticketForm = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]),
    projectId: new FormControl(-1, [Validators.required, Validators.min(1)]),
    description: new FormControl('', [Validators.required, Validators.minLength(5), Validators.maxLength(1200)]),
    categoryId: new FormControl(-1, [Validators.required, Validators.min(1)]),
    priority: new FormControl<CreateTicketRequest['priority']>('MEDIUM', [Validators.required]),
  });

  @Input()
  set defaults(value: TicketFormDefaults | null) {
    if (!value) {
      return;
    }
    this.ticketForm.patchValue({
      title: value.title ?? '',
      description: value.description ?? '',
      categoryId: value.categoryId ?? -1,
      priority: value.priority ?? 'MEDIUM',
    });
  }

  ngOnInit(): void {
    if (this.initialProjectId != null && this.initialProjectId > 0) {
      this.ticketForm.patchValue({ projectId: this.initialProjectId });
      if (this.lockProject) {
        this.ticketForm.get('projectId')?.disable();
      }
      this.projectSelected.emit(this.initialProjectId);
    }

    this.ticketForm.get('projectId')?.valueChanges.subscribe(projectId => {
      if (projectId != null && projectId > 0) {
        this.projectSelected.emit(projectId);
      }
    });
  }

  get title() {
    return this.ticketForm.get('title');
  }

  get description() {
    return this.ticketForm.get('description');
  }

  get categoryId() {
    return this.ticketForm.get('categoryId');
  }

  get projectId() {
    return this.ticketForm.get('projectId');
  }

  submit(): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      return;
    }
    const { title, description, categoryId, projectId, priority } = this.ticketForm.getRawValue();
    if (!title || !description || categoryId == null || categoryId < 1 || projectId == null || projectId < 1 || !priority) {
      return;
    }
    this.submitted.emit({ title, description, categoryId, projectId, priority });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
