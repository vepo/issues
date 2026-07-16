import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { ProjectsService } from './projects.service';

/**
 * Allows project owner or admin — mirrors hub {@code canManage} / backend {@code requireManage}.
 */
export const projectManageGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('admin')) {
    return true;
  }

  const projectIdParam = route.paramMap.get('projectId');
  if (!projectIdParam) {
    return router.parseUrl('/');
  }

  const projectId = Number(projectIdParam);
  if (Number.isNaN(projectId)) {
    return router.parseUrl('/');
  }

  let userId: number;
  try {
    userId = authService.getAuthUserId();
  } catch {
    return router.parseUrl('/');
  }

  return inject(ProjectsService).findById(projectId).pipe(
    map((project) => {
      const ownerId = project.owner?.id;
      if (ownerId != null && ownerId === userId) {
        return true;
      }
      return router.parseUrl('/');
    }),
    catchError(() => of(router.parseUrl('/'))),
  );
};
