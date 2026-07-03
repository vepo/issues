import { TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { AppComponent } from './app';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';

describe('App', () => {
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>;
  let authService: jasmine.SpyObj<AuthService>;
  let notificationService: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    activatedRoute = jasmine.createSpyObj('ActivatedRoute', ['navigate']);
    activatedRoute.queryParams = of({});
    authService = jasmine.createSpyObj('AuthService', [
      'login',
      'isLoggedIn',
      'getEmail',
      'hasRole',
      'logout'
    ]);
    authService.isLoggedIn.and.returnValue(false);
    authService.getEmail.and.returnValue(null);
    authService.hasRole.and.returnValue(false);

    notificationService = jasmine.createSpyObj('NotificationService', ['connect', 'listen']);
    notificationService.listen.and.returnValue(EMPTY);

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show login button when logged out', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.btn-header')).toBeTruthy();
    expect(compiled.querySelector('#header-status')).toBeNull();
  });

  it('should show search and user menu when authenticated', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getEmail.and.returnValue('user@issues.vepo.dev');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.search-bar')).toBeTruthy();
    expect(compiled.querySelector('.search-bar__action')).toBeNull();
    expect(compiled.querySelector('#header-status')).toBeNull();
    expect(compiled.querySelector('app-context-bar')).toBeTruthy();
    expect(compiled.querySelector('[aria-label="Menu do usuário"]')).toBeTruthy();
    expect(compiled.querySelector('[aria-label="Abrir menu"]')).toBeNull();
  });
});
