import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TicketFormComponent } from './ticket-form.component';
import { PhaseService } from '../../services/phase.service';
import { CustomFieldService } from '../../services/custom-field.service';
import { of } from 'rxjs';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('TicketFormComponent', () => {
  let component: TicketFormComponent;
  let fixture: ComponentFixture<TicketFormComponent>;

  beforeEach(async () => {
    const phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    phaseService.list.and.returnValue(of([]));
    const customFieldService = jasmine.createSpyObj('CustomFieldService', ['listInScope']);
    customFieldService.listInScope.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [createTranslocoTestingModule(), TicketFormComponent],
      providers: [
        { provide: PhaseService, useValue: phaseService },
        { provide: CustomFieldService, useValue: customFieldService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketFormComponent);
    component = fixture.componentInstance;
    component.projects = [{ id: 1, name: 'Issues', prefix: 'ISS', description: '', workflow: { id: 1, name: 'Agile' }, owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' }, ticketTemplate: { enabled: false }, phaseTemplate: { deliverables: [] }, securityLevel: 'INTERNAL', prefixLocked: false }];
    component.categories = [{ id: 2, name: 'Bug', color: 'red' }];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when form is invalid', () => {
    const submitted = jasmine.createSpy('submitted');
    component.submitted.subscribe(submitted);

    component.submit();

    expect(submitted).not.toHaveBeenCalled();
  });

  it('should default ticket type to TASK', () => {
    expect(component.ticketForm.value.ticketType).toBe('TASK');
  });

  it('should apply defaults input', () => {
    component.defaults = {
      title: 'Default title',
      description: 'Default description text',
      categoryId: 2,
      priority: 'HIGH',
      ticketType: 'EPIC',
    };
    fixture.detectChanges();

    expect(component.ticketForm.value.title).toBe('Default title');
    expect(component.ticketForm.value.categoryId).toBe(2);
    expect(component.ticketForm.value.priority).toBe('HIGH');
    expect(component.ticketForm.value.ticketType).toBe('EPIC');
  });

  it('should include ticketType on submit', () => {
    const submitted = jasmine.createSpy('submitted');
    component.submitted.subscribe(submitted);
    component.ticketForm.setValue({
      title: 'Valid ticket title',
      projectId: 1,
      description: '<p>Valid description text</p>',
      categoryId: 2,
      priority: 'MEDIUM',
      ticketType: 'STORY',
      dueDate: null,
      phaseId: null,
      storyPoints: 5,
    });
    component.submit();
    expect(submitted).toHaveBeenCalled();
    expect(submitted.calls.mostRecent().args[0].ticketType).toBe('STORY');
    expect(submitted.calls.mostRecent().args[0].storyPoints).toBe(5);
  });

  it('should include story points on submit', () => {
    const submitted = jasmine.createSpy('submitted');
    component.submitted.subscribe(submitted);
    component.ticketForm.setValue({
      title: 'Points ticket title',
      projectId: 1,
      description: '<p>Valid description text</p>',
      categoryId: 2,
      priority: 'HIGH',
      ticketType: 'TASK',
      dueDate: null,
      phaseId: null,
      storyPoints: 8,
    });
    component.submit();
    expect(submitted.calls.mostRecent().args[0].storyPoints).toBe(8);
  });

  it('should apply partial defaults without overwriting unset fields', () => {
    expect(component.ticketForm.value.title).toBe('');
    expect(component.ticketForm.value.priority).toBe('MEDIUM');

    component.defaults = { title: 'Template title only' };
    fixture.detectChanges();

    expect(component.ticketForm.value.title).toBe('Template title only');
    expect(component.ticketForm.value.description).toBe('');
    expect(component.ticketForm.value.categoryId).toBe(-1);
    expect(component.ticketForm.value.priority).toBe('MEDIUM');
  });

  it('should completely reset previous defaults before applying a new target baseline', () => {
    component.defaults = {
      title: 'Source clone title',
      description: '<p>Source clone description</p>',
      categoryId: 2,
      priority: 'HIGH',
      ticketType: 'EPIC',
      customFieldDefaults: [{ key: 'environment', value: 'prod' }],
    };

    component.defaults = {
      description: '<p>Target template description</p>',
      customFieldDefaults: [],
    };

    expect(component.ticketForm.value).toEqual(jasmine.objectContaining({
      title: '',
      description: '<p>Target template description</p>',
      categoryId: -1,
      priority: 'MEDIUM',
      ticketType: 'TASK',
    }));
    expect(component.customFieldDefaults).toEqual([]);
  });
});
