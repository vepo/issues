import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { Project, ProjectsService } from './projects.service';
import { publicReadGuard } from './public-read.guard';

describe('publicReadGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let projectsService: jasmine.SpyObj<ProjectsService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['isLoggedIn']);
    projectsService = jasmine.createSpyObj('ProjectsService', ['findById']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ProjectsService, useValue: projectsService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('allows authenticated users without loading the project', () => {
    authService.isLoggedIn.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      publicReadGuard({ paramMap: { get: () => '1' } } as never, {} as never),
    );
    expect(result).toBeTrue();
    expect(projectsService.findById).not.toHaveBeenCalled();
  });

  it('allows anonymous users when the project is readable (Public)', (done) => {
    authService.isLoggedIn.and.returnValue(false);
    const project = {
      id: 1,
      name: 'Public',
      prefix: 'PUB',
      description: '',
      workflow: { id: 1, name: 'W' },
      owner: { id: 1, name: 'O', email: 'o@x' },
      securityLevel: 'PUBLIC',
      ticketTemplate: { enabled: false },
      phaseTemplate: { deliverables: [] },
      prefixLocked: false,
    } as Project;
    projectsService.findById.and.returnValue(of(project));
    const result = TestBed.runInInjectionContext(() =>
      publicReadGuard(
        { paramMap: { get: (k: string) => (k === 'projectId' ? '1' : null) } } as never,
        {} as never,
      ),
    ) as Observable<boolean>;
    result.subscribe((allowed) => {
      expect(allowed).toBeTrue();
      done();
    });
  });

  it('redirects anonymous users to login when the project is not readable', (done) => {
    authService.isLoggedIn.and.returnValue(false);
    projectsService.findById.and.returnValue(throwError(() => new Error('403')));
    const result = TestBed.runInInjectionContext(() =>
      publicReadGuard(
        { paramMap: { get: (k: string) => (k === 'projectId' ? '1' : null) } } as never,
        {} as never,
      ),
    ) as Observable<boolean>;
    result.subscribe((allowed) => {
      expect(allowed).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
      done();
    });
  });
});
