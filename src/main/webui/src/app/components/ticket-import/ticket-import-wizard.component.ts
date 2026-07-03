import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ColumnMapping } from '../../generated/model/columnMapping';
import { ImportRowValidation } from '../../generated/model/importRowValidation';
import { Project, ProjectWorkflow, ProjectsService } from '../../services/projects.service';
import { User, UsersService, emptyFilter } from '../../services/users.service';
import { TicketImportPreviewRow, TicketImportResult, TicketImportService, TicketImportUpload } from '../../services/ticket-import.service';

const SKIP_COLUMN = '';

interface MappingField {
  key: keyof ColumnMapping;
  label: string;
  required: boolean;
}

@Component({
  selector: 'app-ticket-import-wizard',
  imports: [
    ReactiveFormsModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './ticket-import-wizard.component.html',
})
export class TicketImportWizardComponent implements OnInit {
  @ViewChild(MatStepper) private stepper?: MatStepper;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketImportService = inject(TicketImportService);
  private readonly projectsService = inject(ProjectsService);
  private readonly usersService = inject(UsersService);

  readonly projectMappingField: MappingField = { key: 'projectColumn', label: 'Projeto', required: true };

  readonly baseMappingFields: MappingField[] = [
    { key: 'titleColumn', label: 'Título', required: true },
    { key: 'descriptionColumn', label: 'Descrição', required: true },
    { key: 'categoryColumn', label: 'Categoria', required: true },
    { key: 'priorityColumn', label: 'Prioridade', required: false },
    { key: 'assigneeEmailColumn', label: 'Responsável (e-mail)', required: false },
    { key: 'statusColumn', label: 'Status', required: false },
  ];

  project: Project | null = null;
  projectId: number | null = null;
  projectScoped = true;
  projects: Project[] = [];
  users: User[] = [];
  workflowsByProjectId = new Map<number, ProjectWorkflow>();
  fixingRowId: number | null = null;
  fileName = '';
  uploadResult: TicketImportUpload | null = null;
  importId = 0;
  headers: string[] = [];
  sampleRows: Record<string, string>[] = [];
  uploadError = '';
  isUploading = false;
  previewRows: ImportRowValidation[] = [];
  validCount = 0;
  invalidCount = 0;
  importResult: TicketImportResult | null = null;
  isPreviewLoading = false;
  isImporting = false;
  previewError = '';

  mappingForm = new FormGroup({
    titleColumn: new FormControl(SKIP_COLUMN, Validators.required),
    descriptionColumn: new FormControl(SKIP_COLUMN, Validators.required),
    categoryColumn: new FormControl(SKIP_COLUMN, Validators.required),
    projectColumn: new FormControl(SKIP_COLUMN),
    priorityColumn: new FormControl(SKIP_COLUMN),
    assigneeEmailColumn: new FormControl(SKIP_COLUMN),
    statusColumn: new FormControl(SKIP_COLUMN),
  });

  ngOnInit(): void {
    this.route.data.subscribe(({ project, projects }) => {
      this.project = project ?? null;
      this.projectScoped = !!project;
      this.projectId = project?.id ?? null;
      this.projects = projects ?? [];
      this.updateProjectColumnValidators();
      if (this.projectId != null) {
        this.ensureWorkflow(this.projectId);
      }
    });
  }

  mappingFields(): MappingField[] {
    if (this.projectScoped) {
      return this.baseMappingFields;
    }
    return [this.projectMappingField, ...this.baseMappingFields];
  }

  requiredMappingFields(): MappingField[] {
    return this.mappingFields().filter(field => field.required);
  }

  optionalMappingFields(): MappingField[] {
    return this.mappingFields().filter(field => !field.required);
  }

  sampleValueShort(column: string | null | undefined, maxLength = 48): string {
    const value = this.sampleValue(column);
    if (value === '—' || value.length <= maxLength) {
      return value;
    }
    return `${value.slice(0, maxLength - 1)}…`;
  }

  hasProjectError(row: ImportRowValidation): boolean {
    return !this.projectScoped && (row.errors ?? []).some(
      error => error.startsWith('Unknown project:') || error === 'Project is required',
    );
  }

  hasStatusError(row: ImportRowValidation): boolean {
    return (row.errors ?? []).some(
      error =>
        error.startsWith('Status not in project workflow:') || error.startsWith('No direct transition from start to'),
    );
  }

  hasAssigneeError(row: ImportRowValidation): boolean {
    return (row.errors ?? []).some(error => error.startsWith('Unknown assignee email:'));
  }

  statusOptionsForRow(row: ImportRowValidation): string[] {
    const projectId = this.resolveProjectIdForRow(row);
    if (projectId == null) {
      return [];
    }
    const workflow = this.workflowsByProjectId.get(projectId);
    if (!workflow?.start) {
      return [];
    }
    const reachable = (workflow.transitions ?? [])
      .filter(transition => transition.from === workflow.start)
      .map(transition => transition.to)
      .filter((status): status is string => !!status);
    return [...new Set([workflow.start, ...reachable])];
  }

  isFixingRow(row: ImportRowValidation): boolean {
    return this.fixingRowId === row.rowId;
  }

  correctProject(row: ImportRowValidation, projectName: string): void {
    if (!row.rowId || !projectName) {
      return;
    }
    this.fixingRowId = row.rowId;
    this.ticketImportService.correctRow(this.projectId, this.importId, row.rowId, { projectName }).subscribe({
      next: updated => {
        const project = this.projects.find(item => item.name === projectName);
        if (project?.id != null) {
          this.ensureWorkflow(project.id);
        }
        this.replacePreviewRow(updated);
        this.fixingRowId = null;
      },
      error: () => {
        this.previewError = 'Falha ao corrigir projeto';
        this.fixingRowId = null;
      },
    });
  }

  correctStatus(row: ImportRowValidation, statusName: string): void {
    if (!row.rowId) {
      return;
    }
    this.fixingRowId = row.rowId;
    this.ticketImportService.correctRow(this.projectId, this.importId, row.rowId, { statusName }).subscribe({
      next: updated => {
        this.replacePreviewRow(updated);
        this.fixingRowId = null;
      },
      error: () => {
        this.previewError = 'Falha ao corrigir status';
        this.fixingRowId = null;
      },
    });
  }

  correctAssignee(row: ImportRowValidation, assigneeEmail: string): void {
    if (!row.rowId) {
      return;
    }
    this.fixingRowId = row.rowId;
    this.ticketImportService.correctRow(this.projectId, this.importId, row.rowId, { assigneeEmail }).subscribe({
      next: updated => {
        this.replacePreviewRow(updated);
        this.fixingRowId = null;
      },
      error: () => {
        this.previewError = 'Falha ao corrigir responsável';
        this.fixingRowId = null;
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.uploadError = 'Selecione um arquivo .csv';
      this.uploadResult = null;
      return;
    }

    this.fileName = file.name;
    this.uploadError = '';
    this.isUploading = true;
    this.ticketImportService.upload(this.projectId, file).subscribe({
      next: result => {
        this.uploadResult = result;
        this.projectScoped = result.projectScoped ?? this.projectScoped;
        this.importId = result.id ?? 0;
        this.headers = result.headers ?? [];
        this.sampleRows = (result.sampleRows ?? []) as Record<string, string>[];
        this.updateProjectColumnValidators();
        this.guessColumnMapping(this.headers);
        this.isUploading = false;
      },
      error: () => {
        this.uploadError = 'Não foi possível enviar o arquivo CSV';
        this.uploadResult = null;
        this.isUploading = false;
      },
    });
  }

  headerOptions(): string[] {
    return this.headers;
  }

  sampleValue(column: string | null | undefined): string {
    if (!column || !this.sampleRows.length) {
      return '—';
    }
    return this.sampleRows[0][column] || '—';
  }

  canProceedFromUpload(): boolean {
    return !!this.uploadResult && (this.uploadResult.rowCount ?? 0) > 0 && !this.uploadError && !this.isUploading;
  }

  buildColumnMapping(): ColumnMapping {
    const value = this.mappingForm.getRawValue();
    return {
      titleColumn: value.titleColumn || undefined,
      descriptionColumn: value.descriptionColumn || undefined,
      categoryColumn: value.categoryColumn || undefined,
      projectColumn: this.projectScoped ? undefined : value.projectColumn || undefined,
      priorityColumn: emptyToUndefined(value.priorityColumn),
      assigneeEmailColumn: emptyToUndefined(value.assigneeEmailColumn),
      statusColumn: emptyToUndefined(value.statusColumn),
    };
  }

  saveMappingAndPreview(): void {
    if (!this.importId) {
      return;
    }
    this.isPreviewLoading = true;
    this.previewError = '';
    this.ticketImportService.applyMapping(this.projectId, this.importId, { mapping: this.buildColumnMapping() }).subscribe({
      next: () => this.loadPreview(),
      error: () => {
        this.previewError = 'Falha ao salvar mapeamento de colunas';
        this.isPreviewLoading = false;
      },
    });
  }

  loadPreview(): void {
    this.isPreviewLoading = true;
    this.usersService.search(emptyFilter()).subscribe({
      next: users => {
        this.users = users;
        this.ticketImportService.preview(this.projectId, this.importId).subscribe({
          next: response => {
            this.previewRows = response.rows ?? [];
            this.recountPreview();
            this.preloadWorkflowsForPreview();
            this.isPreviewLoading = false;
          },
          error: () => {
            this.previewError = 'Falha ao validar importação';
            this.isPreviewLoading = false;
          },
        });
      },
      error: () => {
        this.previewError = 'Falha ao carregar usuários';
        this.isPreviewLoading = false;
      },
    });
  }

  importValidRows(): void {
    if (!this.importId || this.validCount === 0) {
      return;
    }

    this.isImporting = true;
    this.ticketImportService.execute(this.projectId, this.importId).subscribe({
      next: result => {
        this.importResult = result;
        this.isImporting = false;
        this.stepper?.next();
      },
      error: () => {
        this.previewError = 'Falha ao importar tickets';
        this.isImporting = false;
      },
    });
  }

  resetWizard(): void {
    this.fileName = '';
    this.uploadResult = null;
    this.importId = 0;
    this.headers = [];
    this.sampleRows = [];
    this.uploadError = '';
    this.previewRows = [];
    this.validCount = 0;
    this.invalidCount = 0;
    this.importResult = null;
    this.previewError = '';
    this.mappingForm.reset({
      titleColumn: SKIP_COLUMN,
      descriptionColumn: SKIP_COLUMN,
      categoryColumn: SKIP_COLUMN,
      projectColumn: SKIP_COLUMN,
      priorityColumn: SKIP_COLUMN,
      assigneeEmailColumn: SKIP_COLUMN,
      statusColumn: SKIP_COLUMN,
    });
    this.updateProjectColumnValidators();
    this.stepper?.reset();
  }

  cancel(): void {
    if (this.projectScoped && this.projectId != null) {
      this.router.navigate(['/project', this.projectId, 'kanban']);
      return;
    }
    this.router.navigate(['/']);
  }

  resultLink(): (string | number)[] {
    if (this.projectScoped && this.projectId != null) {
      return ['/project', this.projectId, 'kanban'];
    }
    return ['/'];
  }

  private replacePreviewRow(updated: TicketImportPreviewRow): void {
    const index = this.previewRows.findIndex(row => row.rowId === updated.rowId);
    if (index < 0) {
      return;
    }
    this.previewRows = [
      ...this.previewRows.slice(0, index),
      updated,
      ...this.previewRows.slice(index + 1),
    ];
    this.recountPreview();
    this.preloadWorkflowsForPreview();
  }

  private recountPreview(): void {
    this.validCount = this.previewRows.filter(row => row.valid).length;
    this.invalidCount = this.previewRows.length - this.validCount;
  }

  private preloadWorkflowsForPreview(): void {
    if (this.projectScoped && this.projectId != null) {
      this.ensureWorkflow(this.projectId);
    }
    for (const row of this.previewRows) {
      const projectId = this.resolveProjectIdForRow(row);
      if (projectId != null) {
        this.ensureWorkflow(projectId);
      }
    }
  }

  private resolveProjectIdForRow(row: ImportRowValidation): number | null {
    if (this.projectScoped) {
      return this.projectId;
    }
    const projectName = row.preview?.projectName;
    if (!projectName) {
      return null;
    }
    return this.projects.find(project => project.name?.toLowerCase() === projectName.toLowerCase())?.id ?? null;
  }

  private ensureWorkflow(projectId: number): void {
    if (this.workflowsByProjectId.has(projectId)) {
      return;
    }
    this.projectsService.findWorkflowByProjectId(projectId).subscribe(workflow => {
      this.workflowsByProjectId.set(projectId, workflow);
    });
  }

  private updateProjectColumnValidators(): void {
    const control = this.mappingForm.controls.projectColumn;
    if (this.projectScoped) {
      control.clearValidators();
      control.setValue(SKIP_COLUMN);
    } else {
      control.setValidators(Validators.required);
    }
    control.updateValueAndValidity();
  }

  private guessColumnMapping(headers: string[]): void {
    const guess = (candidates: string[]) =>
      headers.find(header => candidates.some(candidate => header.toLowerCase().includes(candidate))) ?? SKIP_COLUMN;

    this.mappingForm.patchValue({
      projectColumn: guess(['project', 'projeto']),
      titleColumn: guess(['title', 'titulo', 'título', 'assunto']),
      descriptionColumn: guess(['description', 'descricao', 'descrição', 'detalhe']),
      categoryColumn: guess(['category', 'categoria', 'tipo']),
      priorityColumn: guess(['priority', 'prioridade']),
      assigneeEmailColumn: guess(['assignee', 'responsavel', 'responsável', 'email']),
      statusColumn: guess(['status', 'estado', 'situação', 'situacao']),
    });
  }
}

function emptyToUndefined(value: string | null | undefined): string | undefined {
  return value && value !== SKIP_COLUMN ? value : undefined;
}
