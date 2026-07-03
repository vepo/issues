import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ProjectApi } from '../generated/api/project.service';
import { CreateProjectRequest } from '../generated/model/createProjectRequest';
import { ProjectResponse } from '../generated/model/projectResponse';
import { WorkflowResponse } from '../generated/model/workflowResponse';

import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type Project = Loaded<ProjectResponse>;
export type CreateOrUpdateProjectRequest = CreateProjectRequest & { ownerId?: number };
export type ProjectWorkflow = Loaded<WorkflowResponse>;

@Injectable({
  providedIn: 'root'
})
export class ProjectsService {
  private readonly api = inject(ProjectApi);

  findById(projectId: number): Observable<Project> {
    return this.api.findProjectById(projectId).pipe(map(asLoaded));
  }

  findAll(): Observable<Project[]> {
    return this.api.listProjects().pipe(map(asLoadedArray));
  }

  create(request: CreateOrUpdateProjectRequest): Observable<Project> {
    return this.api.createProject(request).pipe(map(asLoaded));
  }

  update(projectId: number, request: CreateOrUpdateProjectRequest): Observable<Project> {
    return this.api.updateProject(projectId, request).pipe(map(asLoaded));
  }

  findWorkflowByProjectId(projectId: number): Observable<ProjectWorkflow> {
    return this.api.findProjectWorkflow(projectId).pipe(map(asLoaded));
  }
}
