import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs';
import { AuthApi } from '../generated/api/auth.service';
import { LoginRequest } from '../generated/model/loginRequest';
import { LoginResponse } from '../generated/model/loginResponse';
import { ResetPasswordRequest } from '../generated/model/resetPasswordRequest';

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

export type AuthResponse = LoginResponse;
