import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { CustomField, CustomFieldService } from '../../services/custom-field.service';
import {
  CustomFieldDialogComponent,
  CustomFieldDialogData,
  CustomFieldOwner,
} from './custom-field-dialog.component';

const CUSTOM_FIELD_TYPE_TRANSLATION_KEYS: Readonly<Record<string, string>> = {
  STRING: 'customField.shortText',
  TEXT: 'customField.longText',
  INTEGER: 'customField.integer',
  BOOLEAN: 'customField.boolean',
  ENUM: 'customField.list',
};

@Component({
  selector: 'app-custom-field-admin',
  imports: [TranslocoPipe, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <section class="form-section custom-field-admin">
      <div class="workflow-form__section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <button class="btn btn-secondary" matButton="outlined" type="button" (click)="openDialog()" [disabled]="!ownerId">
          <mat-icon fontIcon="add" aria-hidden="true"></mat-icon>
          <span>{{ 'customField.add' | transloco }}</span>
        </button>
      </div>

      @if (ownerId == null) {
        <p class="form-hint">
          @if (owner === 'workflow') {
            {{ 'migration.custom-field-admin.saveWorkflowFirst' | transloco }}
          } @else {
            {{ 'migration.custom-field-admin.saveProjectFirst' | transloco }}
          }
        </p>
      } @else if (loading) {
        <p class="form-hint">{{ 'migration.custom-field-admin.333896f6dc37' | transloco }}</p>
      } @else if (fields.length === 0) {
        <p class="form-hint">{{ 'migration.custom-field-admin.e98e89130943' | transloco }}</p>
      } @else {
        <div class="page-panel page-panel--flush">
          <div class="data-table data-table--layout-table data-table--cols-id-name-color-actions">
            <div class="header">
              <div class="header-cell">{{ 'customField.key' | transloco }}</div>
              <div class="header-cell">{{ 'customField.label' | transloco }}</div>
              <div class="header-cell">{{ 'customField.type' | transloco }}</div>
              <div class="header-cell">{{ 'customField.required' | transloco }}</div>
              <div class="header-cell">{{ 'common.actions' | transloco }}</div>
            </div>
            <div class="body">
              @for (field of fields; track field.id) {
                <div class="row {{ $even ? 'even' : 'odd' }}">
                  <div><code>{{ field.key }}</code></div>
                  <div>{{ field.label }}</div>
                  <div>{{ typeLabel(field.type) }}{{ field.enabled ? '' : ' (inativo)' }}</div>
                  <div>
                    {{ (field.required ? 'customField.yes' : 'customField.no') | transloco }}
                    @if (owner === 'workflow' && (field.statusRequired?.length ?? 0) > 0) {
                      <span class="text-muted"> · {{ field.statusRequired.join(', ') }}</span>
                    }
                  </div>
                  <div class="cell-actions">
                    <button class="btn btn-secondary" matButton="outlined" type="button" (click)="openDialog(field)">
                      {{ 'migration.custom-field-admin.96379ced2ee4' | transloco }}
                    </button>
                    @if (field.enabled) {
                      <button class="btn btn-secondary" matButton="outlined" type="button" (click)="disableField(field)">
                        {{ 'serviceAccount.deactivate' | transloco }}
                      </button>
                    } @else {
                      <button class="btn btn-secondary" matButton="outlined" type="button" (click)="enableField(field)">
                        {{ 'migration.custom-field-admin.12488ede1a91' | transloco }}
                      </button>
                    }
                    <button class="btn btn-cancel" matButton="outlined" type="button" (click)="deleteField(field)">
                      {{ 'migration.custom-field-admin.e99d128634ed' | transloco }}
                    </button>
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }
      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }
    </section>
  `,
})
export class CustomFieldAdminComponent implements OnChanges {
  private readonly customFieldService = inject(CustomFieldService);
  private readonly dialog = inject(MatDialog);
  private readonly transloco = inject(TranslocoService);

  @Input({ required: true }) owner!: CustomFieldOwner;
  @Input({ required: true }) ownerId!: number | null;
  @Input() statusNames: string[] = [];
  @Input() sectionTitle = 'Campos personalizados';

  fields: CustomField[] = [];
  loading = false;
  error = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ownerId'] || changes['owner']) {
      this.reload();
    }
  }

  typeLabel(type: string): string {
    const translationKey = CUSTOM_FIELD_TYPE_TRANSLATION_KEYS[type];
    return translationKey ? this.transloco.translate(translationKey) : type;
  }

  openDialog(field?: CustomField): void {
    if (this.ownerId == null) {
      return;
    }
    const data: CustomFieldDialogData = {
      owner: this.owner,
      ownerId: this.ownerId,
      field: field ?? null,
      statusNames: this.statusNames,
    };
    this.dialog
      .open(CustomFieldDialogComponent, {
        data,
        width: '32rem',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(saved => {
        if (saved) {
          this.reload();
        }
      });
  }

  disableField(field: CustomField): void {
    this.setEnabled(field, false);
  }

  enableField(field: CustomField): void {
    this.setEnabled(field, true);
  }

  deleteField(field: CustomField): void {
    if (this.ownerId == null || !confirm(`Excluir o campo "${field.label}"? Só é possível se nenhum ticket tiver valor.`)) {
      return;
    }
    const request$ =
      this.owner === 'project'
        ? this.customFieldService.deleteProjectField(this.ownerId, field.id)
        : this.customFieldService.deleteWorkflowField(this.ownerId, field.id);
    request$.subscribe({
      next: () => this.reload(),
      error: () => {
        this.error = 'Não foi possível excluir o campo (pode haver valores em tickets). Use Desativar.';
      },
    });
  }

  private setEnabled(field: CustomField, enabled: boolean): void {
    if (this.ownerId == null) {
      return;
    }
    const request = {
      key: field.key,
      label: field.label,
      type: field.type,
      required: field.required,
      enabled,
      stringMaxLength: field.stringMaxLength,
      integerMin: field.integerMin,
      integerMax: field.integerMax,
      sortOrder: field.sortOrder,
      enumOptions: (field.enumOptions ?? []).map(option => ({
        value: option.value ?? '',
        label: option.label ?? '',
        sortOrder: option.sortOrder,
      })),
      statusRequired: field.statusRequired ?? [],
    };
    const request$ =
      this.owner === 'project'
        ? this.customFieldService.updateProjectField(this.ownerId, field.id, request)
        : this.customFieldService.updateWorkflowField(this.ownerId, field.id, request);
    request$.subscribe({
      next: () => this.reload(),
      error: () => {
        this.error = 'Não foi possível atualizar o campo.';
      },
    });
  }

  private reload(): void {
    if (this.ownerId == null || this.ownerId < 1) {
      this.fields = [];
      return;
    }
    this.loading = true;
    this.error = '';
    const request$ =
      this.owner === 'project'
        ? this.customFieldService.listProjectFields(this.ownerId)
        : this.customFieldService.listWorkflowFields(this.ownerId);
    request$.subscribe({
      next: fields => {
        this.fields = fields;
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os campos personalizados.';
        this.loading = false;
      },
    });
  }
}
