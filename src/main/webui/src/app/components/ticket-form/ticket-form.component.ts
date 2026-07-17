import { TranslocoPipe } from '@jsverse/transloco';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CreateTicketRequest, TicketType } from '../../services/ticket.service';
import { Category } from '../../services/category.service';
import { Project } from '../../services/projects.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { CustomFieldFormSectionComponent } from '../custom-fields/custom-field-form-section.component';
import { CustomFieldValueResponse } from '../../generated/model/customFieldValueResponse';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';
import { plainTextLengthValidator } from '../../core/plain-text-length';
import { TICKET_TYPE_OPTIONS } from '../../core/system-labels';

export interface TicketFormValues {
  title: string;
  description: string;
  categoryId: number;
  projectId: number;
  priority: CreateTicketRequest['priority'];
  ticketType: TicketType;
}

export interface TicketFormDefaults {
  title?: string;
  description?: string;
  categoryId?: number;
  priority?: CreateTicketRequest['priority'];
  ticketType?: TicketType;
  customFieldDefaults?: CustomFieldValueResponse[];
}

export interface TicketFormLabels {
  project: string;
  title: string;
  create: string;
}

@Component({
  selector: 'app-ticket-form',
  imports: [
    TranslocoPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    CustomFieldFormSectionComponent,
    RichTextEditorComponent,
  ],
  templateUrl: './ticket-form.component.html'
})
export class TicketFormComponent implements OnInit {
  private readonly phaseService = inject(PhaseService);

  @ViewChild(CustomFieldFormSectionComponent) customFieldsSection?: CustomFieldFormSectionComponent;

  @Input() projects: Project[] = [];
  @Input() categories: Category[] = [];
  @Input() initialProjectId: number | null = null;
  @Input() lockProject = false;
  @Input() isSaving = false;
  @Input() customFieldDefaults: CustomFieldValueResponse[] | null = null;
  @Input() labels: TicketFormLabels = {
    project: 'Projeto',
    title: 'Título',
    create: 'Criar',
  };

  @Output() submitted = new EventEmitter<CreateTicketRequest>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() projectSelected = new EventEmitter<number>();

  readonly priorities: CreateTicketRequest['priority'][] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly ticketTypes = TICKET_TYPE_OPTIONS;
  assignablePhases: Phase[] = [];
  selectedProjectId: number | null = null;

  ticketForm = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]),
    projectId: new FormControl(-1, [Validators.required, Validators.min(1)]),
    description: new FormControl('', [Validators.required, plainTextLengthValidator(5, 1200)]),
    categoryId: new FormControl(-1, [Validators.required, Validators.min(1)]),
    priority: new FormControl<CreateTicketRequest['priority']>('MEDIUM', [Validators.required]),
    ticketType: new FormControl<TicketType>('TASK', [Validators.required]),
    dueDate: new FormControl<string | null>(null),
    phaseId: new FormControl<number | null>(null),
    storyPoints: new FormControl<number | null>(null, [Validators.min(0)]),
  });

  @Input()
  set defaults(value: TicketFormDefaults | null) {
    if (!value) {
      return;
    }
    const patch = {
      title: '',
      description: '',
      categoryId: -1,
      priority: 'MEDIUM' as CreateTicketRequest['priority'],
      ticketType: 'TASK' as TicketType,
      dueDate: null,
      phaseId: null,
      storyPoints: null,
    };
    if (value.title !== undefined) {
      patch.title = value.title;
    }
    if (value.description !== undefined) {
      patch.description = value.description;
    }
    if (value.categoryId !== undefined) {
      patch.categoryId = value.categoryId;
    }
    if (value.priority !== undefined) {
      patch.priority = value.priority;
    }
    if (value.ticketType !== undefined) {
      patch.ticketType = value.ticketType;
    }
    this.ticketForm.patchValue(patch);
    this.customFieldDefaults = value.customFieldDefaults ?? [];
  }

  ngOnInit(): void {
    if (this.initialProjectId != null && this.initialProjectId > 0) {
      this.ticketForm.patchValue({ projectId: this.initialProjectId });
      this.selectedProjectId = this.initialProjectId;
      if (this.lockProject) {
        this.ticketForm.get('projectId')?.disable();
      }
      this.projectSelected.emit(this.initialProjectId);
    }

    this.ticketForm.get('projectId')?.valueChanges.subscribe(projectId => {
      if (projectId != null && projectId > 0) {
        this.selectedProjectId = projectId;
        this.projectSelected.emit(projectId);
        this.loadAssignablePhases(projectId);
      } else {
        this.selectedProjectId = null;
        this.assignablePhases = [];
        this.ticketForm.patchValue({ phaseId: null });
      }
    });
  }

  private loadAssignablePhases(projectId: number): void {
    this.phaseService.list(projectId).subscribe(phases => {
      this.assignablePhases = phases.filter(p => p.status === 'PLANNED' || p.status === 'ACTIVE');
      const currentPhaseId = this.ticketForm.value.phaseId;
      if (currentPhaseId != null && !this.assignablePhases.some(p => p.id === currentPhaseId)) {
        this.ticketForm.patchValue({ phaseId: null });
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
    if (this.customFieldsSection && !this.customFieldsSection.isValid()) {
      return;
    }
    const { title, description, categoryId, projectId, priority, ticketType, dueDate, phaseId, storyPoints } = this.ticketForm.getRawValue();
    if (!title || !description || categoryId == null || categoryId < 1 || projectId == null || projectId < 1 || !priority || !ticketType) {
      return;
    }
    this.submitted.emit({
      title,
      description,
      categoryId,
      projectId,
      priority,
      ticketType,
      dueDate: dueDate || undefined,
      phaseId: phaseId ?? undefined,
      storyPoints: storyPoints ?? undefined,
      customFields: this.customFieldsSection?.toValueRequests() ?? [],
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
