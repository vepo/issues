import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UiLocaleService } from '../core/ui-locale.service';
import { AuthApi } from '../generated/api/auth.service';
import { AuthService } from './auth.service';

describe('AuthService locale reconciliation', () => {
  let authApi: jasmine.SpyObj<AuthApi>;
  let authService: AuthService;
  let uiLocaleService: jasmine.SpyObj<UiLocaleService>;

  beforeEach(() => {
    authApi = jasmine.createSpyObj<AuthApi>('AuthApi', ['login', 'registerUser', 'me']);
    uiLocaleService = jasmine.createSpyObj<UiLocaleService>(
      'UiLocaleService',
      ['setActiveLocale'],
      { currentLocale: 'en' }
    );

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: AuthApi, useValue: authApi },
        { provide: UiLocaleService, useValue: uiLocaleService },
      ],
    });
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should apply the persisted current user locale during authentication reconciliation', () => {
    authApi.me.and.returnValue(of({
      id: 1,
      username: 'user',
      name: 'User',
      email: 'user@issues.vepo.dev',
      roles: new Set(['user']),
      locale: 'en',
    }) as any);

    authService.me().subscribe();

    expect(uiLocaleService.setActiveLocale).toHaveBeenCalledOnceWith('en');
  });

  it('should register with the active locale without path-derived locale selection', () => {
    authApi.registerUser.and.returnValue(of({
      id: 1,
      username: 'newuser',
      name: 'New User',
      email: 'new@issues.vepo.dev',
      roles: ['user'],
    }) as any);

    authService.register('newuser', 'New User', 'new@issues.vepo.dev', 'Secret123').subscribe();

    expect(authApi.registerUser).toHaveBeenCalledWith({
      username: 'newuser',
      name: 'New User',
      email: 'new@issues.vepo.dev',
      password: 'Secret123',
      locale: 'en',
    });
  });
});
