import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { PasswordResetRequestComponent } from './password-reset-request.component';

describe('PasswordResetRequestComponent', () => {
  let component: PasswordResetRequestComponent;
  let fixture: ComponentFixture<PasswordResetRequestComponent>;

  beforeEach(async () => {
    const authService = jasmine.createSpyObj('AuthService', ['recoverPassword']);
    authService.recoverPassword.and.returnValue(of({}));
    const toastService = jasmine.createSpyObj('ToastService', ['success']);

    await TestBed.configureTestingModule({
      imports: [PasswordResetRequestComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordResetRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
