import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { TicketApi } from '../generated/api/ticket.service';
import { ProjectApi } from '../generated/api/project.service';
import { ChildrenSummaryResponse } from '../generated/model/childrenSummaryResponse';
import { CommentRequest } from '../generated/model/commentRequest';
import { CommentResponse } from '../generated/model/commentResponse';
import { CreateChildTicketRequest } from '../generated/model/createChildTicketRequest';
import { CreateTicketLinkRequest } from '../generated/model/createTicketLinkRequest';
import { CreateTicketRequest } from '../generated/model/createTicketRequest';
import { MoveTicketRequest } from '../generated/model/moveTicketRequest';
import { SubscribeTicketRequest } from '../generated/model/subscribeTicketRequest';
import { TicketLinkResponse } from '../generated/model/ticketLinkResponse';
import { TicketLinkType } from '../generated/model/ticketLinkType';
import { TicketType } from '../generated/model/ticketType';
import { UpdateAssigneeRequest } from '../generated/model/updateAssigneeRequest';
import { UpdateTicketRequest } from '../generated/model/updateTicketRequest';
import { TicketExpandedResponse } from '../generated/model/ticketExpandedResponse';
import { TicketHistoryResponse } from '../generated/model/ticketHistoryResponse';
import { TicketResponse } from '../generated/model/ticketResponse';
import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type Ticket = Loaded<TicketResponse>;
export type TicketExpanded = Loaded<TicketExpandedResponse>;
export type Comment = Loaded<CommentResponse>;
export type TicketHistory = Loaded<TicketHistoryResponse>;
export type TicketLink = Loaded<TicketLinkResponse>;
export type ChildrenSummary = Loaded<ChildrenSummaryResponse>;
export type CreateCommentRequest = CommentRequest;
export type { CreateTicketRequest, UpdateTicketRequest, CreateTicketLinkRequest, CreateChildTicketRequest, TicketLinkType, TicketType };

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private readonly api = inject(TicketApi);
  private readonly projectApi = inject(ProjectApi);

  findByProjectId(projectId: number): Observable<Ticket[]> {
    return this.projectApi.listProjectTickets(projectId).pipe(map(asLoadedArray));
  }

  findById(ticketId: number): Observable<Ticket> {
    return this.api.findTicketById(ticketId).pipe(map(asLoaded));
  }

  findExpandedById(ticketId: number): Observable<TicketExpanded> {
    return this.api.findExpandedTicketByIdentifier(String(ticketId)).pipe(map(asLoaded));
  }

  findExpandedByIdentifier(ticketIdentifier: string): Observable<TicketExpanded> {
    return this.api.findExpandedTicketByIdentifier(ticketIdentifier).pipe(map(asLoaded));
  }

  search(term: string, status: number): Observable<Ticket[]> {
    return this.api.searchTickets(status, term).pipe(map(asLoadedArray));
  }

  move(ticketId: number, toStatus: number): Observable<Ticket> {
    return this.api.moveTicket(ticketId, { to: toStatus } as MoveTicketRequest).pipe(map(asLoaded));
  }

  createTicket(request: CreateTicketRequest): Observable<Ticket> {
    return this.api.createTicket(request).pipe(map(asLoaded));
  }

  update(ticketId: number, request: UpdateTicketRequest): Observable<Ticket> {
    return this.api.updateTicket(ticketId, request).pipe(map(asLoaded));
  }

  updateAssignee(ticketId: number, assigneeId: number): Observable<Ticket> {
    return this.api.updateAssignee(ticketId, { assigneeId } as UpdateAssigneeRequest).pipe(map(asLoaded));
  }

  delete(ticketId: number): Observable<unknown> {
    return this.api.deleteTicket(ticketId);
  }

  restore(ticketId: number): Observable<Ticket> {
    return this.api.restoreTicket(ticketId).pipe(map(asLoaded));
  }

  getTicket(id: string): Observable<Ticket> {
    return this.api.findTicketById(Number(id)).pipe(map(asLoaded));
  }

  getTicketHistory(id: string): Observable<TicketHistory[]> {
    return this.api.getTicketHistory(Number(id)).pipe(map(asLoadedArray));
  }

  getComments(ticketId: number): Observable<Comment[]> {
    return this.api.listComments(ticketId).pipe(map(asLoadedArray));
  }

  addComment(ticketId: number, request: CreateCommentRequest): Observable<Comment> {
    return this.api.addComment(ticketId, request).pipe(map(asLoaded));
  }

  addSubscription(ticketId: number, userId: number): Observable<TicketExpanded> {
    return this.api.subscribeTicket(ticketId, { subscriberId: userId } as SubscribeTicketRequest).pipe(map(asLoaded));
  }

  removeSubscription(ticketId: number, userId: number): Observable<TicketExpanded> {
    return this.api.unsubscribeTicket(ticketId, userId).pipe(map(asLoaded));
  }

  listLinks(ticketId: number): Observable<TicketLink[]> {
    return this.api.listTicketLinks(ticketId).pipe(map(asLoadedArray));
  }

  createLink(ticketId: number, request: CreateTicketLinkRequest): Observable<TicketLink> {
    return this.api.createTicketLink(ticketId, request).pipe(map(asLoaded));
  }

  deleteLink(ticketId: number, linkId: number): Observable<unknown> {
    return this.api.deleteTicketLink(ticketId, linkId);
  }

  createChild(ticketId: number, request: CreateChildTicketRequest): Observable<Ticket> {
    return this.api.createChildTicket(ticketId, request).pipe(map(asLoaded));
  }
}
