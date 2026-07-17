import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TicketExportService } from './ticket-export.service';

describe('TicketExportService', () => {
  let service: TicketExportService;
  let http: HttpTestingController;
  let createObjectUrl: jasmine.Spy;
  let revokeObjectUrl: jasmine.Spy;
  let clickAnchor: jasmine.Spy;
  let appendAnchor: jasmine.Spy;
  let removeAnchor: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TicketExportService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TicketExportService);
    http = TestBed.inject(HttpTestingController);
    createObjectUrl = spyOn(URL, 'createObjectURL').and.returnValue('blob:ticket-export');
    revokeObjectUrl = spyOn(URL, 'revokeObjectURL');
    clickAnchor = spyOn(HTMLAnchorElement.prototype, 'click');
    appendAnchor = spyOn(document.body, 'appendChild').and.callThrough();
    removeAnchor = spyOn(document.body, 'removeChild').and.callThrough();
  });

  afterEach(() => {
    http.verify();
  });

  it('should export simple search criteria as CSV and honor the UTF-8 server filename', () => {
    const blob = new Blob(['identifier,title\nISS-1,Example'], { type: 'text/csv' });
    let completed = false;

    service.download({ source: 'simple', term: 'login', statusId: 7 }, 'CSV').subscribe({
      complete: () => {
        completed = true;
      },
    });

    const request = http.expectOne('/api/tickets/export');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      format: 'CSV',
      source: 'SIMPLE_SEARCH',
      term: 'login',
      statusId: 7,
    });
    expect(request.request.responseType).toBe('blob');
    request.flush(blob, {
      headers: {
        'Content-Disposition':
          'attachment; filename="tickets-ascii.csv"; filename*=UTF-8\'\'tickets-%C3%A7%C3%A3o.csv',
      },
    });

    const anchor = appendAnchor.calls.mostRecent().args[0] as HTMLAnchorElement;
    expect(anchor.href).toBe('blob:ticket-export');
    expect(anchor.download).toBe('tickets-ção.csv');
    expect(clickAnchor).toHaveBeenCalledTimes(1);
    expect(removeAnchor).toHaveBeenCalledOnceWith(anchor);
    expect(revokeObjectUrl).toHaveBeenCalledOnceWith('blob:ticket-export');
    expect(completed).toBeTrue();
  });

  it('should map advanced query criteria to a JSON export request', () => {
    service.download({ source: 'advanced', query: 'project = ISS ORDER BY priority DESC' }, 'JSON').subscribe();

    const request = http.expectOne('/api/tickets/export');
    expect(request.request.body).toEqual({
      format: 'JSON',
      source: 'ADVANCED_QUERY',
      query: 'project = ISS ORDER BY priority DESC',
    });
    request.flush(new Blob(['{"schemaVersion":1}'], { type: 'application/json' }), {
      headers: {
        'Content-Disposition': 'attachment; filename="tickets-query.json"',
      },
    });

    const anchor = appendAnchor.calls.mostRecent().args[0] as HTMLAnchorElement;
    expect(anchor.download).toBe('tickets-query.json');
  });

  it('should map a saved query slug and use the ASCII filename fallback', () => {
    service.download({ source: 'saved', savedQuerySlug: 'my-open-tickets' }, 'CSV').subscribe();

    const request = http.expectOne('/api/tickets/export');
    expect(request.request.body).toEqual({
      format: 'CSV',
      source: 'SAVED_QUERY',
      savedQuerySlug: 'my-open-tickets',
    });
    request.flush(new Blob(['identifier'], { type: 'text/csv' }), {
      headers: {
        'Content-Disposition': 'attachment; filename="tickets-2026-07-17.csv"',
      },
    });

    const anchor = appendAnchor.calls.mostRecent().args[0] as HTMLAnchorElement;
    expect(anchor.download).toBe('tickets-2026-07-17.csv');
  });

  it('should remove the temporary anchor and revoke the object URL when clicking fails', () => {
    const clickFailure = new Error('download click failed');
    let receivedError: unknown;
    clickAnchor.and.throwError(clickFailure);

    service.download({ source: 'advanced', query: 'status = OPEN' }, 'CSV').subscribe({
      error: (error: Error) => {
        receivedError = error;
      },
    });

    const request = http.expectOne('/api/tickets/export');
    request.flush(new Blob(['identifier'], { type: 'text/csv' }), {
      headers: {
        'Content-Disposition': 'attachment; filename="tickets.csv"',
      },
    });

    const anchor = appendAnchor.calls.mostRecent().args[0] as HTMLAnchorElement;
    expect(receivedError).toBe(clickFailure);
    expect(removeAnchor).toHaveBeenCalledOnceWith(anchor);
    expect(revokeObjectUrl).toHaveBeenCalledOnceWith('blob:ticket-export');
  });

  it('should expose server errors without creating a download resource', () => {
    let receivedStatus: number | undefined;

    service.download({ source: 'saved', savedQuerySlug: 'team-board' }, 'JSON').subscribe({
      error: (error: HttpErrorResponse) => {
        receivedStatus = error.status;
      },
    });

    const request = http.expectOne('/api/tickets/export');
    request.flush(
      new Blob([JSON.stringify({ message: 'Export limit exceeded' })], { type: 'application/json' }),
      { status: 413, statusText: 'Payload Too Large' },
    );

    expect(receivedStatus).toBe(413);
    expect(createObjectUrl).not.toHaveBeenCalled();
    expect(appendAnchor).not.toHaveBeenCalled();
    expect(revokeObjectUrl).not.toHaveBeenCalled();
  });
});
