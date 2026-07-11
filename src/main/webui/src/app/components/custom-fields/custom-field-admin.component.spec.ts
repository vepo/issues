import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CustomFieldService } from '../../services/custom-field.service';
import { CustomFieldAdminComponent } from './custom-field-admin.component';

describe('CustomFieldAdminComponent', () => {
  let fixture: ComponentFixture<CustomFieldAdminComponent>;
  let component: CustomFieldAdminComponent;
  let customFieldService: jasmine.SpyObj<CustomFieldService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const saveFirstHint = 'Salve o processo antes de adicionar campos personalizados.';

  beforeEach(async () => {
    customFieldService = jasmine.createSpyObj('CustomFieldService', [
      'listWorkflowFields',
      'listProjectFields',
    ]);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    customFieldService.listWorkflowFields.and.returnValue(of([]));
    customFieldService.listProjectFields.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CustomFieldAdminComponent],
      providers: [
        { provide: CustomFieldService, useValue: customFieldService },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    fixture = TestBed.createComponent(CustomFieldAdminComponent);
    component = fixture.componentInstance;
  });

  function addFieldButton(): HTMLButtonElement {
    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>
    );
    const button = buttons.find(b => b.textContent?.includes('Adicionar campo'));
    expect(button).withContext('Adicionar campo button').toBeTruthy();
    return button!;
  }

  function applyInputs(ownerId: number | null): void {
    component.owner = 'workflow';
    component.ownerId = ownerId;
    component.sectionTitle = 'Campos personalizados (processo)';
    component.ngOnChanges({
      owner: {
        currentValue: 'workflow',
        previousValue: undefined,
        firstChange: true,
        isFirstChange: () => true,
      },
      ownerId: {
        currentValue: ownerId,
        previousValue: undefined,
        firstChange: true,
        isFirstChange: () => true,
      },
    });
    fixture.detectChanges();
  }

  it('should show section disabled with save-first hint when workflow has no owner id', () => {
    applyInputs(null);

    expect(fixture.nativeElement.textContent).toContain('Campos personalizados (processo)');
    expect(addFieldButton().disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain(saveFirstHint);
    expect(fixture.nativeElement.querySelector('.form-hint')?.textContent).toContain(saveFirstHint);
  });

  it('should enable add field and hide save-first hint when workflow owner id is set', () => {
    applyInputs(1);

    expect(customFieldService.listWorkflowFields).toHaveBeenCalledWith(1);
    expect(addFieldButton().disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain(saveFirstHint);
    expect(fixture.nativeElement.textContent).toContain('Nenhum campo personalizado definido');
  });
});
