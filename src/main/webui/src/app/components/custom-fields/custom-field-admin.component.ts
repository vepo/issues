import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { CustomField, CustomFieldService } from '../../services/custom-field.service';
import {
  CustomFieldDialogComponent,
  CustomFieldDialogData,
  CustomFieldOwner,
} from './custom-field-dialog.component';

@Component({
  selector: 'app-custom-field-admin',
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <section class="form-section custom-field-admin">
      <div class="workflow-form__section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <button class="btn btn-secondary" matButton="outlined" type="button" (click)="openDialog()" [disabled]="!ownerId">
          <mat-icon fontIcon="add" aria-hidden="true"></mat-icon>
          <span i18n>Adicionar campo</span>
        </button>
      </div>

      @if (ownerId == null) {
        <p class="form-hint" i18n>
          @if (owner === 'workflow') {
            Salve o processo antes de adicionar campos personalizados.
          } @else {
            Salve o projeto antes de adicionar campos personalizados.
          }
        </p>
      } @else if (loading) {
        <p class="form-hint" i18n>Carregando campos…</p>
      } @else if (fields.length === 0) {
        <p class="form-hint" i18n>Nenhum campo personalizado definido.</p>
      } @else {
        <div class="page-panel page-panel--flush">
          <div class="data-table data-table--layout-table data-table--cols-id-name-color-actions">
            <div class="header">
              <div class="header-cell" i18n>Chave</div>
              <div class="header-cell" i18n>Rótulo</div>
              <div class="header-cell" i18n>Tipo</div>
              <div class="header-cell" i18n>Obrigatório</div>
              <div class="header-cell" i18n>Ações</div>
            </div>
            <div class="body">
              @for (field of fields; track field.id) {
                <div class="row {{ $even ? 'even' : 'odd' }}">
                  <div><code>{{ field.key }}</code></div>
                  <div>{{ field.label }}</div>
                  <div>{{ typeLabel(field.type) }}{{ field.enabled ? '' : ' (inativo)' }}</div>
                  <div>
                    {{ field.required ? 'Sim' : 'Não' }}
                    @if (owner === 'workflow' && (field.statusRequired?.length ?? 0) > 0) {
                      <span class="text-muted"> · {{ field.statusRequired.join(', ') }}</span>
                    }
                  </div>
                  <div class="cell-actions">
                    <button class="btn btn-secondary" matButton="outlined" type="button" (click)="openDialog(field)" i18n>
                      Editar
                    </button>
                    @if (field.enabled) {
                      <button class="btn btn-secondary" matButton="outlined" type="button" (click)="disableField(field)" i18n>
                        Desativar
                      </button>
                    } @else {
                      <button class="btn btn-secondary" matButton="outlined" type="button" (click)="enableField(field)" i18n>
                        Ativar
                      </button>
                    }
                    <button class="btn btn-cancel" matButton="outlined" type="button" (click)="deleteField(field)" i18n>
                      Excluir
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
    switch (type) {
      case 'STRING':
        return 'Texto curto';
      case 'TEXT':
        return 'Texto longo';
      case 'INTEGER':
        return 'Inteiro';
      case 'BOOLEAN':
        return 'Sim/Não';
      case 'ENUM':
        return 'Lista';
      default:
        return type;
    }
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
