import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule, MatError } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { passwordsMatchValidator, strongPasswordValidators } from '../../core/password-policy';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatError,
    MatButtonModule,
    MatIconModule,
    RouterLink,
  ],
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  hide = signal(true);
  hideConfirm = signal(true);
  error = '';
  saving = false;

  form = this.formBuilder.group(
    {
      username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(15)]],
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', strongPasswordValidators],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatchValidator('password', 'confirmPassword') }
  );

  toggleVisibility(event: MouseEvent, field: 'password' | 'confirm') {
    if (field === 'password') {
      this.hide.set(!this.hide());
    } else {
      this.hideConfirm.set(!this.hideConfirm());
    }
    event.stopPropagation();
  }

  passwordsMismatch(): boolean {
    return !!this.form.hasError('passwordMismatch')
      && !!this.form.get('password')?.value
      && !!this.form.get('confirmPassword')?.value;
  }

  register() {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.error = '';
    const { username, name, email, password } = this.form.getRawValue();
    this.auth.register(username!, name!, email!, password!).subscribe({
      next: async () => {
        this.saving = false;
        await this.router.navigate(['/login']);
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'Não foi possível criar a conta.';
      },
    });
  }
}
