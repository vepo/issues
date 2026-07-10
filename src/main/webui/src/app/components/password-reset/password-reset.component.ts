import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { strongPasswordValidators } from '../../core/password-policy';

@Component({
  selector: 'app-password-reset',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    RouterLink
  ],
  templateUrl: './password-reset.component.html',
  styleUrl: './password-reset.component.scss'
})
export class PasswordResetComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly formBuilder = inject(FormBuilder);

  hide = signal(true);
  hideConfirm = signal(true);
  isLoading = false;
  error = '';

  resetForm: FormGroup = this.formBuilder.group({
    newPassword: ['', strongPasswordValidators],
    confirmPassword: ['', [Validators.required]]
  });

  constructor() {
    this.resetForm.addValidators(() => {
      const password = this.resetForm.get('newPassword')?.value;
      const confirm = this.resetForm.get('confirmPassword')?.value;
      return password === confirm ? null : { passwordMismatch: true };
    });
  }

  toggleVisibility(event: MouseEvent, field: 'new' | 'confirm') {
    if (field === 'new') {
      this.hide.set(!this.hide());
    } else {
      this.hideConfirm.set(!this.hideConfirm());
    }
    event.stopPropagation();
  }

  confirmReset() {
    if (this.resetForm.invalid) {
      return;
    }
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error = 'Link de redefinição inválido.';
      return;
    }

    this.isLoading = true;
    this.error = '';
    this.auth.confirmPasswordReset(token, this.resetForm.value.newPassword).subscribe({
      next: async () => {
        this.isLoading = false;
        this.toastService.success('Senha redefinida com sucesso. Faça login com a nova senha.');
        await this.router.navigate(['/login']);
      },
      error: () => {
        this.isLoading = false;
        this.error = 'Link inválido ou expirado. Solicite uma nova recuperação de senha.';
      }
    });
  }
}
