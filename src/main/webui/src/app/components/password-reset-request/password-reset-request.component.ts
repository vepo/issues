import { CommonModule } from '@angular/common';
import { Component, inject, TemplateRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-password-reset-reset',
  templateUrl: './password-reset-request.component.html',
  styleUrl: './password-reset-request.component.scss',
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, RouterLink]
})
export class PasswordResetRequestComponent {
  passwordResetForm: FormGroup;
  isLoading = false;
  private formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toastService = inject(ToastService);
  @ViewChild('customToast') customToast!: TemplateRef<any>;

  constructor() {
    this.passwordResetForm = this.formBuilder.group({
      credential: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  recoverPassword() {
    if (this.passwordResetForm.valid) {
      this.isLoading = true;
      this.auth.recoverPassword(this.passwordResetForm.value.credential)
        .subscribe({
          next: (resp) => {
            console.log("Success!!", resp)
            this.isLoading = false;
            this.toastService.success("Recuperação de senha iniciada. Verifique seu email!", 15000);
          }, error: () => {
            this.isLoading = false;
            this.toastService.error('Não foi possível iniciar a recuperação. Tente novamente.');
          }
        })
    }
  }

  // Helper method for easy access to form fields
  get f() {
    return this.passwordResetForm.controls;
  }
}