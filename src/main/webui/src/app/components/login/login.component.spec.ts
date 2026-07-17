import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: jasmine.SpyObj<AuthService>;

  async function setup(passwordRecovery: boolean): Promise<void> {
    authService = jasmine.createSpyObj('AuthService', ['login', 'getCapabilities']);
    authService.getCapabilities.and.returnValue(of({
      provider: passwordRecovery ? 'local' : 'ldap',
      passwordRecovery,
      changePassword: passwordRecovery,
    }));
    authService.login.and.returnValue(of({ token: 't', refreshToken: 'r', expiresIn: 900 }));

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            pt: {
              auth: {
                email: 'E-mail',
                password: 'Senha',
                showPassword: 'Mostrar senha',
                login: {
                  title: 'Entrar',
                  subtitle: 'Sistema de gerenciamento de tickets',
                },
                register: { title: 'Criar conta' },
                reset: { requestTitle: 'Recuperar senha' },
              },
            },
            en: {
              auth: {
                email: 'Email',
                password: 'Password',
                showPassword: 'Show password',
                login: {
                  title: 'Sign in',
                  subtitle: 'Ticket management system',
                },
                register: { title: 'Create account' },
                reset: { requestTitle: 'Recover password' },
              },
            },
          },
          translocoConfig: {
            availableLangs: ['pt', 'en'],
            defaultLang: 'pt',
            reRenderOnLangChange: true,
          },
          preloadLangs: true,
        }),
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should show recovery and register when passwordRecovery is true', async () => {
    await setup(true);
    expect(component.passwordRecovery).toBeTrue();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Recuperar senha');
    expect(text).toContain('Criar conta');
  });

  it('should hide recovery and register when passwordRecovery is false', async () => {
    await setup(false);
    expect(component.passwordRecovery).toBeFalse();
    const text = fixture.nativeElement.textContent as string;
    expect(text).not.toContain('Recuperar senha');
    expect(text).not.toContain('Criar conta');
  });

  it('should rerender login content from Portuguese to English in place', async () => {
    await setup(true);
    const transloco = TestBed.inject(TranslocoService);
    const currentPath = window.location.pathname;
    expect(fixture.nativeElement.textContent).toContain('Entrar');
    expect(fixture.nativeElement.textContent).toContain('Sistema de gerenciamento de tickets');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sign in');
    expect(fixture.nativeElement.textContent).toContain('Ticket management system');
    expect(fixture.nativeElement.textContent).not.toContain('Sistema de gerenciamento de tickets');
    expect(window.location.pathname).toBe(currentPath);
  });
});
