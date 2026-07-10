import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
      imports: [LoginComponent],
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
});
