import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ProjectsService } from '../../services/projects.service';
import { TicketService } from '../../services/ticket.service';
import { CreateTicketComponent } from './create-ticket.component';

describe('CreateTicketComponent', () => {
  let component: CreateTicketComponent;
  let fixture: ComponentFixture<CreateTicketComponent>;
  let ticketService: jasmine.SpyObj<TicketService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    ticketService = jasmine.createSpyObj('TicketService', ['createTicket']);
    router = jasmine.createSpyObj('Router', ['navigate']);

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
                ticketTemplate: {
                  enabled: true,
                  title: 'New work item',
                  description: 'Describe the change here.',
                  categoryId: 2,
                  priority: 'MEDIUM',
                },
              }],
              categories: [{ id: 2, name: 'Bug', color: 'red' }],
              project: {
                id: 1,
                name: 'Issues',
                prefix: 'ISS',
                description: '',
                workflow: { id: 1, name: 'Agile' },
                ticketTemplate: {
                  enabled: true,
                  title: 'New work item',
                  description: 'Describe the change here.',
                  categoryId: 2,
                  priority: 'MEDIUM',
                },
              },
            }),
            snapshot: { paramMap: { get: () => '1' } },
          },
        },
        { provide: TicketService, useValue: ticketService },
        { provide: Router, useValue: router },
        { provide: ProjectsService, useValue: jasmine.createSpyObj('ProjectsService', ['findById']) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTicketComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should pre-fill defaults from project template', () => {
    expect(component.formDefaults).toEqual(jasmine.objectContaining({
      title: 'New work item',
      categoryId: 2,
      priority: 'MEDIUM',
    }));
  });
});
