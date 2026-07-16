import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CustomFieldService } from '../../services/custom-field.service';
import { CustomFieldFormSectionComponent } from './custom-field-form-section.component';

describe('CustomFieldFormSectionComponent', () => {
  let fixture: ComponentFixture<CustomFieldFormSectionComponent>;
  let component: CustomFieldFormSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomFieldFormSectionComponent],
      providers: [
        {
          provide: CustomFieldService,
          useValue: {
            listInScope: jasmine.createSpy('listInScope').and.returnValue(of([
              {
                id: 1,
                key: 'sprint',
                label: 'Sprint',
                type: 'INTEGER',
                required: true,
                enabled: true,
                enumOptions: [],
                statusRequired: [],
              },
              {
                id: 2,
                key: 'environment',
                label: 'Ambiente',
                type: 'ENUM',
                required: false,
                enabled: true,
                enumOptions: [{ value: 'dev', label: 'Dev' }, { value: 'homolog', label: 'Homolog' }],
                statusRequired: [],
              },
            ])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomFieldFormSectionComponent);
    component = fixture.componentInstance;
  });

  it('should render in-scope fields for a project', () => {
    component.projectId = 1;
    component.ngOnChanges({
      projectId: {
        currentValue: 1,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });
    fixture.detectChanges();

    expect(component.fields.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Sprint');
    expect(fixture.nativeElement.textContent).toContain('Ambiente');
  });

  it('should emit typed values on submit payload', () => {
    component.definitions = [
      {
        id: 1,
        key: 'sprint',
        label: 'Sprint',
        type: 'INTEGER',
        required: false,
        enabled: true,
        projectId: 1,
        workflowId: null as never,
        stringMaxLength: null as never,
        integerMin: 1,
        integerMax: 99,
        sortOrder: 0,
        enumOptions: [],
        statusRequired: [],
      },
    ];
    component.ngOnChanges({
      definitions: {
        currentValue: component.definitions,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });
    component.valuesForm.patchValue({ sprint: 3 });

    expect(component.toValueRequests()).toEqual([{ key: 'sprint', value: 3 }]);
  });

  it('should reset stale target values after applying template then clone overrides', () => {
    component.definitions = [
      {
        id: 1,
        key: 'sprint',
        label: 'Sprint',
        type: 'INTEGER',
        required: false,
        enabled: true,
        enumOptions: [],
        statusRequired: [],
      },
      {
        id: 2,
        key: 'environment',
        label: 'Ambiente',
        type: 'ENUM',
        required: false,
        enabled: true,
        enumOptions: [
          { value: 'dev', label: 'Dev' },
          { value: 'homolog', label: 'Homolog' },
          { value: 'prod', label: 'Prod' },
        ],
        statusRequired: [],
      },
    ] as never;
    component.templateDefaults = [
      { key: 'sprint', value: 3 },
      { key: 'environment', value: 'homolog' },
    ] as never;
    component.initialValues = [
      { key: 'sprint', value: 8 },
      { key: 'environment', value: 'prod' },
    ] as never;
    component.ngOnChanges({
      definitions: {
        currentValue: component.definitions,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(component.valuesForm.getRawValue()).toEqual({
      sprint: 8,
      environment: 'prod',
    });

    component.templateDefaults = [
      { key: 'environment', value: 'homolog' },
    ] as never;
    component.initialValues = [];
    component.ngOnChanges({
      templateDefaults: {
        currentValue: component.templateDefaults,
        previousValue: null,
        firstChange: false,
        isFirstChange: () => false,
      },
      initialValues: {
        currentValue: component.initialValues,
        previousValue: null,
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    expect(component.valuesForm.getRawValue()).toEqual({
      sprint: null,
      environment: 'homolog',
    });
  });
});
