import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { CategoriesViewComponent } from './categories-view.component';
import { CategoryService } from '../../services/category.service';
import { ToastService } from '../../services/toast.service';
import { TranslocoService } from '@jsverse/transloco';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('CategoriesViewComponent', () => {
  let component: CategoriesViewComponent;
  let fixture: ComponentFixture<CategoriesViewComponent>;
  let categoryService: jasmine.SpyObj<CategoryService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let toast: jasmine.SpyObj<ToastService>;

  const categories = [
    { id: 1, name: 'Bug', color: '#BF0603' },
    { id: 2, name: 'Feature', color: '#00635D' },
  ];

  beforeEach(async () => {
    categoryService = jasmine.createSpyObj('CategoryService', ['findAll', 'delete']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    toast = jasmine.createSpyObj('ToastService', ['error']);
    categoryService.findAll.and.returnValue(of(categories));

    await TestBed.configureTestingModule({
      imports: [
        CategoriesViewComponent,
        createTranslocoTestingModule(
          { category: { title: 'Categorias', create: 'Nova categoria' } },
          { category: { title: 'Categories', create: 'New category' } },
        ),
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ categories }) },
        },
        { provide: CategoryService, useValue: categoryService },
        { provide: ToastService, useValue: toast },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    fixture = TestBed.createComponent(CategoriesViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.categories.length).toBe(2);
  });

  it('should rerender category actions immediately when locale changes from Portuguese to English', async () => {
    expect(fixture.nativeElement.textContent).toContain('Categorias');
    expect(fixture.nativeElement.textContent).toContain('Nova categoria');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Categories');
    expect(fixture.nativeElement.textContent).toContain('New category');
    expect(fixture.nativeElement.textContent).not.toContain('Nova categoria');
  });

  it('should delete category and refresh list when confirmed', () => {
    dialog.open.and.returnValue({
      afterClosed: () => of(true),
    } as MatDialogRef<unknown>);
    categoryService.delete.and.returnValue(of(void 0));
    categoryService.findAll.and.returnValue(of([categories[1]]));

    component.confirmDelete(categories[0]);

    expect(categoryService.delete).toHaveBeenCalledWith(1);
    expect(component.categories).toEqual([categories[1]]);
  });

  it('should show toast error when delete is rejected', () => {
    dialog.open.and.returnValue({
      afterClosed: () => of(true),
    } as MatDialogRef<unknown>);
    categoryService.delete.and.returnValue(
      throwError(() => ({ error: { message: 'Category cannot be deleted while tickets reference it' } }))
    );

    component.confirmDelete(categories[0]);

    expect(toast.error).toHaveBeenCalledWith('Category cannot be deleted while tickets reference it');
    expect(component.categories.length).toBe(2);
  });

  it('should not delete when confirmation is cancelled', () => {
    dialog.open.and.returnValue({
      afterClosed: () => of(false),
    } as MatDialogRef<unknown>);

    component.confirmDelete(categories[0]);

    expect(categoryService.delete).not.toHaveBeenCalled();
  });
});
