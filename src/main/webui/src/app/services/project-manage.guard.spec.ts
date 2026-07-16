import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { Project, ProjectsService } from './projects.service';
import { projectManageGuard } from './project-manage.guard';

describe('projectManageGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let projectsService: jasmine.SpyObj<ProjectsService>;
  let router: jasmine.SpyObj<Router>;
  let homeUrl: UrlTree;

  const project = {
    id: 10,
    name: 'Demo',
    prefix: 'DEM',
    description: '',
    workflow: { id: 1, name: 'W' },
    owner: { id: 42, name: 'Owner', email: 'owner@example.com' },
    securityLevel: 'INTERNAL',
    ticketTemplate: { enabled: false },
    phaseTemplate: { deliverables: [] },
    prefixLocked: false,
  } as Project;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['hasRole', 'getAuthUserId']);
    projectsService = jasmine.createSpyObj('ProjectsService', ['findById']);
    homeUrl = {} as UrlTree;
    router = jasmine.createSpyObj('Router', ['parseUrl']);
    router.parseUrl.and.returnValue(homeUrl);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ProjectsService, useValue: projectsService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('allows admin without loading the project', () => {
    authService.hasRole.and.callFake((role: string) => role === 'admin');
    const result = TestBed.runInInjectionContext(() =>
      projectManageGuard(
        { paramMap: { get: () => '10' } } as never,
        {} as never,
      ),
    );
    expect(result).toBeTrue();
    expect(projectsService.findById).not.toHaveBeenCalled();
  });

  it('allows the project owner', (done) => {
    authService.hasRole.and.returnValue(false);
    authService.getAuthUserId.and.returnValue(42);
    projectsService.findById.and.returnValue(of(project));
    const result = TestBed.runInInjectionContext(() =>
      projectManageGuard(
        { paramMap: { get: (k: string) => (k === 'projectId' ? '10' : null) } } as never,
        {} as never,
      ),
    ) as Observable<boolean | UrlTree>;
    result.subscribe((allowed) => {
      expect(allowed).toBeTrue();
      done();
    });
  });

  it('redirects non-owner non-admin home', (done) => {
    authService.hasRole.and.returnValue(false);
    authService.getAuthUserId.and.returnValue(99);
    projectsService.findById.and.returnValue(of(project));
    const result = TestBed.runInInjectionContext(() =>
      projectManageGuard(
        { paramMap: { get: (k: string) => (k === 'projectId' ? '10' : null) } } as never,
        {} as never,
      ),
    ) as Observable<boolean | UrlTree>;
    result.subscribe((allowed) => {
      expect(allowed).toBe(homeUrl);
      done();
    });
  });

  it('redirects home when project load fails', (done) => {
    authService.hasRole.and.returnValue(false);
    authService.getAuthUserId.and.returnValue(42);
    projectsService.findById.and.returnValue(throwError(() => new Error('403')));
    const result = TestBed.runInInjectionContext(() =>
      projectManageGuard(
        { paramMap: { get: (k: string) => (k === 'projectId' ? '10' : null) } } as never,
        {} as never,
      ),
    ) as Observable<boolean | UrlTree>;
    result.subscribe((allowed) => {
      expect(allowed).toBe(homeUrl);
      done();
    });
  });
});
