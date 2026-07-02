import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { WorkflowApi } from '../generated/api/workflow.service';
import { WorkflowResponse } from '../generated/model/workflowResponse';

import { asLoadedArray, Loaded } from '../core/required-types';

export type Workflow = Loaded<WorkflowResponse>;

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  private readonly api = inject(WorkflowApi);

  findAll(): Observable<Workflow[]> {
    return this.api.listWorkflows().pipe(map(asLoadedArray));
  }

  findById(workflowId: number): Observable<Workflow | undefined> {
    return this.findAll().pipe(map(workflows => workflows.find(w => w.id === workflowId)));
  }
}
