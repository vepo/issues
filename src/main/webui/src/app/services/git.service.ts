import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';

export type GitProvider = 'GITHUB' | 'GITLAB' | 'GITEA' | 'OTHER';

export interface ProjectGitRepositoryRequest {
  remoteUrl: string;
  provider: GitProvider;
  defaultBranch?: string;
}

export interface ProjectGitRepository {
  projectId: number;
  remoteUrl: string;
  provider: GitProvider;
  defaultBranch?: string;
  webhookUrl: string;
  hasSecret: boolean;
  webhookSecret?: string;
}

export interface LinkedCommit {
  id?: number;
  sha?: string;
  message?: string;
  authorName?: string;
  authorEmail?: string;
  matchedUserId?: number;
  matchedUserName?: string;
  committedAt?: string;
  commitUrl?: string;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root',
})
export class GitService {
  private readonly http = inject(HttpClient);

  get(projectId: number): Observable<ProjectGitRepository | null> {
    return this.http.get<ProjectGitRepository>(`/api/projects/${projectId}/git`).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  put(projectId: number, request: ProjectGitRepositoryRequest): Observable<ProjectGitRepository> {
    return this.http.put<ProjectGitRepository>(`/api/projects/${projectId}/git`, request);
  }

  regenerateSecret(projectId: number): Observable<ProjectGitRepository> {
    return this.http.post<ProjectGitRepository>(`/api/projects/${projectId}/git/regenerate-secret`, {});
  }
}
