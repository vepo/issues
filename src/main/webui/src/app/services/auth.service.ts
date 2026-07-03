import { Injectable, inject } from '@angular/core';
import { map, tap } from 'rxjs';
import { AuthApi } from '../generated/api/auth.service';
import { AuthResponse as GeneratedAuthResponse } from '../generated/model/authResponse';
import { ConfirmPasswordResetRequest } from '../generated/model/confirmPasswordResetRequest';
import { LoginRequest } from '../generated/model/loginRequest';
import { LoginResponse } from '../generated/model/loginResponse';
import { ResetPasswordRequest } from '../generated/model/resetPasswordRequest';
import { asLoaded, Loaded } from '../core/required-types';

export type CurrentUser = Loaded<GeneratedAuthResponse>;
export type AuthResponse = LoginResponse;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApi);
  private readonly tokenKey = 'jwt_token';

  login(email: string, password: string) {
    return this.api.login({ email, password } as LoginRequest)
                   .pipe(tap(res => {
                     if (res.token) {
                       this.saveToken(res.token);
                     }
                   }));
  }

  recoverPassword(credential: string) {
    return this.api.resetPassword({ credential } as ResetPasswordRequest);
  }

  confirmPasswordReset(token: string, newPassword: string) {
    return this.api.confirmPasswordReset({ token, newPassword } as ConfirmPasswordResetRequest);
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.api.changePassword({ currentPassword, newPassword });
  }

  me() {
    return this.api.me().pipe(map(asLoaded));
  }

  saveToken(token: string) {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getAuthUserId(): number {
    const token = this.getToken();
    if (!token) {
      throw new Error('Invalid token!');
    }
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.id) {
      throw new Error('Invalid token!');
    }
    return payload.id;
  }

  getRoles(): string[] {
    const token = this.getToken();
    if (!token) {
      return [];
    }
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.groups || [];
  }

  getEmail(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.email ?? null;
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
