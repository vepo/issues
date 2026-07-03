import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TicketFormComponent } from './ticket-form.component';

describe('TicketFormComponent', () => {
  let component: TicketFormComponent;
  let fixture: ComponentFixture<TicketFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketFormComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketFormComponent);
    component = fixture.componentInstance;
    component.projects = [{ id: 1, name: 'Issues', prefix: 'ISS', description: '', workflow: { id: 1, name: 'Agile' }, ticketTemplate: { enabled: false } }];
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

  it('should apply defaults input', () => {
    component.defaults = {
      title: 'Default title',
      description: 'Default description text',
      categoryId: 2,
      priority: 'HIGH',
    };
    fixture.detectChanges();

    expect(component.ticketForm.value.title).toBe('Default title');
    expect(component.ticketForm.value.categoryId).toBe(2);
    expect(component.ticketForm.value.priority).toBe('HIGH');
  });
});
