import { inject } from '@angular/core';
import { RedirectCommand, ResolveFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Workflow, WorkflowService } from '../services/workflow.service';

export const workflowResolver: ResolveFn<Workflow> = (route) => {
  const workflowId = route.paramMap.get('workflowId');
  if (!workflowId) {
    return new RedirectCommand(inject(Router).parseUrl('/'));
  }
  return inject(WorkflowService).findById(Number(workflowId)).pipe(
    map(workflow => workflow ?? new RedirectCommand(inject(Router).parseUrl('/')))
  );
};

export const workflowsResolver: ResolveFn<Workflow[]> = () => {
  return inject(WorkflowService).findAll();
};
