import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { ProjectsService } from './projects.service';

/**
 * Allows authenticated users always. Anonymous users may proceed when the
 * route's project is PUBLIC (resolved via GET /projects/{id}).
 */
export const publicReadGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  const projectIdParam = route.paramMap.get('projectId');
  if (!projectIdParam) {
    // Ticket deep links without projectId — allow; API enforces Public-only.
    return true;
  }

  const projectId = Number(projectIdParam);
  if (Number.isNaN(projectId)) {
    void router.navigate(['/login']);
    return false;
  }

  return inject(ProjectsService).findById(projectId).pipe(
    map(() => true),
    catchError(() => {
      void router.navigate(['/login']);
      return of(false);
    }),
  );
};
