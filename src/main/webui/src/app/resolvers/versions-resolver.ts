import { inject } from '@angular/core';
import { RedirectCommand, ResolveFn, Router } from '@angular/router';
import { Version, VersionService } from '../services/version.service';

export const versionsResolver: ResolveFn<Version[]> = (route) => {
  const projectId = route.paramMap.get('projectId');
  if (!projectId) {
    return new RedirectCommand(inject(Router).parseUrl('/'));
  }
  return inject(VersionService).list(Number(projectId));
};

export const versionResolver: ResolveFn<Version> = (route) => {
  const projectId = route.paramMap.get('projectId');
  const versionId = route.paramMap.get('versionId');
  if (!projectId || !versionId) {
    return new RedirectCommand(inject(Router).parseUrl('/'));
  }
  return inject(VersionService).findById(Number(projectId), Number(versionId));
};
