import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Project, ProjectsService } from '../../services/projects.service';
import { ProjectMenuComponent } from './project-menu.component';

describe('ProjectMenuComponent', () => {
  let fixture: ComponentFixture<ProjectMenuComponent>;
  let projectsService: jasmine.SpyObj<ProjectsService>;
  let authService: jasmine.SpyObj<AuthService>;

  const sampleProjects: Project[] = [
    {
      id: 10,
      name: 'Alpha',
      prefix: 'ALP',
      description: 'A',
      workflow: { id: 1, name: 'Agile' },
      owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
      ticketTemplate: { enabled: false },
      phaseTemplate: { deliverables: [] },
      securityLevel: 'INTERNAL', prefixLocked: false,
    } as Project
  ];

  beforeEach(async () => {
    projectsService = jasmine.createSpyObj('ProjectsService', ['findAll']);
    projectsService.findAll.and.returnValue(of(sampleProjects));
    authService = jasmine.createSpyObj('AuthService', ['hasRole', 'isLoggedIn']);
    authService.hasRole.and.returnValue(false);
    authService.isLoggedIn.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [
        ProjectMenuComponent,
        TranslocoTestingModule.forRoot({
          langs: { pt: { shell: { noProjects: 'Nenhum projeto', projects: 'Projetos', manageProjects: 'Gerenciar projetos' } } },
          translocoConfig: { availableLangs: ['pt'], defaultLang: 'pt' },
        }),
      ],
      providers: [
        provideAnimations(),
        provideRouter([]),
        { provide: ProjectsService, useValue: projectsService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectMenuComponent);
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load viewable projects and link each item to Kanban', () => {
    fixture.detectChanges();

    const trigger = fixture.debugElement.query(By.css('[aria-label="Projetos"]'));
    expect(trigger).toBeTruthy();
    expect(trigger.nativeElement.disabled).toBeFalse();

    trigger.nativeElement.click();
    fixture.detectChanges();

    const items = document.querySelectorAll('.mat-mdc-menu-item');
    const kanbanLink = Array.from(items).find(el => el.textContent?.includes('Alpha'));
    expect(kanbanLink).toBeTruthy();
    expect(kanbanLink?.getAttribute('href') || kanbanLink?.getAttribute('ng-reflect-router-link'))
      .toContain('10');
    expect(fixture.componentInstance.kanbanLink(sampleProjects[0])).toEqual(['/project', 10, 'kanban']);
  });

  it('should disable the button and expose empty tooltip when there are no projects', () => {
    projectsService.findAll.and.returnValue(of([]));
    fixture.detectChanges();

    const trigger = fixture.debugElement.query(By.css('[aria-label="Projetos"]'));
    expect(trigger.nativeElement.disabled).toBeTrue();
    expect(fixture.componentInstance.emptyTooltip).toContain('Nenhum projeto');
  });

  it('should show Gerenciar projetos footer for project-manager or admin', () => {
    authService.hasRole.and.callFake((role: string) => role === 'project-manager' || role === 'admin');
    fixture.detectChanges();

    expect(fixture.componentInstance.canManageProjects()).toBeTrue();

    const trigger = fixture.debugElement.query(By.css('[aria-label="Projetos"]'));
    trigger.nativeElement.click();
    fixture.detectChanges();

    const manage = Array.from(document.querySelectorAll('.mat-mdc-menu-item'))
      .find(el => el.textContent?.includes('Gerenciar projetos'));
    expect(manage).toBeTruthy();
  });
});
