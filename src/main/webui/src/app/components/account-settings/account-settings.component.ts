import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatError } from '@angular/material/form-field';
import { AuthService, CurrentUser } from '../../services/auth.service';

@Component({
  selector: 'app-account-settings',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatError,
    MatButtonModule,
    MatIconModule,
    RouterLink
  ],
  templateUrl: './account-settings.component.html'
})
export class AccountSettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  user: CurrentUser | null = null;
  loading = true;
  error = '';
  profileMessage = '';
  profileError = '';
  passwordMessage = '';
  passwordError = '';
  isSavingProfile = false;
  isSavingPassword = false;

  profileForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]]
  });

  passwordForm: FormGroup = this.formBuilder.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  ngOnInit(): void {
    this.authService.me().subscribe({
      next: (user: CurrentUser) => {
        this.user = user;
        this.profileForm.patchValue({
          name: user.name,
          email: user.email
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar seu perfil.';
        this.loading = false;
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }
    this.isSavingProfile = true;
    this.profileMessage = '';
    this.profileError = '';
    const { name, email } = this.profileForm.value;
    this.authService.updateProfile(name, email).subscribe({
      next: (user) => {
        this.user = user;
        this.isSavingProfile = false;
        this.profileMessage = 'Perfil atualizado com sucesso.';
      },
      error: () => {
        this.isSavingProfile = false;
        this.profileError = 'Não foi possível atualizar o perfil. Verifique o e-mail informado.';
      }
    });
  }

  passwordsMismatch(): boolean {
    const newPassword = this.passwordForm.get('newPassword')?.value;
    const confirmPassword = this.passwordForm.get('confirmPassword')?.value;
    return !!newPassword && !!confirmPassword && newPassword !== confirmPassword;
  }

  changePassword(): void {
    if (this.passwordForm.invalid || this.passwordsMismatch()) {
      return;
    }
    this.isSavingPassword = true;
    this.passwordMessage = '';
    this.passwordError = '';
    const { currentPassword, newPassword } = this.passwordForm.value;
    this.authService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.isSavingPassword = false;
        this.passwordMessage = 'Senha alterada com sucesso.';
        this.passwordForm.reset();
      },
      error: () => {
        this.isSavingPassword = false;
        this.passwordError = 'Não foi possível alterar a senha. Verifique a senha atual.';
      }
    });
  }
}
