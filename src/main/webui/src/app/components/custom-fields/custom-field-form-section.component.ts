import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CustomFieldValueRequest } from '../../generated/model/customFieldValueRequest';
import { CustomFieldValueResponse } from '../../generated/model/customFieldValueResponse';
import { CustomField, CustomFieldService } from '../../services/custom-field.service';
import { RichTextEditorComponent } from '../rich-text-editor/rich-text-editor.component';
import { plainTextLengthValidator } from '../../core/plain-text-length';

export interface CustomFieldValueView {
  key: string;
  label: string;
  type: string;
  value: unknown;
  orphan: boolean;
  readOnly: boolean;
}

@Component({
  selector: 'app-custom-field-form-section',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, RichTextEditorComponent],
  template: `
    @if (fields.length > 0 || orphanValues.length > 0) {
      <section class="form-section custom-field-form-section">
        <h2 class="section-title" i18n>Campos personalizados</h2>

        @if (fields.length > 0) {
          <div [formGroup]="valuesForm" class="custom-field-form-section__fields">
            @for (field of fields; track field.key) {
              @switch (field.type) {
                @case ('BOOLEAN') {
                  <mat-checkbox [formControlName]="field.key">{{ field.label }}{{ field.required ? ' *' : '' }}</mat-checkbox>
                }
                @case ('TEXT') {
                  <div class="form-field form-field--rich-text">
                    <label class="form-label">{{ field.label }}{{ field.required ? ' *' : '' }}</label>
                    <app-rich-text-editor [formControlName]="field.key" [placeholder]="field.label"></app-rich-text-editor>
                  </div>
                }
                @case ('INTEGER') {
                  <mat-form-field class="form-field" appearance="outline">
                    <mat-label>{{ field.label }}</mat-label>
                    <input matInput type="number" [formControlName]="field.key" [required]="field.required" />
                  </mat-form-field>
                }
                @case ('ENUM') {
                  <mat-form-field class="form-field" appearance="outline">
                    <mat-label>{{ field.label }}</mat-label>
                    <mat-select [formControlName]="field.key" [required]="field.required">
                      @if (!field.required) {
                        <mat-option [value]="null" i18n>Nenhum</mat-option>
                      }
                      @for (option of field.enumOptions; track option.value) {
                        <mat-option [value]="option.value">{{ option.label }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                }
                @default {
                  <mat-form-field class="form-field" appearance="outline">
                    <mat-label>{{ field.label }}</mat-label>
                    <input
                      matInput
                      [formControlName]="field.key"
                      [required]="field.required"
                      [attr.maxlength]="field.stringMaxLength ?? 255" />
                  </mat-form-field>
                }
              }
            }
          </div>
        }

        @if (orphanValues.length > 0) {
          <div class="custom-field-form-section__orphans">
            <p class="form-hint" i18n>Valores de campos fora do escopo atual (somente leitura):</p>
            <dl class="detail-list">
              @for (orphan of orphanValues; track orphan.key) {
                <dt><code>{{ orphan.key }}</code></dt>
                @if (orphan.type === 'TEXT') {
                  <dd class="rich-text-display" [innerHTML]="displayValue(orphan.value)"></dd>
                } @else {
                  <dd>{{ displayValue(orphan.value) }}</dd>
                }
              }
            </dl>
          </div>
        }
      </section>
    }
  `,
})
export class CustomFieldFormSectionComponent implements OnChanges {
  private readonly customFieldService = inject(CustomFieldService);

  @Input() projectId: number | null = null;
  @Input() definitions: CustomField[] | null = null;
  @Input() initialValues: CustomFieldValueResponse[] | null = null;
  @Input() templateDefaults: CustomFieldValueResponse[] | null = null;
  @Input() disabled = false;

  fields: CustomField[] = [];
  orphanValues: CustomFieldValueView[] = [];
  valuesForm = new FormGroup({});
  private loadedProjectId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['definitions'] && this.definitions) {
      this.applyDefinitions(this.definitions);
      return;
    }
    if (changes['projectId'] && this.projectId != null && this.projectId > 0 && this.projectId !== this.loadedProjectId) {
      this.loadInScope(this.projectId);
    }
    if (changes['initialValues'] || changes['templateDefaults'] || changes['disabled']) {
      this.patchValues();
    }
  }

  isValid(): boolean {
    if (this.fields.length === 0) {
      return true;
    }
    if (this.valuesForm.invalid) {
      this.valuesForm.markAllAsTouched();
      return false;
    }
    return true;
  }

  toValueRequests(): CustomFieldValueRequest[] {
    const raw = this.valuesForm.getRawValue() as Record<string, unknown>;
    return this.fields.map(field => ({
      key: field.key,
      value: normalizeSubmitValue(field.type, raw[field.key]),
    }));
  }

  private loadInScope(projectId: number): void {
    this.customFieldService.listInScope(projectId).subscribe({
      next: fields => {
        this.loadedProjectId = projectId;
        this.applyDefinitions(fields);
      },
      error: () => {
        this.fields = [];
        this.rebuildForm();
      },
    });
  }

  private applyDefinitions(fields: CustomField[]): void {
    this.fields = fields.filter(field => field.enabled !== false);
    this.rebuildForm();
    this.patchValues();
  }

  private rebuildForm(): void {
    const group: Record<string, FormControl> = {};
    for (const field of this.fields) {
      const validators = [];
      if (field.required && field.type !== 'BOOLEAN') {
        validators.push(Validators.required);
      }
      if (field.type === 'STRING') {
        validators.push(Validators.maxLength(field.stringMaxLength ?? 255));
      }
      if (field.type === 'TEXT') {
        validators.push(plainTextLengthValidator(field.required ? 1 : 0, 1200));
      }
      group[field.key] = new FormControl(
        { value: defaultControlValue(field.type), disabled: this.disabled },
        validators,
      );
    }
    this.valuesForm = new FormGroup(group);
  }

  private patchValues(): void {
    const byKey = new Map<string, unknown>();
    for (const field of this.fields) {
      byKey.set(field.key, defaultControlValue(field.type));
    }
    this.overlayValues(byKey, this.templateDefaults);
    this.overlayValues(byKey, this.initialValues);

    const patch: Record<string, unknown> = {};
    for (const field of this.fields) {
      if (byKey.has(field.key)) {
        patch[field.key] = coerceControlValue(field.type, byKey.get(field.key));
      }
    }
    this.valuesForm.patchValue(patch);

    this.orphanValues = (this.initialValues ?? [])
      .filter(value => value.orphan || value.readOnly)
      .filter(value => !this.fields.some(field => field.key === value.key))
      .map(value => ({
        key: value.key ?? '',
        label: value.key ?? '',
        type: value.type ?? 'STRING',
        value: value.value,
        orphan: !!value.orphan,
        readOnly: !!value.readOnly,
      }));

    if (this.disabled) {
      this.valuesForm.disable({ emitEvent: false });
    } else {
      this.valuesForm.enable({ emitEvent: false });
    }
  }

  private overlayValues(
    valuesByKey: Map<string, unknown>,
    values: CustomFieldValueResponse[] | null,
  ): void {
    for (const value of values ?? []) {
      if (value.key) {
        valuesByKey.set(value.key, value.value);
      }
    }
  }

  displayValue(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    if (typeof value === 'boolean') {
      return value ? 'Sim' : 'Não';
    }
    return String(value);
  }
}

function defaultControlValue(type: string): unknown {
  if (type === 'BOOLEAN') {
    return false;
  }
  return null;
}

function coerceControlValue(type: string, value: unknown): unknown {
  if (value === null || value === undefined || value === '') {
    return defaultControlValue(type);
  }
  if (type === 'INTEGER') {
    return typeof value === 'number' ? value : Number(value);
  }
  if (type === 'BOOLEAN') {
    return value === true || value === 'true';
  }
  return value;
}

function normalizeSubmitValue(type: string, value: unknown): unknown {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  if (type === 'INTEGER') {
    const numberValue = typeof value === 'number' ? value : Number(value);
    return Number.isNaN(numberValue) ? null : numberValue;
  }
  if (type === 'BOOLEAN') {
    return value === true;
  }
  return value;
}
