import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Category, CategoryService, CreateCategoryRequest, UpdateCategoryRequest } from '../../services/category.service';

export interface CategoryDialogData {
  category?: Category;
}

@Component({
  selector: 'app-categories-view',
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  templateUrl: './categories-view.component.html'
})
export class CategoriesViewComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly categoryService = inject(CategoryService);
  private readonly dialog = inject(MatDialog);

  categories: Category[] = [];

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categories }) => this.categories = categories);
  }

  openCreateDialog(): void {
    this.openDialog();
  }

  openEditDialog(category: Category): void {
    this.openDialog(category);
  }

  private openDialog(category?: Category): void {
    const dialogRef = this.dialog.open(CategoryDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      panelClass: 'category-dialog',
      data: { category } satisfies CategoryDialogData
    });
    dialogRef.afterClosed().subscribe(saved => {
      if (saved) {
        this.categoryService.findAll().subscribe(categories => this.categories = categories);
      }
    });
  }
}

@Component({
  selector: 'app-category-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Editar categoria' : 'Nova categoria' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <mat-form-field class="form-field" appearance="outline">
          <mat-label i18n>Nome</mat-label>
          <input matInput formControlName="name" required />
        </mat-form-field>
        <div class="color-picker-field">
          <span class="color-picker-field__label" i18n>Cor</span>
          <div class="color-picker-field__row">
            <label class="color-picker-trigger" i18n-title title="Clique para abrir o seletor de cores">
              <input
                type="color"
                class="color-picker-trigger__input"
                [value]="colorValue"
                (input)="onColorPickerChange($event)"
                aria-label="Abrir seletor de cores" />
              <span class="color-picker-trigger__surface">
                <span class="color-picker-trigger__swatch" [style.background-color]="colorValue"></span>
                <mat-icon fontIcon="palette" aria-hidden="true"></mat-icon>
                <span class="color-picker-trigger__text" i18n>Escolher cor</span>
              </span>
            </label>
            <mat-form-field class="form-field form-field--compact color-picker-field__hex" appearance="outline">
              <mat-label i18n>Código hex</mat-label>
              <input matInput formControlName="color" required (input)="onHexInput($event)" />
            </mat-form-field>
          </div>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button class="btn btn-secondary" matButton="outlined" type="button" mat-dialog-close i18n>Cancelar</button>
        <button class="btn" matButton="filled" type="submit" [disabled]="form.invalid || saving" i18n>Salvar</button>
      </mat-dialog-actions>
    </form>
  `
})
export class CategoryDialogComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly dialogRef = inject(MatDialogRef<CategoryDialogComponent>);
  private readonly formBuilder = inject(FormBuilder);
  private readonly data = inject<CategoryDialogData>(MAT_DIALOG_DATA);

  saving = false;
  isEdit = false;
  colorValue = '#00635D';

  form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    color: ['#00635D', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]]
  });

  ngOnInit(): void {
    if (this.data.category) {
      this.isEdit = true;
      this.form.patchValue({
        name: this.data.category.name,
        color: this.data.category.color
      });
      this.colorValue = this.normalizeHex(this.data.category.color);
    }
  }

  onColorPickerChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value.toUpperCase();
    this.colorValue = value;
    this.form.patchValue({ color: value });
  }

  onHexInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value.trim();
    if (/^#[0-9A-Fa-f]{6}$/i.test(value)) {
      const normalized = value.toUpperCase();
      this.colorValue = normalized;
      this.form.patchValue({ color: normalized }, { emitEvent: false });
    }
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    const payload = this.form.value as { name: string; color: string };
    const request$ = this.isEdit && this.data.category
      ? this.categoryService.update(this.data.category.id, payload as UpdateCategoryRequest)
      : this.categoryService.create(payload as CreateCategoryRequest);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.dialogRef.close(true);
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  private normalizeHex(value: string): string {
    return /^#[0-9A-Fa-f]{6}$/.test(value) ? value.toUpperCase() : '#00635D';
  }
}
