import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { BacklogApi } from '../generated/api/backlog.service';
import { BacklogPageResponse } from '../generated/model/backlogPageResponse';
import { BacklogTicketResponse } from '../generated/model/backlogTicketResponse';
import { ReorderBacklogRequest } from '../generated/model/reorderBacklogRequest';
import { asLoaded, Loaded } from '../core/required-types';

export type BacklogTicket = Loaded<BacklogTicketResponse>;
export type BacklogPage = Loaded<BacklogPageResponse>;
export type { ReorderBacklogRequest };

@Injectable({
  providedIn: 'root'
})
export class BacklogService {
  private readonly api = inject(BacklogApi);

  list(projectId: number, page = 0, size = 20): Observable<BacklogPage> {
    return this.api.listProjectBacklog(projectId, page, size).pipe(map(asLoaded));
  }

  reorder(projectId: number, request: ReorderBacklogRequest): Observable<BacklogTicket> {
    return this.api.reorderProjectBacklog(projectId, request).pipe(map(asLoaded));
  }
}
