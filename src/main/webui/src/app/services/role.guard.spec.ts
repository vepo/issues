import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let homeUrl: UrlTree;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['hasRole']);
    homeUrl = {} as UrlTree;
    router = jasmine.createSpyObj('Router', ['parseUrl']);
    router.parseUrl.and.returnValue(homeUrl);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('allows admin on admin-only routes', () => {
    authService.hasRole.and.callFake((role: string) => role === 'admin');
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(['admin'])({} as never, {} as never),
    );
    expect(result).toBeTrue();
  });

  it('blocks non-admin on admin-only routes and redirects home', () => {
    authService.hasRole.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(['admin'])({} as never, {} as never),
    );
    expect(result).toBe(homeUrl);
    expect(router.parseUrl).toHaveBeenCalledWith('/');
  });

  it('allows project-manager when listed among allowed roles', () => {
    authService.hasRole.and.callFake((role: string) => role === 'project-manager');
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(['admin', 'project-manager'])({} as never, {} as never),
    );
    expect(result).toBeTrue();
  });
});
