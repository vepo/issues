import { TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { AppComponent } from './app';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';
import { ProjectsService } from './services/projects.service';

describe('App', () => {
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>;
  let authService: jasmine.SpyObj<AuthService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let projectsService: jasmine.SpyObj<ProjectsService>;

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

    notificationService = jasmine.createSpyObj('NotificationService', [
      'connect',
      'disconnect',
      'listen',
      'reconnected',
      'unreadCount',
      'list',
      'markAllAsRead',
      'markAsRead',
    ]);
    notificationService.listen.and.returnValue(EMPTY);
    notificationService.reconnected.and.returnValue(EMPTY);
    notificationService.unreadCount.and.returnValue(of({ unread: 0 } as any));
    notificationService.list.and.returnValue(of({ items: [], page: 0, size: 20, hasMore: false } as any));
    notificationService.markAllAsRead.and.returnValue(of({ updated: 0, unread: 0 } as any));
    notificationService.markAsRead.and.returnValue(of({ id: 1, read: true } as any));

    projectsService = jasmine.createSpyObj('ProjectsService', ['findAll']);
    projectsService.findAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService },
        { provide: ProjectsService, useValue: projectsService }
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

  it('should show search, project menu, and user menu when authenticated', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getEmail.and.returnValue('user@issues.vepo.dev');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.search-bar')).toBeTruthy();
    expect(compiled.querySelector('.search-bar__action')).toBeNull();
    expect(compiled.querySelector('#header-status')).toBeNull();
    expect(compiled.querySelector('app-context-bar')).toBeTruthy();
    expect(compiled.querySelector('app-project-menu')).toBeTruthy();
    expect(compiled.querySelector('[aria-label="Menu do usuário"]')).toBeTruthy();
    expect(compiled.querySelector('[aria-label="Abrir menu"]')).toBeNull();
  });

  it('should expose Conta Projetos and Administração for admin without project-manager', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getEmail.and.returnValue('tech_lead@issues.ui');
    authService.hasRole.and.callFake((role: string) => role === 'admin');

    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance.hasAdminMenu()).toBeTrue();
  });

  it('should expose Administração including Processos for project-manager', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getEmail.and.returnValue('pm@issues.vepo.dev');
    authService.hasRole.and.callFake((role: string) => role === 'project-manager');

    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance.hasAdminMenu()).toBeTrue();
  });
});
