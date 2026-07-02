import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ProjectApi } from '../generated/api/project.service';
import { WorkflowApi } from '../generated/api/workflow.service';
import { ProjectStatusResponse } from '../generated/model/projectStatusResponse';
import { StatusResponse } from '../generated/model/statusResponse';

import { asLoadedArray, Loaded } from '../core/required-types';

export type ProjectStatus = Loaded<ProjectStatusResponse>;
export type Status = Loaded<StatusResponse>;

@Injectable({
  providedIn: 'root'
})
export class StatusService {
  private readonly projectApi = inject(ProjectApi);
  private readonly workflowApi = inject(WorkflowApi);

  findProjectsStatuses(projectId: number): Observable<ProjectStatus[]> {
    return this.projectApi.listProjectStatuses(projectId).pipe(map(asLoadedArray));
  }

  findAll(): Observable<Status[]> {
    return this.workflowApi.listStatuses().pipe(map(asLoadedArray));
  }
}
