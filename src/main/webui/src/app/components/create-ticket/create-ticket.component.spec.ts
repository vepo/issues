import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ProjectsService } from '../../services/projects.service';
import { TicketService } from '../../services/ticket.service';
import { PhaseService } from '../../services/phase.service';
import { CreateTicketComponent } from './create-ticket.component';

describe('CreateTicketComponent', () => {
  let component: CreateTicketComponent;
  let fixture: ComponentFixture<CreateTicketComponent>;
  let ticketService: jasmine.SpyObj<TicketService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    ticketService = jasmine.createSpyObj('TicketService', ['createTicket']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    const phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    phaseService.list.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CreateTicketComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              projects: [{
                id: 1,
                name: 'Issues',
                prefix: 'ISS',
                description: '',
                workflow: { id: 1, name: 'Agile' },
                owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
                ticketTemplate: {
                  enabled: true,
                  title: 'New work item',
                  description: 'Describe the change here.',
                  categoryId: 2,
                  priority: 'MEDIUM',
                },
                phaseTemplate: { deliverables: [] },
              }],
              categories: [{ id: 2, name: 'Bug', color: 'red' }],
              project: {
                id: 1,
                name: 'Issues',
                prefix: 'ISS',
                description: '',
                workflow: { id: 1, name: 'Agile' },
                owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
                ticketTemplate: {
                  enabled: true,
                  title: 'New work item',
                  description: 'Describe the change here.',
                  categoryId: 2,
                  priority: 'MEDIUM',
                },
                phaseTemplate: { deliverables: [] },
              },
            }),
            snapshot: { paramMap: { get: () => '1' } },
          },
        },
        { provide: TicketService, useValue: ticketService },
        { provide: Router, useValue: router },
        { provide: ProjectsService, useValue: jasmine.createSpyObj('ProjectsService', ['findById']) },
        { provide: PhaseService, useValue: phaseService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTicketComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should pre-fill only configured template fields', () => {
    component.projects = [{
      id: 2,
      name: 'Partial',
      prefix: 'PAR',
      description: '',
      workflow: { id: 1, name: 'Agile' },
      owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
      ticketTemplate: { enabled: true, title: 'Title only' },
      phaseTemplate: { deliverables: [] },
    }];
    component['applyTemplateFromProject'](component.projects[0]);

    expect(component.formDefaults).toEqual({ title: 'Title only' });
  });

  it('should pre-fill defaults from project template', () => {
    expect(component.formDefaults).toEqual(jasmine.objectContaining({
      title: 'New work item',
      categoryId: 2,
      priority: 'MEDIUM',
    }));
  });
});
