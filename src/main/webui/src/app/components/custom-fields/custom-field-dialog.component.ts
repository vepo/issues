import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogModule,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CustomField, CustomFieldRequest, CustomFieldService, CustomFieldType } from '../../services/custom-field.service';

export type CustomFieldOwner = 'project' | 'workflow';

export interface CustomFieldDialogData {
  owner: CustomFieldOwner;
  ownerId: number;
  field?: CustomField | null;
  statusNames?: string[];
}

@Component({
  selector: 'app-custom-field-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Editar campo personalizado' : 'Novo campo personalizado' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="custom-field-dialog">
        <mat-form-field class="form-field" appearance="outline">
          <mat-label i18n>Chave</mat-label>
          <input matInput formControlName="key" required maxlength="32" />
          <mat-hint i18n>Identificador estável (não muda após criar)</mat-hint>
        </mat-form-field>

        <mat-form-field class="form-field" appearance="outline">
          <mat-label i18n>Rótulo</mat-label>
          <input matInput formControlName="label" required maxlength="128" />
        </mat-form-field>

        <mat-form-field class="form-field" appearance="outline">
          <mat-label i18n>Tipo</mat-label>
          <mat-select formControlName="type" required>
            @for (type of fieldTypes; track type.value) {
              <mat-option [value]="type.value">{{ type.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-checkbox formControlName="required" i18n>Obrigatório</mat-checkbox>
        <mat-checkbox formControlName="enabled" i18n>Ativo</mat-checkbox>

        @if (form.value.type === 'STRING') {
          <mat-form-field class="form-field" appearance="outline">
            <mat-label i18n>Tamanho máximo</mat-label>
            <input matInput type="number" formControlName="stringMaxLength" min="1" max="255" />
          </mat-form-field>
        }

        @if (form.value.type === 'INTEGER') {
          <mat-form-field class="form-field" appearance="outline">
            <mat-label i18n>Mínimo</mat-label>
            <input matInput type="number" formControlName="integerMin" />
          </mat-form-field>
          <mat-form-field class="form-field" appearance="outline">
            <mat-label i18n>Máximo</mat-label>
            <input matInput type="number" formControlName="integerMax" />
          </mat-form-field>
        }

        @if (form.value.type === 'ENUM') {
          <section class="form-section" formArrayName="enumOptions">
            <div class="workflow-form__section-header">
              <h3 class="section-title" i18n>Opções</h3>
              <button class="btn btn-secondary" matButton="outlined" type="button" (click)="addEnumOption()">
                <mat-icon fontIcon="add" aria-hidden="true"></mat-icon>
                <span i18n>Adicionar opção</span>
              </button>
            </div>
            @for (option of enumOptions.controls; track $index; let i = $index) {
              <div class="custom-field-dialog__option" [formGroupName]="i">
                <mat-form-field class="form-field form-field--compact" appearance="outline">
                  <mat-label i18n>Valor</mat-label>
                  <input matInput formControlName="value" required />
                </mat-form-field>
                <mat-form-field class="form-field form-field--compact" appearance="outline">
                  <mat-label i18n>Rótulo</mat-label>
                  <input matInput formControlName="label" required />
                </mat-form-field>
                <button
                  class="btn btn-secondary"
                  matButton="outlined"
                  type="button"
                  (click)="removeEnumOption(i)"
                  [disabled]="enumOptions.length <= 1"
                  i18n-aria-label
                  aria-label="Remover opção">
                  <mat-icon fontIcon="delete" aria-hidden="true"></mat-icon>
                </button>
              </div>
            }
          </section>
        }

        @if (data.owner === 'workflow' && (data.statusNames?.length ?? 0) > 0) {
          <mat-form-field class="form-field" appearance="outline">
            <mat-label i18n>Obrigatório nos status</mat-label>
            <mat-select formControlName="statusRequired" multiple>
              @for (status of data.statusNames; track status) {
                <mat-option [value]="status">{{ status }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button class="btn btn-secondary" matButton="outlined" type="button" mat-dialog-close i18n>Cancelar</button>
        <button class="btn" matButton="filled" type="submit" [disabled]="form.invalid || saving" i18n>Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .custom-field-dialog {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      min-width: min(28rem, 90vw);
    }
    .custom-field-dialog__option {
      display: flex;
      gap: 0.5rem;
      align-items: flex-start;
    }
  `,
})
export class CustomFieldDialogComponent implements OnInit {
  private readonly customFieldService = inject(CustomFieldService);
  private readonly dialogRef = inject(MatDialogRef<CustomFieldDialogComponent>);
  private readonly formBuilder = inject(FormBuilder);
  readonly data = inject<CustomFieldDialogData>(MAT_DIALOG_DATA);

  saving = false;
  isEdit = false;

  readonly fieldTypes: { value: CustomFieldType; label: string }[] = [
    { value: 'STRING', label: 'Texto curto' },
    { value: 'TEXT', label: 'Texto longo' },
    { value: 'INTEGER', label: 'Número inteiro' },
    { value: 'BOOLEAN', label: 'Sim/Não' },
    { value: 'ENUM', label: 'Lista (única escolha)' },
  ];

  form: FormGroup = this.formBuilder.group({
    key: ['', [Validators.required, Validators.maxLength(32), Validators.pattern(/^[a-z][a-z0-9_]*$/)]],
    label: ['', [Validators.required, Validators.maxLength(128)]],
    type: ['STRING' as CustomFieldType, Validators.required],
    required: [false],
    enabled: [true],
    stringMaxLength: [255],
    integerMin: [null as number | null],
    integerMax: [null as number | null],
    statusRequired: [[] as string[]],
    enumOptions: this.formBuilder.array([this.createEnumOptionGroup()]),
  });

  get enumOptions(): FormArray {
    return this.form.get('enumOptions') as FormArray;
  }

  ngOnInit(): void {
    if (this.data.field) {
      this.isEdit = true;
      const field = this.data.field;
      this.form.patchValue({
        key: field.key,
        label: field.label,
        type: field.type,
        required: field.required,
        enabled: field.enabled,
        stringMaxLength: field.stringMaxLength ?? 255,
        integerMin: field.integerMin ?? null,
        integerMax: field.integerMax ?? null,
        statusRequired: field.statusRequired ?? [],
      });
      this.form.controls['key'].disable({ emitEvent: false });
      this.form.controls['type'].disable({ emitEvent: false });
      this.enumOptions.clear();
      const options = field.enumOptions ?? [];
      if (options.length === 0) {
        this.enumOptions.push(this.createEnumOptionGroup());
      } else {
        options.forEach(option => {
          this.enumOptions.push(
            this.createEnumOptionGroup(option.value ?? '', option.label ?? '', option.sortOrder ?? undefined),
          );
        });
      }
    }

    this.form.controls['type'].valueChanges.subscribe(type => {
      if (type === 'ENUM' && this.enumOptions.length === 0) {
        this.addEnumOption();
      }
    });
  }

  createEnumOptionGroup(value = '', label = '', sortOrder?: number): FormGroup {
    return this.formBuilder.group({
      value: [value, [Validators.required, Validators.maxLength(128)]],
      label: [label, [Validators.required, Validators.maxLength(128)]],
      sortOrder: [sortOrder ?? null],
    });
  }

  addEnumOption(): void {
    this.enumOptions.push(this.createEnumOptionGroup());
  }

  removeEnumOption(index: number): void {
    if (this.enumOptions.length > 1) {
      this.enumOptions.removeAt(index);
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const raw = this.form.getRawValue();
    const request: CustomFieldRequest = {
      key: raw.key,
      label: raw.label,
      type: raw.type,
      required: !!raw.required,
      enabled: raw.enabled !== false,
      stringMaxLength: raw.type === 'STRING' ? Number(raw.stringMaxLength) || 255 : undefined,
      integerMin: raw.type === 'INTEGER' && raw.integerMin != null && raw.integerMin !== '' ? Number(raw.integerMin) : undefined,
      integerMax: raw.type === 'INTEGER' && raw.integerMax != null && raw.integerMax !== '' ? Number(raw.integerMax) : undefined,
      enumOptions:
        raw.type === 'ENUM'
          ? (raw.enumOptions as { value: string; label: string; sortOrder?: number | null }[]).map((option, index) => ({
              value: option.value,
              label: option.label,
              sortOrder: option.sortOrder ?? index,
            }))
          : undefined,
      statusRequired: this.data.owner === 'workflow' ? raw.statusRequired ?? [] : undefined,
    };

    const request$ =
      this.data.owner === 'project'
        ? this.isEdit && this.data.field
          ? this.customFieldService.updateProjectField(this.data.ownerId, this.data.field.id, request)
          : this.customFieldService.createProjectField(this.data.ownerId, request)
        : this.isEdit && this.data.field
          ? this.customFieldService.updateWorkflowField(this.data.ownerId, this.data.field.id, request)
          : this.customFieldService.createWorkflowField(this.data.ownerId, request);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.dialogRef.close(true);
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
