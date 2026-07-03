import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface HomeTicket {
  id: number;
  identifier: string;
  title: string;
  projectId: number;
  projectName: string;
  status: string;
  priority: string;
  updatedAt: string;
}

export interface HomeActivity {
  type: 'COMMENT' | 'STATUS_CHANGED';
  ticketId: number;
  ticketIdentifier: string;
  ticketTitle: string;
  projectName: string;
  actorName: string;
  summary: string;
  occurredAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class HomeService {
  private readonly http = inject(HttpClient);

  listCurrentTickets(): Observable<HomeTicket[]> {
    return this.http.get<HomeTicket[]>('/api/home/tickets/current');
  }

  listAssignedTickets(): Observable<HomeTicket[]> {
    return this.http.get<HomeTicket[]>('/api/home/tickets/assigned');
  }

  listActivity(): Observable<HomeActivity[]> {
    return this.http.get<HomeActivity[]>('/api/home/activity');
  }
}
