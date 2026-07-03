import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const roleGuard = (roles: string[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const allowed = roles.some(role => authService.hasRole(role));
  if (!allowed) {
    return router.parseUrl('/');
  }
  return true;
};
