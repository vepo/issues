import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { WorkflowApi } from '../generated/api/workflow.service';
import { CreateWorkflowRequest } from '../generated/model/createWorkflowRequest';
import { UpdateWorkflowRequest } from '../generated/model/updateWorkflowRequest';
import { WorkflowResponse } from '../generated/model/workflowResponse';

import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type Workflow = Loaded<WorkflowResponse>;
export type { CreateWorkflowRequest, UpdateWorkflowRequest };

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  private readonly api = inject(WorkflowApi);

  findAll(): Observable<Workflow[]> {
    return this.api.listWorkflows().pipe(map(asLoadedArray));
  }

  findById(workflowId: number): Observable<Workflow> {
    return this.api.findWorkflowById(workflowId).pipe(map(asLoaded));
  }

  create(request: CreateWorkflowRequest): Observable<Workflow> {
    return this.api.createWorkflow(request).pipe(map(asLoaded));
  }

  update(workflowId: number, request: UpdateWorkflowRequest): Observable<Workflow> {
    return this.api.updateWorkflow(workflowId, request).pipe(map(asLoaded));
  }
}
