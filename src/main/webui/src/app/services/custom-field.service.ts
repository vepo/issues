import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ProjectApi } from '../generated/api/project.service';
import { WorkflowApi } from '../generated/api/workflow.service';
import { CustomFieldRequest } from '../generated/model/customFieldRequest';
import { CustomFieldResponse } from '../generated/model/customFieldResponse';
import { CustomFieldType } from '../generated/model/customFieldType';
import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type CustomField = Loaded<CustomFieldResponse>;
export type { CustomFieldRequest, CustomFieldType };

@Injectable({
  providedIn: 'root'
})
export class CustomFieldService {
  private readonly projectApi = inject(ProjectApi);
  private readonly workflowApi = inject(WorkflowApi);

  listProjectFields(projectId: number): Observable<CustomField[]> {
    return this.projectApi.listProjectCustomFields(projectId).pipe(map(asLoadedArray));
  }

  listInScope(projectId: number): Observable<CustomField[]> {
    return this.projectApi.listInScopeCustomFields(projectId).pipe(map(asLoadedArray));
  }

  createProjectField(projectId: number, request: CustomFieldRequest): Observable<CustomField> {
    return this.projectApi.createProjectCustomField(projectId, request).pipe(map(asLoaded));
  }

  updateProjectField(projectId: number, fieldId: number, request: CustomFieldRequest): Observable<CustomField> {
    return this.projectApi.updateProjectCustomField(fieldId, projectId, request).pipe(map(asLoaded));
  }

  deleteProjectField(projectId: number, fieldId: number): Observable<unknown> {
    return this.projectApi.deleteProjectCustomField(fieldId, projectId);
  }

  listWorkflowFields(workflowId: number): Observable<CustomField[]> {
    return this.workflowApi.listWorkflowCustomFields(workflowId).pipe(map(asLoadedArray));
  }

  createWorkflowField(workflowId: number, request: CustomFieldRequest): Observable<CustomField> {
    return this.workflowApi.createWorkflowCustomField(workflowId, request).pipe(map(asLoaded));
  }

  updateWorkflowField(workflowId: number, fieldId: number, request: CustomFieldRequest): Observable<CustomField> {
    return this.workflowApi.updateWorkflowCustomField(fieldId, workflowId, request).pipe(map(asLoaded));
  }

  deleteWorkflowField(workflowId: number, fieldId: number): Observable<unknown> {
    return this.workflowApi.deleteWorkflowCustomField(fieldId, workflowId);
  }
}
