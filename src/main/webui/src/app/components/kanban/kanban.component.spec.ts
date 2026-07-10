import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { KanbanComponent } from './kanban.component';
import { CdkDrag, CdkDragDrop, CdkDropList, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Project, ProjectsService, ProjectWorkflow } from '../../services/projects.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { of } from 'rxjs';
import { NormalizePipe } from '../pipes/normalize.pipe';
import { ProjectStatus } from '../../services/status.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { ProjectMembersService } from '../../services/project-members.service';

describe('KanbanComponent', () => {
  let component: KanbanComponent;
  let fixture: ComponentFixture<KanbanComponent>;
  let mockActivatedRoute: any;
  let mockProjectsService: jasmine.SpyObj<ProjectsService>;
  let mockTicketService: jasmine.SpyObj<TicketService>;
  let mockPhaseService: jasmine.SpyObj<PhaseService>;
  let mockMembersService: jasmine.SpyObj<ProjectMembersService>;

  const mockProject = {
    id: 1,
    name: 'Test Project',
    description: 'Test Description',
    prefix: 'PRJ',
    workflow: { id: 1, name: 'Waterfall' },
    owner: { id: 1, name: 'Owner', email: 'owner@issues.vepo.dev' },
    ticketTemplate: { enabled: false },
    phaseTemplate: { deliverables: [] },
    prefixLocked: false,
  } as Project;

  const mockStatuses: ProjectStatus[] = [
    { id: 1, name: 'To Do', moveable: [2], start: false },
    { id: 2, name: 'In Progress', moveable: [1, 3, 4], start: false, wipLimit: 1 },
    { id: 3, name: 'Blocked', moveable: [2], start: false },
    { id: 4, name: 'Done', moveable: [2], start: false }
  ] as unknown as ProjectStatus[];

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
    mockMembersService = jasmine.createSpyObj('ProjectMembersService', ['listMembers']);
    mockPhaseService.list.and.returnValue(of([]));
    mockMembersService.listMembers.and.returnValue(of([{ id: 1, name: 'Alice', email: 'alice@issues.vepo.dev' }]));
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
        { provide: PhaseService, useValue: mockPhaseService },
        { provide: ProjectMembersService, useValue: mockMembersService }
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

  describe('connectedTo', () => {
    it('should return connected column IDs for moveable statuses', () => {
      expect(component.connectedTo(mockStatuses[0])).toEqual(['column-2']);
      expect(component.connectedTo(mockStatuses[1])).toEqual(['column-1', 'column-3', 'column-4']);
    });

    it('should connect to swimlane cells when swimlanes are enabled', () => {
      component.swimlaneMode = 'assignee';
      component.tickets = [
        { ...mockTickets[0], assignee: 1 },
        { ...mockTickets[1], assignee: null }
      ] as Ticket[];
      const connections = component.connectedTo(mockStatuses[0]);
      expect(connections).toContain('column-2__assignee:1');
      expect(connections).toContain('column-2__assignee:none');
    });
  });

  describe('swimlanes', () => {
    it('should return a single lane when mode is none', () => {
      component.swimlaneMode = 'none';
      expect(component.swimlanes()).toEqual([{ key: 'all', label: '' }]);
    });

    it('should group by assignee including unassigned', () => {
      component.swimlaneMode = 'assignee';
      component.members = [{ id: 1, name: 'Alice', email: 'alice@issues.vepo.dev' }];
      component.tickets = [
        { ...mockTickets[0], assignee: 1 },
        { ...mockTickets[1], assignee: null, status: 1 }
      ] as Ticket[];
      expect(component.swimlanes().map(l => l.key)).toEqual(['assignee:1', 'assignee:none']);
      expect(component.swimlanes()[0].label).toBe('Alice');
    });

    it('should group by priority', () => {
      component.swimlaneMode = 'priority';
      expect(component.swimlanes().map(l => l.key)).toEqual(['priority:HIGH', 'priority:MEDIUM']);
    });
  });

  describe('WIP', () => {
    it('should format wip label with limit', () => {
      expect(component.wipLabel(mockStatuses[1])).toBe('1/1');
      expect(component.wipLabel(mockStatuses[0])).toBe('1');
    });

    it('should report full WIP when count reaches limit', () => {
      expect(component.isWipFull(2)).toBeTrue();
      expect(component.isWipFull(1)).toBeFalse();
    });

    it('should block enter when target WIP is full', () => {
      const drag = { data: mockTickets[0] } as CdkDrag<Ticket>;
      const drop = { id: 'column-2', data: [] } as unknown as CdkDropList<Ticket[]>;
      expect(component.canEnterColumn(drag, drop)).toBeFalse();
    });

    it('should allow enter when same status even if WIP is full', () => {
      const drag = { data: mockTickets[1] } as CdkDrag<Ticket>;
      const drop = { id: 'column-2', data: [] } as unknown as CdkDropList<Ticket[]>;
      expect(component.canEnterColumn(drag, drop)).toBeTrue();
    });

    it('should not call move when dropping into a full WIP column', () => {
      const event = {
        previousContainer: { data: [mockTickets[0]], id: 'column-1' },
        container: { id: 'column-2', data: [] },
        previousIndex: 0,
        currentIndex: 0
      } as unknown as CdkDragDrop<Ticket[]>;
      component.drop(event);
      expect(mockTicketService.move).not.toHaveBeenCalled();
    });
  });
});
