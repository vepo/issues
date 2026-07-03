import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { TicketImportWizardComponent } from './ticket-import-wizard.component';
import { TicketImportService } from '../../services/ticket-import.service';
import { ProjectsService } from '../../services/projects.service';
import { UsersService } from '../../services/users.service';

describe('TicketImportWizardComponent', () => {
  let fixture: ComponentFixture<TicketImportWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketImportWizardComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ projectId: '1' }) },
            data: of({ project: { id: 1, name: 'Test Project' }, projects: [{ id: 1, name: 'Test Project' }] }),
          },
        },
        {
          provide: TicketImportService,
          useValue: {
            upload: jasmine.createSpy('upload').and.returnValue(of({ id: 1, headers: ['Title'], rowCount: 1, truncated: false, sampleRows: [], projectScoped: true })),
            applyMapping: jasmine.createSpy('applyMapping').and.returnValue(of(void 0)),
            preview: jasmine.createSpy('preview').and.returnValue(of({ rows: [], validCount: 0, invalidCount: 0 })),
            correctRow: jasmine.createSpy('correctRow').and.returnValue(of({ rowId: 1, rowNumber: 2, valid: true, errors: [] })),
            execute: jasmine.createSpy('execute').and.returnValue(of({
              created: [],
              errors: [],
              summary: { importedCount: 0, projectsImpacted: 0, ticketsByProject: [], ticketsByStatus: [] },
            })),
          },
        },
        {
          provide: ProjectsService,
          useValue: {
            findWorkflowByProjectId: jasmine.createSpy('findWorkflowByProjectId').and.returnValue(of({
              id: 1,
              name: 'Workflow',
              start: 'Open',
              statuses: ['Open', 'In Progress'],
              transitions: [{ from: 'Open', to: 'In Progress' }],
            })),
          },
        },
        {
          provide: UsersService,
          useValue: {
            search: jasmine.createSpy('search').and.returnValue(of([
              { id: 1, name: 'User', email: 'user@issues.vepo.dev', username: 'user', roles: ['USER'] },
            ])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketImportWizardComponent);
    fixture.detectChanges();
  });

  it('should render import wizard title', () => {
    const title = fixture.nativeElement.querySelector('.page-title');
    expect(title.textContent).toContain('Importar tickets via CSV');
  });

  it('should not allow proceeding from upload until CSV is uploaded', () => {
    expect(fixture.componentInstance.canProceedFromUpload()).toBeFalse();
  });

  it('should include project column mapping when not project scoped', () => {
    fixture.componentInstance.projectScoped = false;
    fixture.detectChanges();
    expect(fixture.componentInstance.requiredMappingFields().some(field => field.key === 'projectColumn')).toBeTrue();
  });

  it('should expose project fix when row has unknown project on global import', () => {
    fixture.componentInstance.projectScoped = false;
    fixture.componentInstance.projects = [{ id: 1, name: 'Test Project' } as import('../../services/projects.service').Project];
    const row = {
      rowId: 10,
      rowNumber: 2,
      valid: false,
      errors: ['Unknown project: Missing'],
      preview: { projectName: 'Missing' },
    };
    expect(fixture.componentInstance.hasProjectError(row)).toBeTrue();
  });
});
