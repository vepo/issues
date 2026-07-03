import { inject } from '@angular/core';
import { RedirectCommand, ResolveFn, Router } from '@angular/router';
import { Phase, PhaseService } from '../services/phase.service';

export const phasesResolver: ResolveFn<Phase[]> = (route) => {
  const projectId = route.paramMap.get('projectId');
  if (!projectId) {
    return new RedirectCommand(inject(Router).parseUrl('/'));
  }
  return inject(PhaseService).list(Number(projectId));
};

export const phaseResolver: ResolveFn<Phase> = (route) => {
  const projectId = route.paramMap.get('projectId');
  const phaseId = route.paramMap.get('phaseId');
  if (!projectId || !phaseId) {
    return new RedirectCommand(inject(Router).parseUrl('/'));
  }
  return inject(PhaseService).findById(Number(projectId), Number(phaseId));
};
