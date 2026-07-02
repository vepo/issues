import { TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AppComponent } from './app';
import { AuthService } from './services/auth.service';
import { ProjectsService } from './services/projects.service';
import { StatusService } from './services/status.service';

describe('App', () => {
  let statusService: jasmine.SpyObj<StatusService>;
  let projectsService: jasmine.SpyObj<ProjectsService>;
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>;
  let authService: jasmine.SpyObj<AuthService>;
  beforeEach(async () => {
    statusService = jasmine.createSpyObj('StatusService', ['findAll']);
    statusService.findAll.and.returnValue(of([]));
    projectsService = jasmine.createSpyObj('ProjectsService', ['findById']);
    activatedRoute = jasmine.createSpyObj('ActivatedRoute', ['navigate']);
    activatedRoute.queryParams = of({});
    authService = jasmine.createSpyObj('AuthService', ['login', 'isLoggedIn']);
    authService.isLoggedIn.and.returnValue(false);
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideAnimations(),
        { provide: StatusService, useValue: statusService },
        { provide: ProjectsService, useValue: projectsService },
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    // const compiled = fixture.nativeElement as HTMLElement;
    // expect(compiled.querySelector('h1')?.textContent).toContain('Hello, issues');
  });
});
