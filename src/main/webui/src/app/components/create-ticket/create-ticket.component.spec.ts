import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { Project, ProjectsService } from '../../services/projects.service';
import { Ticket, TicketService } from '../../services/ticket.service';
import { PhaseService } from '../../services/phase.service';
import { CustomFieldService } from '../../services/custom-field.service';
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
    const customFieldService = jasmine.createSpyObj('CustomFieldService', ['listInScope']);
    customFieldService.listInScope.and.returnValue(of([]));

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
        { provide: CustomFieldService, useValue: customFieldService },
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
      securityLevel: 'INTERNAL', prefixLocked: false,
    }];
    component['applyTemplateFromProject'](component.projects[0]);

    expect(component.formDefaults).toEqual({ title: 'Title only', customFieldDefaults: [] });
  });

  it('should pre-fill defaults from project template', () => {
    expect(component.formDefaults).toEqual(jasmine.objectContaining({
      title: 'New work item',
      categoryId: 2,
      priority: 'MEDIUM',
      customFieldDefaults: [],
    }));
  });
});

describe('CreateTicketComponent clone flow', () => {
  let component: CreateTicketComponent;
  let fixture: ComponentFixture<CreateTicketComponent>;
  let ticketService: jasmine.SpyObj<TicketService> & { getClonePrefill: jasmine.Spy };
  let projectsService: jasmine.SpyObj<ProjectsService> & { listWritable: jasmine.Spy };
  let router: jasmine.SpyObj<Router>;

  const sourceProject = {
    id: 1,
    name: 'Issues',
    prefix: 'ISS',
    description: '',
    workflow: { id: 1, name: 'Agile' },
    owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
    ticketTemplate: {
      enabled: true,
      title: 'Source template title',
      description: 'Source template description',
      categoryId: 2,
      priority: 'MEDIUM',
      customFieldDefaults: [{ key: 'environment', value: 'dev' }],
    },
    phaseTemplate: { deliverables: [] },
    securityLevel: 'INTERNAL',
    prefixLocked: false,
  };
  const targetProject = {
    ...sourceProject,
    id: 2,
    name: 'Target',
    prefix: 'TGT',
    ticketTemplate: {
      enabled: true,
      title: 'Target template title',
      description: 'Target template description',
      categoryId: 3,
      priority: 'LOW',
      customFieldDefaults: [
        { key: 'environment', value: 'homolog' },
        { key: 'sprint', value: 3 },
      ],
    },
  };
  const sourcePrefill = {
    sourceIdentifier: 'ISS-42',
    targetProjectId: 1,
    title: 'Original ticket title',
    description: '<p>Original ticket description</p>',
    categoryId: 2,
    priority: 'HIGH',
    ticketType: 'STORY',
    customFields: [{ key: 'environment', value: 'prod' }],
    warnings: ['Campo legado não foi copiado.'],
  };
  const targetPrefill = {
    ...sourcePrefill,
    targetProjectId: 2,
    categoryId: 3,
    customFields: [{ key: 'environment', value: 'prod' }],
    warnings: ['Sprint da origem foi omitido.'],
  };

  beforeEach(async () => {
    ticketService = jasmine.createSpyObj('TicketService', [
      'createTicket',
      'getClonePrefill',
    ]) as typeof ticketService;
    ticketService.getClonePrefill.and.callFake(
      (_sourceId: number, targetProjectId: number) =>
        of(targetProjectId === 1 ? sourcePrefill : targetPrefill),
    );
    ticketService.createTicket.and.returnValue(of({
      id: 100,
      identifier: 'TGT-100',
    } as unknown as Ticket));

    projectsService = jasmine.createSpyObj('ProjectsService', [
      'findById',
      'listWritable',
    ]) as typeof projectsService;
    projectsService.listWritable.and.returnValue(of([sourceProject, targetProject]));
    projectsService.findById.and.callFake((projectId: number) =>
      of((projectId === 1 ? sourceProject : targetProject) as unknown as Project));
    router = jasmine.createSpyObj('Router', ['navigate']);

    const queryParamMap = convertToParamMap({
      cloneFrom: '42',
      targetProjectId: '1',
    });
    const phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    phaseService.list.and.returnValue(of([]));
    const customFieldService = jasmine.createSpyObj('CustomFieldService', ['listInScope']);
    customFieldService.listInScope.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CreateTicketComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              projects: [{ ...sourceProject, id: 99, name: 'Readable only' }],
              categories: [
                { id: 2, name: 'Bug', color: 'red' },
                { id: 3, name: 'Feature', color: 'blue' },
              ],
            }),
            queryParamMap: of(queryParamMap),
            snapshot: {
              paramMap: convertToParamMap({}),
              queryParamMap,
            },
          },
        },
        { provide: TicketService, useValue: ticketService },
        { provide: Router, useValue: router },
        { provide: ProjectsService, useValue: projectsService },
        { provide: PhaseService, useValue: phaseService },
        { provide: CustomFieldService, useValue: customFieldService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTicketComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load writable projects and source-target clone prefill from query context', () => {
    expect(projectsService.listWritable).toHaveBeenCalled();
    expect(component.projects.map(project => project.id)).toEqual([1, 2]);
    expect(ticketService.getClonePrefill).toHaveBeenCalledWith(42, 1);
    expect(component.initialProjectId).toBe(1);
    expect(component.lockProject).toBeFalse();
    expect(component.formDefaults).toEqual(jasmine.objectContaining({
      title: 'Original ticket title',
      description: '<p>Original ticket description</p>',
      priority: 'HIGH',
      ticketType: 'STORY',
    }));
  });

  it('should reload target-aware prefill and overlay clone values on target template', () => {
    component.onProjectSelected(2);

    expect(ticketService.getClonePrefill).toHaveBeenCalledWith(42, 2);
    expect(component.formDefaults).toEqual(jasmine.objectContaining({
      title: 'Original ticket title',
      description: '<p>Original ticket description</p>',
      categoryId: 3,
      priority: 'HIGH',
      ticketType: 'STORY',
      customFieldDefaults: [
        { key: 'environment', value: 'prod' },
        { key: 'sprint', value: 3 },
      ],
    }));
  });

  it('should display clone omission warnings', () => {
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Campo legado não foi copiado.');
  });

  it('should submit clone values through the existing create API', () => {
    const request = {
      projectId: 2,
      title: 'Reviewed clone title',
      description: '<p>Reviewed clone description</p>',
      categoryId: 3,
      priority: 'HIGH',
      ticketType: 'STORY',
      customFields: [{ key: 'environment', value: 'prod' }],
    } as never;

    component.save(request);

    expect(ticketService.createTicket).toHaveBeenCalledWith(request);
    expect(router.navigate).toHaveBeenCalledWith(['/', 'ticket', 'TGT-100']);
  });
});
