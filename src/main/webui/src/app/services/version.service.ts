import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Version {
  id: number;
  projectId: number;
  label: string;
  description?: string | null;
}

export interface CreateVersionRequest {
  label: string;
  description?: string | null;
}

export interface UpdateVersionRequest {
  label: string;
  description?: string | null;
}

export interface VersionChangelogEntry {
  ticketId: number;
  identifier: string;
  title: string;
  statusName: string;
  priority: string;
  finishedAt?: string | null;
  associations: string[];
}

export interface VersionChangelogSection {
  name: string;
  tickets: VersionChangelogEntry[];
}

export interface VersionChangelog {
  versionId: number;
  label: string;
  description?: string | null;
  sections: VersionChangelogSection[];
}

@Injectable({
  providedIn: 'root'
})
export class VersionService {
  private readonly http = inject(HttpClient);

  list(projectId: number): Observable<Version[]> {
    return this.http.get<Version[]>(`/api/projects/${projectId}/versions`);
  }

  findById(projectId: number, versionId: number): Observable<Version> {
    return this.http.get<Version>(`/api/projects/${projectId}/versions/${versionId}`);
  }

  create(projectId: number, request: CreateVersionRequest): Observable<Version> {
    return this.http.post<Version>(`/api/projects/${projectId}/versions`, request);
  }

  update(projectId: number, versionId: number, request: UpdateVersionRequest): Observable<Version> {
    return this.http.post<Version>(`/api/projects/${projectId}/versions/${versionId}`, request);
  }

  changelog(projectId: number, versionId: number): Observable<VersionChangelog> {
    return this.http.get<VersionChangelog>(`/api/projects/${projectId}/versions/${versionId}/changelog`);
  }
}
