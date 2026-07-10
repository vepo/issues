import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    auth = jasmine.createSpyObj('AuthService', ['register']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: {}, paramMap: of({}) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reject weak passwords', () => {
    component.form.patchValue({
      username: 'newuser1',
      name: 'New User',
      email: 'new@example.com',
      password: 'secret12',
      confirmPassword: 'secret12',
    });
    expect(component.form.get('password')?.invalid).toBeTrue();
  });

  it('should register and navigate to login', () => {
    auth.register.and.returnValue(of({ id: 1, username: 'newuser1', name: 'New User', email: 'new@example.com', roles: ['user'] }) as any);
    component.form.patchValue({
      username: 'newuser1',
      name: 'New User',
      email: 'new@example.com',
      password: 'Secret123',
      confirmPassword: 'Secret123',
    });

    component.register();

    expect(auth.register).toHaveBeenCalledWith('newuser1', 'New User', 'new@example.com', 'Secret123');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should show error when register fails', () => {
    auth.register.and.returnValue(throwError(() => ({ error: { message: 'Email already in use' } })));
    component.form.patchValue({
      username: 'newuser1',
      name: 'New User',
      email: 'new@example.com',
      password: 'Secret123',
      confirmPassword: 'Secret123',
    });

    component.register();

    expect(component.error).toBe('Email already in use');
  });
});
