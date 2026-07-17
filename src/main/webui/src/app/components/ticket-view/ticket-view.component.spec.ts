import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TicketViewComponent } from './ticket-view.component';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { ProjectMembersService } from '../../services/project-members.service';
import { ProjectsService } from '../../services/projects.service';
import { CategoryService } from '../../services/category.service';
import { StatusService } from '../../services/status.service';
import { VersionService } from '../../services/version.service';
import { PhaseService } from '../../services/phase.service';
import { CustomFieldService } from '../../services/custom-field.service';
import { buildActivityFeed, filterActivity } from '../ticket-activity-feed/activity-feed.utils';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('ticket-view activity merge', () => {
  it('should keep newest comment before older history when building feed', () => {
    const history = [
      { id: 1, action: 'CREATED', timestamp: 1000, user: { name: 'Alice' } },
    ];
    const comments = [
      { id: 2, content: 'note', createdAt: 2000, author: { name: 'Bob' } },
    ];

    const feed = buildActivityFeed(history as never, comments as never);
    expect(feed[0].kind).toBe('comment');
    expect(filterActivity(feed, 'changes')).toHaveSize(1);
  });

  it('should merge linked commits into history tab items', () => {
    const history = [
      { id: 1, action: 'CREATED', timestamp: 1000, user: { name: 'Alice' } },
    ];
    const linkedCommits = [
      {
        id: 5,
        sha: 'abc123def456',
        message: 'fix(auth): redirect',
        authorName: 'Dev',
        committedAt: '2026-07-11T15:00:00Z',
        commitUrl: 'https://github.com/org/repo/commit/abc123',
      },
    ];
    const feed = buildActivityFeed(history as never, [], linkedCommits);
    expect(feed.some(item => item.kind === 'commit')).toBeTrue();
  });
});

describe('TicketViewComponent links', () => {
  let fixture: ComponentFixture<TicketViewComponent>;
  let component: TicketViewComponent;
  let ticketService: jasmine.SpyObj<TicketService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  const epicTicket = {
    id: 42,
    identifier: 'ISS-42',
    title: 'Implement ticket links',
    description: '<p>Epic description text here</p>',
    category: 'Feature',
    categoryColor: '#336699',
    priority: 'MEDIUM',
    ticketType: 'EPIC',
    author: { id: 1, name: 'Admin', username: 'admin', email: 'admin@issues.vepo.dev' },
    assignee: null,
    subscribers: [],
    project: { id: 1, name: 'Issues' },
    status: 'TODO',
    finishedAt: null,
    canceledAt: null,
    dueDate: null,
    storyPoints: null,
    observedVersionId: null,
    observedVersionLabel: null,
    targetVersionId: null,
    targetVersionLabel: null,
    phaseId: null,
    phaseName: null,
    deleted: false,
    history: [
      { action: 'LINK_ADDED', field: null, oldValue: null, newValue: 'BLOCKS → ISS-004', timestamp: '2026-07-11T10:00:00', user: { email: 'admin@issues.vepo.dev' } },
    ],
    customFields: [],
    links: [
      {
        id: 10,
        linkType: 'BLOCKS',
        displayLabel: 'Bloqueia',
        direction: 'OUTBOUND',
        otherTicketId: 4,
        otherIdentifier: 'ISS-004',
        otherTitle: 'Configuração da Build Integrada',
        otherStatus: 'TODO',
        otherProjectId: 1,
        otherProjectPrefix: 'ISS',
        otherDeleted: false,
      },
      {
        id: 11,
        linkType: 'CHILD_OF',
        displayLabel: 'Pai de',
        direction: 'INBOUND',
        otherTicketId: 43,
        otherIdentifier: 'ISS-043',
        otherTitle: 'Schema + entity',
        otherStatus: 'DONE',
        otherProjectId: 1,
        otherProjectPrefix: 'ISS',
        otherDeleted: false,
      },
      {
        id: 12,
        linkType: 'CHILD_OF',
        displayLabel: 'Pai de',
        direction: 'INBOUND',
        otherTicketId: 44,
        otherIdentifier: 'ISS-044',
        otherTitle: 'Link API',
        otherStatus: 'IN_PROGRESS',
        otherProjectId: 1,
        otherProjectPrefix: 'ISS',
        otherDeleted: false,
      },
    ],
    childrenSummary: { total: 2, done: 1 },
  };

  beforeEach(async () => {
    ticketService = jasmine.createSpyObj('TicketService', [
      'getComments',
      'createLink',
      'deleteLink',
      'createChild',
      'findExpandedByIdentifier',
      'search',
      'move',
      'update',
      'listAttachments',
      'uploadAttachment',
      'downloadAttachment',
      'deleteAttachment',
    ]);
    ticketService.getComments.and.returnValue(of([]));
    ticketService.listAttachments.and.returnValue(of([]));
    ticketService.createLink.and.returnValue(of(epicTicket.links[0] as any));
    ticketService.deleteLink.and.returnValue(of(undefined));
    ticketService.findExpandedByIdentifier.and.returnValue(of(epicTicket as any));
    ticketService.search.and.returnValue(of([]));
    ticketService.move.and.returnValue(of(epicTicket as any));
    ticketService.uploadAttachment.and.returnValue(of({
      id: 1,
      originalFilename: 'spec.pdf',
      contentType: 'application/pdf',
      sizeBytes: 1024,
      uploadedBy: { id: 1, name: 'Admin', username: 'admin', email: 'admin@issues.vepo.dev' },
      uploadedAt: Date.now(),
    } as any));
    ticketService.downloadAttachment.and.returnValue(of(new Blob(['x'])));
    ticketService.deleteAttachment.and.returnValue(of(undefined));

    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);

    const membersService = jasmine.createSpyObj('ProjectMembersService', ['listMembers']);
    membersService.listMembers.and.returnValue(of([]));
    const projectsService = jasmine.createSpyObj('ProjectsService', ['findWorkflowByProjectId']);
    projectsService.findWorkflowByProjectId.and.returnValue(of({
      id: 1,
      name: 'Agile',
      finishStatuses: [{ status: 'DONE', outcome: 'DONE' }],
    }));
    const categoryService = jasmine.createSpyObj('CategoryService', ['findAll']);
    categoryService.findAll.and.returnValue(of([{ id: 1, name: 'Feature', color: '#336699' }]));
    const statusService = jasmine.createSpyObj('StatusService', ['findProjectsStatuses']);
    statusService.findProjectsStatuses.and.returnValue(of([
      { id: 1, name: 'TODO', start: true, moveable: [2] },
      { id: 2, name: 'DONE', start: false, moveable: [] },
    ]));
    const versionService = jasmine.createSpyObj('VersionService', ['list']);
    versionService.list.and.returnValue(of([]));
    const phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    phaseService.list.and.returnValue(of([]));
    const customFieldService = jasmine.createSpyObj('CustomFieldService', ['listInScope']);
    customFieldService.listInScope.and.returnValue(of([]));
    const authService = jasmine.createSpyObj('AuthService', ['getAuthUserId', 'hasRole']);
    authService.getAuthUserId.and.returnValue(1);
    authService.hasRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [
        createTranslocoTestingModule(
          {
            ticket: {
              clone: 'Clonar ticket',
              attachments: 'Anexos',
              download: 'Baixar',
              linkAdded: 'Vínculo adicionado',
              childrenCompleted: '{{done}}/{{total}} concluídas',
            },
          },
          {
            ticket: {
              clone: 'Clone ticket',
              attachments: 'Attachments',
              download: 'Download',
              linkAdded: 'Link added',
              childrenCompleted: '{{done}}/{{total}} completed',
            },
          },
        ),
        TicketViewComponent,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: TicketService, useValue: ticketService },
        { provide: AuthService, useValue: authService },
        { provide: ProjectMembersService, useValue: membersService },
        { provide: ProjectsService, useValue: projectsService },
        { provide: CategoryService, useValue: categoryService },
        { provide: StatusService, useValue: statusService },
        { provide: VersionService, useValue: versionService },
        { provide: PhaseService, useValue: phaseService },
        { provide: CustomFieldService, useValue: customFieldService },
        { provide: MatDialog, useValue: dialog },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ ticket: epicTicket }) },
        },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    fixture = TestBed.createComponent(TicketViewComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should show type badge Épico in header', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Épico');
  });

  it('should navigate from an active ticket to the global clone flow', () => {
    const cloneButton = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find(button => button.textContent?.includes('Clonar ticket'));

    expect(cloneButton).toBeDefined();
    cloneButton?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/tickets/new'], {
      queryParams: { cloneFrom: 42, targetProjectId: 1 },
    });
  });

  it('should hide clone action for a deleted ticket', () => {
    component.ticket = { ...epicTicket, deleted: true } as never;
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Clonar ticket');
  });

  it('should group and render vínculos', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Vínculos');
    expect(text).toContain('Bloqueia');
    expect(text).toContain('ISS-004');
  });

  it('should show subtarefas progress for epic', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Subtarefas');
    expect(text).toContain('1/2 concluídas');
    expect(text).toContain('ISS-043');
    expect(text).toContain('ISS-044');
  });

  it('should create a peer link', () => {
    component.selectedLinkTarget = {
      id: 99,
      identifier: 'ISS-099',
      title: 'Other ticket',
    } as never;
    component.newLinkType = 'RELATES_TO';
    component.createLink();
    expect(ticketService.createLink).toHaveBeenCalledWith(42, {
      targetTicketId: 99,
      linkType: 'RELATES_TO',
    });
  });

  it('should remove a link', () => {
    component.removeLink(epicTicket.links[0] as never);
    expect(ticketService.deleteLink).toHaveBeenCalledWith(42, 10);
  });

  it('should label LINK_ADDED history in Portuguese', () => {
    expect(component.historyActionLabel('LINK_ADDED')).toBe('Vínculo adicionado');
    expect(component.historyActionLabel('LINK_REMOVED')).toBe('Vínculo removido');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Vínculo adicionado');
  });

  it('should label ATTACHMENT history actions', () => {
    expect(component.historyActionLabel('ATTACHMENT_ADDED')).toBe('Anexo adicionado');
    expect(component.historyActionLabel('ATTACHMENT_REMOVED')).toBe('Anexo removido');
  });

  it('should render Anexos section and upload selected file', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Anexos');
    expect(text).toContain('Nenhum anexo neste ticket.');

    component.attachments = [{
      id: 7,
      originalFilename: 'spec.pdf',
      contentType: 'application/pdf',
      sizeBytes: 2048,
      uploadedBy: { id: 1, name: 'Admin', username: 'admin', email: 'admin@issues.vepo.dev' },
      uploadedAt: Date.now(),
    } as never];
    fixture.detectChanges();
    const withFile = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(withFile).toContain('spec.pdf');
    expect(withFile).toContain('Baixar');

    const file = new File(['hello'], 'notes.txt', { type: 'text/plain' });
    component.selectedAttachmentFile = file;
    component.uploadAttachment();
    expect(ticketService.uploadAttachment).toHaveBeenCalledWith(42, file);
  });

  it('should rerender ticket, activity, attachment, and child-count copy while preserving authored content', async () => {
    component.attachments = [{
      id: 7,
      originalFilename: 'release-notes-pt.pdf',
      contentType: 'application/pdf',
      sizeBytes: 2048,
      uploadedBy: { id: 1, name: 'Admin', username: 'admin', email: 'admin@issues.vepo.dev' },
      uploadedAt: Date.now(),
    } as never];
    fixture.detectChanges();

    const transloco = TestBed.inject(TranslocoService);
    expect(fixture.nativeElement.textContent).toContain('Clonar ticket');
    expect(fixture.nativeElement.textContent).toContain('Anexos');
    expect(fixture.nativeElement.textContent).toContain('1/2 concluídas');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Clone ticket');
    expect(text).toContain('Attachments');
    expect(text).toContain('Download');
    expect(text).toContain('Link added');
    expect(text).toContain('1/2 completed');
    expect(text).toContain('Implement ticket links');
    expect(text).toContain('Configuração da Build Integrada');
    expect(text).toContain('release-notes-pt.pdf');
    expect(text).not.toContain('Clonar ticket');
  });

  it('should download and confirm delete attachment', () => {
    const attachment = {
      id: 7,
      originalFilename: 'spec.pdf',
      contentType: 'application/pdf',
      sizeBytes: 2048,
      uploadedBy: { id: 1, name: 'Admin', username: 'admin', email: 'admin@issues.vepo.dev' },
      uploadedAt: Date.now(),
    } as never;
    component.downloadAttachment(attachment);
    expect(ticketService.downloadAttachment).toHaveBeenCalledWith(42, 7);

    component.confirmDeleteAttachment(attachment);
    expect(dialog.open).toHaveBeenCalled();
    expect(ticketService.deleteAttachment).toHaveBeenCalledWith(42, 7);
  });

  it('should render linked commits in history tab', () => {
    component.ticket = {
      ...epicTicket,
      linkedCommits: [{
        id: 99,
        sha: 'abc123def456',
        message: 'fix(auth): redirect (ISS-42)',
        authorName: 'Dev User',
        committedAt: '2026-07-12T10:00:00Z',
        commitUrl: 'https://github.com/org/repo/commit/abc123',
      }],
    } as never;
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('abc123d');
    expect(text).toContain('fix(auth): redirect (ISS-42)');
    expect(text).toContain('Abrir');
  });

  it('should warn when moving epic to DONE with open children', () => {
    component.selectedStatusId = 2;
    component.moveTicket();
    expect(dialog.open).toHaveBeenCalled();
    expect(ticketService.move).toHaveBeenCalledWith(42, 2);
  });

  it('should search tickets for link target', fakeAsync(() => {
    ticketService.search.and.returnValue(of([
      { id: 99, identifier: 'ISS-099', title: 'Candidate', deleted: false },
    ] as any));
    component.onLinkSearchInput('Candidate ticket');
    tick(300);
    expect(ticketService.search).toHaveBeenCalledWith('Candidate ticket', -1);
    expect(component.linkSearchResults.length).toBe(1);
  }));
});
