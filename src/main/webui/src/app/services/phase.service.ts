import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type PhaseStatus = 'PLANNED' | 'ACTIVE' | 'COMPLETED';

export interface PhaseDeliverable {
  id: number;
  sortOrder: number;
  text: string;
}

export interface Phase {
  id: number;
  projectId: number;
  name: string;
  objective?: string | null;
  status: PhaseStatus;
  startDate?: string | null;
  endDate?: string | null;
  deliverableVersionId?: number | null;
  deliverableVersionLabel?: string | null;
  createdAt: string;
  completedAt?: string | null;
  deliverables: PhaseDeliverable[];
}

export interface CreatePhaseRequest {
  name: string;
  objective?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  deliverableVersionId?: number | null;
  deliverables?: string[];
}

export interface UpdatePhaseRequest extends CreatePhaseRequest {}

@Injectable({
  providedIn: 'root'
})
export class PhaseService {
  private readonly http = inject(HttpClient);

  list(projectId: number): Observable<Phase[]> {
    return this.http.get<Phase[]>(`/api/projects/${projectId}/phases`);
  }

  findById(projectId: number, phaseId: number): Observable<Phase> {
    return this.http.get<Phase>(`/api/projects/${projectId}/phases/${phaseId}`);
  }

  findActive(projectId: number): Observable<Phase> {
    return this.http.get<Phase>(`/api/projects/${projectId}/phases/active`);
  }

  create(projectId: number, request: CreatePhaseRequest): Observable<Phase> {
    return this.http.post<Phase>(`/api/projects/${projectId}/phases`, request);
  }

  update(projectId: number, phaseId: number, request: UpdatePhaseRequest): Observable<Phase> {
    return this.http.post<Phase>(`/api/projects/${projectId}/phases/${phaseId}`, request);
  }

  activate(projectId: number, phaseId: number): Observable<Phase> {
    return this.http.post<Phase>(`/api/projects/${projectId}/phases/${phaseId}/activate`, null);
  }

  complete(projectId: number, phaseId: number): Observable<Phase> {
    return this.http.post<Phase>(`/api/projects/${projectId}/phases/${phaseId}/complete`, null);
  }
}
