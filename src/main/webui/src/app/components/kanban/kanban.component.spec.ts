import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { KanbanComponent } from './kanban.component';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project, ProjectsService, ProjectWorkflow } from '../../services/projects.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { of } from 'rxjs';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { ProjectStatus } from '../../services/status.service';
import { Phase, PhaseService } from '../../services/phase.service';

describe('KanbanComponent', () => {
  let component: KanbanComponent;
  let fixture: ComponentFixture<KanbanComponent>;
  let mockActivatedRoute: any;
  let mockProjectsService: jasmine.SpyObj<ProjectsService>;
  let mockTicketService: jasmine.SpyObj<TicketService>;
  let mockPhaseService: jasmine.SpyObj<PhaseService>;

  const mockProject = {
    id: 1,
    name: 'Test Project',
    description: 'Test Description',
    prefix: 'PRJ',
    workflow: { id: 1, name: 'Waterfall' },
    owner: { id: 1, name: 'Owner', email: 'owner@issues.vepo.dev' },
    ticketTemplate: { enabled: false },
    phaseTemplate: { deliverables: [] },
  } as Project;

  const mockStatuses: ProjectStatus[] = [
    { id: 1, name: 'To Do', moveable: [2], start: false } as ProjectStatus,
    { id: 2, name: 'In Progress', moveable: [1, 3, 4], start: false } as ProjectStatus,
    { id: 3, name: 'Blocked', moveable: [2], start: false } as ProjectStatus,
    { id: 4, name: 'Done', moveable: [2], start: false } as ProjectStatus
  ];

  const emptyPlanningFields = {
    finishedAt: null,
    observedVersionId: null,
    observedVersionLabel: null,
    targetVersionId: null,
    targetVersionLabel: null,
    phaseId: null,
    phaseName: null
  };

  const mockTickets: Ticket[] = [
    {
      id: 1,
      identifier: 'PRJ-001',
      title: 'Ticket 1',
      description: 'Description 1',
      author: 1,
      project: 1,
      status: 1,
      assignee: 1,
      category: 1,
      categoryName: 'Bug',
      categoryColor: '#E53935',
      priority: 'MEDIUM',
      ...emptyPlanningFields
    },
    {
      id: 2,
      identifier: 'PRJ-002',
      title: 'Ticket 2',
      description: 'Description 2\nWith line break',
      author: 2,
      project: 1,
      status: 2,
      assignee: 1,
      category: 1,
      categoryName: 'Bug',
      categoryColor: '#E53935',
      priority: 'HIGH',
      ...emptyPlanningFields
    }
  ] as unknown as Ticket[];

  const mockWorkflow = {
    id: 1,
    name: "Waterfall",
    statuses: mockStatuses.map(s => s.name),
    start: mockStatuses[0].name,
    transitions: [
      { from: "To Do", to: "In Progress" },
      { from: "In Progress", to: "To Do" },
      { from: "In Progress", to: "Blocked" },
      { from: "In Progress", to: "Done" },
      { from: "Blocked", to: "In Progress" },
      { from: "Done", to: "In Progress" }
    ]
  } as ProjectWorkflow;

  beforeEach(waitForAsync(() => {
    mockProjectsService = jasmine.createSpyObj('ProjectsService', ['findWorkflowByProjectId']);
    mockTicketService = jasmine.createSpyObj('TicketService', ['move']);
    mockPhaseService = jasmine.createSpyObj('PhaseService', ['list']);
    mockPhaseService.list.and.returnValue(of([]));
    mockActivatedRoute = {
      data: of({
        statuses: mockStatuses,
        project: mockProject,
        tickets: mockTickets
      })
    };

    TestBed.configureTestingModule({
      imports: [CommonModule, DragDropModule, RouterLink, NormalizePipe, KanbanComponent],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: ProjectsService, useValue: mockProjectsService },
        { provide: TicketService, useValue: mockTicketService },
        { provide: PhaseService, useValue: mockPhaseService }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(KanbanComponent);
    component = fixture.componentInstance;
    mockProjectsService.findWorkflowByProjectId.and.returnValue(of(mockWorkflow));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('visibleTickets', () => {
    const mockPhases: Phase[] = [
      {
        id: 10,
        projectId: 1,
        name: 'Sprint A',
        status: 'COMPLETED',
        createdAt: '2026-01-01T00:00:00Z',
        deliverables: [],
      },
      {
        id: 20,
        projectId: 1,
        name: 'Sprint B',
        status: 'ACTIVE',
        createdAt: '2026-02-01T00:00:00Z',
        deliverables: [],
      },
    ];

    beforeEach(() => {
      mockPhaseService.list.and.returnValue(of(mockPhases));
      component.tickets = [
        { ...mockTickets[0], phaseId: 10, phaseName: 'Sprint A' },
        { ...mockTickets[1], phaseId: 20, phaseName: 'Sprint B' },
        { ...mockTickets[0], id: 3, identifier: 'PRJ-003', phaseId: null, phaseName: null },
      ] as Ticket[];
      component.phases = mockPhases;
      component.activePhaseId = 20;
    });

    it('should return all tickets when filter is all', () => {
      component.phaseFilter = 'all';
      expect(component.visibleTickets().length).toBe(3);
    });

    it('should return only active phase tickets', () => {
      component.phaseFilter = 'active';
      expect(component.visibleTickets().map(t => t.id)).toEqual([2]);
    });

    it('should return only unplanned tickets', () => {
      component.phaseFilter = 'unplanned';
      expect(component.visibleTickets().map(t => t.id)).toEqual([3]);
    });

    it('should return tickets for a selected phase', () => {
      component.phaseFilter = 'phase:10';
      expect(component.visibleTickets().map(t => t.id)).toEqual([1]);
    });
  });

  // it('should initialize with data from route', () => {
  //   expect(component.project).toEqual(mockProject);
  //   expect(component.statuses).toEqual(mockStatuses);
  //   expect(component.tickets.length).toBe(2);
  //   expect(component.workflow).toEqual(mockWorkflow);
  // });

  // it('should fix line breaks in ticket descriptions', () => {
  //   const ticketWithBreak = mockTickets[1];
  //   const fixedTicket = component.fixLineBreak(ticketWithBreak);
  //   expect(fixedTicket.description).toContain('<br/>');
  //   expect(fixedTicket.description).not.toContain('\n');
  // });

  // describe('ticketsOf', () => {
  //   it('should return tickets for a given status', () => {
  //     const todoTickets = component.ticketsOf(1);
  //     expect(todoTickets.length).toBe(1);
  //     expect(todoTickets[0].id).toBe(1);

  //     const inProgressTickets = component.ticketsOf(2);
  //     expect(inProgressTickets.length).toBe(1);
  //     expect(inProgressTickets[0].id).toBe(2);

  //     const doneTickets = component.ticketsOf(3);
  //     expect(doneTickets.length).toBe(0);
  //   });
  // });

  // describe('connectedTo', () => {
  //   it('should return connected column IDs for a status', () => {
  //     const todoConnections = component.connectedTo(mockStatuses[0]);
  //     expect(todoConnections).toEqual(['column-2']);

  //     const inProgressConnections = component.connectedTo(mockStatuses[1]);
  //     expect(inProgressConnections).toEqual(['column-1', 'column-3', 'column-4']);
  //   });
  // });

  // describe('toColumnId', () => {
  //   it('should convert status to column ID', () => {
  //     expect(component.toColumnId(mockStatuses[0])).toBe('column-1');
  //     expect(component.toColumnId(mockStatuses[1])).toBe('column-2');
  //   });
  // });

  // describe('fromColumnId', () => {
  //   it('should extract status ID from column ID', () => {
  //     expect(component.fromColumnId('column-1')).toBe(1);
  //     expect(component.fromColumnId('column-2')).toBe(2);
  //   });
  // });

  // describe('drop', () => {
  //   it('should not call move if status is the same', () => {
  //     const event = {
  //       previousContainer: { data: [mockTickets[0]], id: 'column-1' },
  //       container: { id: 'column-1', data: [] },
  //       previousIndex: 0,
  //       currentIndex: 0
  //     } as unknown as CdkDragDrop<any>;

  //     component.drop(event);
  //     expect(mockTicketService.move).not.toHaveBeenCalled();
  //   });

  //   it('should call move if status is different', () => {
  //     const updatedTicket = { ...mockTickets[0], status: 2 };
  //     mockTicketService.move.and.returnValue(of(updatedTicket));

  //     const event = {
  //       previousContainer: { data: [mockTickets[0]], id: 'column-1' },
  //       container: { id: 'column-2', data: [] },
  //       previousIndex: 0,
  //       currentIndex: 0
  //     } as unknown as CdkDragDrop<any>;

  //     component.drop(event);
  //     expect(mockTicketService.move).toHaveBeenCalledWith(1, 2);
  //   });
  // });

  // describe('template', () => {
    // it('should display project name', () => {
    //   const h1 = fixture.nativeElement.querySelector('h1');
    //   expect(h1.textContent).toContain(mockProject.name);
    // });

    // it('should render columns for each status', () => {
    //   const columns = fixture.nativeElement.querySelectorAll('.column');
    //   expect(columns.length).toBe(mockStatuses.length);
    // });

    // it('should render tickets in correct columns', () => {
    //   fixture.detectChanges();
    //   const todoColumn = fixture.nativeElement.querySelector('[id="column-1"]');
    //   const todoTickets = todoColumn.querySelectorAll('.card:not(.empty)');
    //   expect(todoTickets.length).toBe(1);
    //   expect(todoTickets[0].querySelector('.title').textContent).toContain('Ticket 1');

    //   const inProgressColumn = fixture.nativeElement.querySelector('[id="column-2"]');
    //   const inProgressTickets = inProgressColumn.querySelectorAll('.card:not(.empty)');
    //   expect(inProgressTickets.length).toBe(1);
    //   expect(inProgressTickets[0].querySelector('.title').textContent).toContain('Ticket 2');

    //   const doneColumn = fixture.nativeElement.querySelector('[id="column-3"]');
    //   const emptyCard = doneColumn.querySelector('.card.empty');
    //   expect(emptyCard).toBeTruthy();
    //   expect(emptyCard.textContent).toContain('Nenhum ticket...');
    // });

    // it('should render ticket links correctly', () => {
    //   fixture.detectChanges();
    //   const ticketLink = fixture.nativeElement.querySelector('.identifier a');
    //   expect(ticketLink.getAttribute('href')).toContain('/ticket/PRJ-001');
    //   expect(ticketLink.textContent).toContain('1');
    // });
  // });
});