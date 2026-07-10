import { Injectable, inject } from '@angular/core';
import { catchError, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { AuthApi } from '../generated/api/auth.service';
import { AuthCapabilitiesResponse } from '../generated/model/authCapabilitiesResponse';
import { AuthResponse as GeneratedAuthResponse } from '../generated/model/authResponse';
import { ConfirmPasswordResetRequest } from '../generated/model/confirmPasswordResetRequest';
import { LoginRequest } from '../generated/model/loginRequest';
import { LoginResponse } from '../generated/model/loginResponse';
import { RefreshTokenRequest } from '../generated/model/refreshTokenRequest';
import { ResetPasswordRequest } from '../generated/model/resetPasswordRequest';
import { asLoaded, Loaded } from '../core/required-types';

export type CurrentUser = Loaded<GeneratedAuthResponse>;
export type AuthResponse = LoginResponse;
export type AuthCapabilities = Loaded<AuthCapabilitiesResponse>;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApi);
  private readonly tokenKey = 'jwt_token';
  private readonly refreshTokenKey = 'jwt_refresh_token';
  private capabilities$?: Observable<AuthCapabilities>;

  login(email: string, password: string) {
    return this.api.login({ email, password } as LoginRequest)
                   .pipe(tap(res => this.storeTokens(res)));
  }

  getCapabilities(): Observable<AuthCapabilities> {
    if (!this.capabilities$) {
      this.capabilities$ = this.api.getAuthCapabilities().pipe(
        map(asLoaded),
        catchError(() => of({
          provider: 'local',
          passwordRecovery: true,
          changePassword: true,
        } as AuthCapabilities)),
        shareReplay(1),
      );
    }
    return this.capabilities$;
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem(this.refreshTokenKey);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.api.refreshToken({ refreshToken } as RefreshTokenRequest)
                   .pipe(tap(res => this.storeTokens(res)));
  }

  recoverPassword(credential: string) {
    return this.api.resetPassword({ credential } as ResetPasswordRequest);
  }

  confirmPasswordReset(token: string, newPassword: string) {
    return this.api.confirmPasswordReset({ token, newPassword } as ConfirmPasswordResetRequest);
  }

  register(username: string, name: string, email: string, password: string) {
    return this.api.registerUser({ username, name, email, password });
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.api.changePassword({ currentPassword, newPassword });
  }

  updateProfile(name: string, email: string) {
    return this.api.updateProfile({ name, email }).pipe(map(asLoaded));
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
    localStorage.removeItem(this.refreshTokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private storeTokens(response: LoginResponse) {
    if (response.token) {
      this.saveToken(response.token);
    }
    if (response.refreshToken) {
      localStorage.setItem(this.refreshTokenKey, response.refreshToken);
    }
  }
}
