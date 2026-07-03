import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Ticket } from './ticket.service';

export interface SavedQuery {
  id: number;
  slug: string;
  name: string;
  query: string;
  showAtHome: boolean;
  ownerId: number;
  ownerName: string;
  createdAt: string;
  updatedAt: string;
}

export interface SavedQueryWithResults {
  savedQuery: SavedQuery;
  tickets: Ticket[];
}

export interface HomeSavedQuerySection {
  savedQuery: SavedQuery;
  tickets: Ticket[];
}

export interface CreateSavedQueryRequest {
  name: string;
  query: string;
  showAtHome: boolean;
}

export interface UpdateSavedQueryRequest {
  name: string;
  query: string;
  showAtHome: boolean;
}

export interface SearchTicketsByQueryRequest {
  query: string;
}

@Injectable({
  providedIn: 'root'
})
export class SavedQueryService {
  private readonly http = inject(HttpClient);

  searchByQuery(query: string): Observable<Ticket[]> {
    return this.http.post<Ticket[]>('/api/tickets/search/query', { query });
  }

  list(): Observable<SavedQuery[]> {
    return this.http.get<SavedQuery[]>('/api/saved-queries');
  }

  create(request: CreateSavedQueryRequest): Observable<SavedQuery> {
    return this.http.post<SavedQuery>('/api/saved-queries', request);
  }

  findBySlug(slug: string): Observable<SavedQueryWithResults> {
    return this.http.get<SavedQueryWithResults>(`/api/saved-queries/by-slug/${slug}`);
  }

  update(id: number, request: UpdateSavedQueryRequest): Observable<SavedQuery> {
    return this.http.put<SavedQuery>(`/api/saved-queries/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/saved-queries/${id}`);
  }

  clone(id: number): Observable<SavedQuery> {
    return this.http.post<SavedQuery>(`/api/saved-queries/${id}/clone`, {});
  }

  listHomeSections(): Observable<HomeSavedQuerySection[]> {
    return this.http.get<HomeSavedQuerySection[]>('/api/home/saved-queries');
  }
}
