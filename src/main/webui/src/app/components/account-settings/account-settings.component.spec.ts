import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { AccountSettingsComponent } from './account-settings.component';

describe('AccountSettingsComponent', () => {
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  async function setup(changePassword: boolean): Promise<void> {
    authService = jasmine.createSpyObj('AuthService', ['me', 'getCapabilities', 'changePassword', 'updateProfile']);
    authService.getCapabilities.and.returnValue(of({
      provider: changePassword ? 'local' : 'endpoint',
      passwordRecovery: changePassword,
      changePassword,
    }));
    authService.me.and.returnValue(of({
      id: 1,
      username: 'user',
      name: 'User',
      email: 'user@issues.vepo.dev',
      roles: new Set(['user']),
    } as any));

    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSettingsComponent);
    fixture.detectChanges();
  }

  it('should show password section when changePassword is true', async () => {
    await setup(true);
    expect(fixture.nativeElement.textContent).toContain('Alterar senha');
    expect(fixture.debugElement.query(By.css('[formControlName=currentPassword]'))).toBeTruthy();
  });

  it('should hide password section when changePassword is false', async () => {
    await setup(false);
    expect(fixture.debugElement.query(By.css('[formControlName=currentPassword]'))).toBeNull();
  });
});
