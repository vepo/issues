import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable, tap } from 'rxjs';

export type TicketExportFormat = 'CSV' | 'JSON';

export type TicketExportCriteria =
  | Readonly<{ source: 'simple'; term: string; statusId: number }>
  | Readonly<{ source: 'advanced'; query: string }>
  | Readonly<{ source: 'saved'; savedQuerySlug: string }>;

type TicketExportRequest = Readonly<{ format: TicketExportFormat }> &
  (
    | Readonly<{ source: 'SIMPLE_SEARCH'; term: string; statusId: number }>
    | Readonly<{ source: 'ADVANCED_QUERY'; query: string }>
    | Readonly<{ source: 'SAVED_QUERY'; savedQuerySlug: string }>
  );

const ENCODED_FILENAME_PATTERN = /(?:^|;)\s*filename\*\s*=\s*(?:UTF-8'')?([^;]+)/i;
const ASCII_FILENAME_PATTERN = /(?:^|;)\s*filename\s*=\s*("(?:[^"\\]|\\.)*"|[^;]*)/i;

function removeSurroundingQuotes(value: string): string {
  if (value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1);
  }
  return value;
}

function parseDownloadFilename(contentDisposition: string | null): string | null {
  if (contentDisposition == null) {
    return null;
  }

  const encoded = ENCODED_FILENAME_PATTERN.exec(contentDisposition)?.[1];
  if (encoded != null) {
    const value = removeSurroundingQuotes(encoded.trim());
    try {
      return decodeURIComponent(value);
    } catch {
      // Fall through to the ASCII filename.
    }
  }

  const ascii = ASCII_FILENAME_PATTERN.exec(contentDisposition)?.[1];
  return ascii == null ? null : removeSurroundingQuotes(ascii.trim());
}

@Injectable({
  providedIn: 'root',
})
export class TicketExportService {
  private readonly http = inject(HttpClient);

  download(criteria: TicketExportCriteria, format: TicketExportFormat): Observable<void> {
    return this.http
      .post('/api/tickets/export', this.buildRequest(criteria, format), {
        observe: 'response',
        responseType: 'blob',
      })
      .pipe(
        tap(response => this.triggerBrowserDownload(response, format)),
        map(() => undefined),
      );
  }

  private buildRequest(criteria: TicketExportCriteria, format: TicketExportFormat): TicketExportRequest {
    switch (criteria.source) {
      case 'simple':
        return { format, source: 'SIMPLE_SEARCH', term: criteria.term, statusId: criteria.statusId };
      case 'advanced':
        return { format, source: 'ADVANCED_QUERY', query: criteria.query };
      case 'saved':
        return { format, source: 'SAVED_QUERY', savedQuerySlug: criteria.savedQuerySlug };
    }
  }

  private triggerBrowserDownload(response: HttpResponse<Blob>, format: TicketExportFormat): void {
    const objectUrl = URL.createObjectURL(response.body ?? new Blob());
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download =
      parseDownloadFilename(response.headers.get('Content-Disposition')) ?? `tickets.${format.toLowerCase()}`;

    try {
      document.body.appendChild(anchor);
      anchor.click();
    } finally {
      try {
        if (anchor.parentNode === document.body) {
          document.body.removeChild(anchor); // NOSONAR: explicit removal is part of the temporary-anchor contract
        }
      } finally {
        URL.revokeObjectURL(objectUrl);
      }
    }
  }
}
