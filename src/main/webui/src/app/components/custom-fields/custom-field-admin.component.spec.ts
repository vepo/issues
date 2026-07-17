import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';
import { CustomFieldService } from '../../services/custom-field.service';
import { CustomFieldAdminComponent } from './custom-field-admin.component';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

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
      imports: [
        createTranslocoTestingModule(
          {
            customField: {
              add: 'Adicionar campo',
              key: 'Chave',
              label: 'Rótulo',
              type: 'Tipo',
              required: 'Obrigatório',
              integer: 'Inteiro',
              yes: 'Sim',
            },
          },
          {
            customField: {
              add: 'Add field',
              key: 'Key',
              label: 'Label',
              type: 'Type',
              required: 'Required',
              integer: 'Integer',
              yes: 'Yes',
            },
          },
        ),
        CustomFieldAdminComponent,
      ],
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

  it('should rerender custom-field copy while preserving administrator-authored labels', async () => {
    customFieldService.listWorkflowFields.and.returnValue(of([{
      id: 1,
      key: 'customer_environment',
      label: 'Ambiente do cliente',
      type: 'INTEGER',
      required: true,
      enabled: true,
      enumOptions: [],
      statusRequired: [],
    } as never]));
    applyInputs(1);
    component.sectionTitle = 'Campos da entrega';
    fixture.detectChanges();

    const transloco = TestBed.inject(TranslocoService);
    expect(fixture.nativeElement.textContent).toContain('Adicionar campo');
    expect(fixture.nativeElement.textContent).toContain('Inteiro');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Add field');
    expect(text).toContain('Key');
    expect(text).toContain('Label');
    expect(text).toContain('Integer');
    expect(text).toContain('Yes');
    expect(text).toContain('Campos da entrega');
    expect(text).toContain('Ambiente do cliente');
    expect(text).not.toContain('Adicionar campo');
  });
});
