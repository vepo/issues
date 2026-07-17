import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Ticket } from './ticket.service';

export interface ProjectMember {
  id: number;
  username: string;
  name: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProjectMembersService {
  private readonly http = inject(HttpClient);

  listMembers(projectId: number): Observable<ProjectMember[]> {
    return this.http.get<ProjectMember[]>(`/api/projects/${projectId}/members`);
  }

  addMember(projectId: number, userId: number): Observable<ProjectMember> {
    return this.http.post<ProjectMember>(`/api/projects/${projectId}/members`, { userId });
  }

  removeMember(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}/members/${userId}`);
  }

  listOpenAssignedTickets(projectId: number, userId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`/api/projects/${projectId}/members/${userId}/open-tickets`);
  }
}
