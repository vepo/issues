import { Injectable, inject } from '@angular/core';
import { Observable, from, map, switchMap, throwError } from 'rxjs';
import { firstValueFrom } from 'rxjs';
import { TicketApi } from '../generated/api/ticket.service';
import { ApplyColumnMappingRequest } from '../generated/model/applyColumnMappingRequest';
import { CorrectImportRowRequest } from '../generated/model/correctImportRowRequest';
import { ImportRowValidation } from '../generated/model/importRowValidation';
import { ImportTicketsResponse } from '../generated/model/importTicketsResponse';
import { PreviewTicketImportResponse } from '../generated/model/previewTicketImportResponse';
import { TicketImportUploadResponse } from '../generated/model/ticketImportUploadResponse';
import { asLoaded, Loaded } from '../core/required-types';

export type TicketImportPreview = Loaded<PreviewTicketImportResponse>;
export type TicketImportPreviewRow = Loaded<ImportRowValidation>;
export type TicketImportResult = Loaded<ImportTicketsResponse>;
export type TicketImportUpload = Loaded<TicketImportUploadResponse>;

/** Matches server CsvImportParser.MAX_FILE_BYTES / MAX_CHUNK_BYTES */
export const TICKET_IMPORT_MAX_FILE_BYTES = 5 * 1024 * 1024;
export const TICKET_IMPORT_MAX_CHUNK_BYTES = 1024 * 1024;

export interface TicketImportUploadProgress {
  partsSent: number;
  partCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class TicketImportService {
  private readonly api = inject(TicketApi);

  upload(
    projectId: number | null,
    file: File,
    onProgress?: (progress: TicketImportUploadProgress) => void,
  ): Observable<TicketImportUpload> {
    if (file.size > TICKET_IMPORT_MAX_FILE_BYTES) {
      return throwError(() => new Error('FILE_TOO_LARGE'));
    }
    if (file.size === 0) {
      return throwError(() => new Error('FILE_EMPTY'));
    }

    const partCount = Math.max(1, Math.ceil(file.size / TICKET_IMPORT_MAX_CHUNK_BYTES));
    const initRequest = {
      fileName: file.name,
      totalBytes: file.size,
      chunkCount: partCount,
    };

    const init$ = projectId != null
      ? this.api.initTicketImportUpload(projectId, initRequest)
      : this.api.initGlobalTicketImportUpload(initRequest);

    return init$.pipe(
      switchMap(init => {
        const importId = init.importId;
        if (importId == null) {
          return throwError(() => new Error('MISSING_IMPORT_ID'));
        }
        return from(this.uploadParts(projectId, importId, file, partCount, onProgress)).pipe(
          switchMap(() => {
            const complete$ = projectId != null
              ? this.api.completeTicketImportUpload(importId, projectId)
              : this.api.completeGlobalTicketImportUpload(importId);
            return complete$.pipe(map(asLoaded));
          }),
        );
      }),
    );
  }

  private async uploadParts(
    projectId: number | null,
    importId: number,
    file: File,
    partCount: number,
    onProgress?: (progress: TicketImportUploadProgress) => void,
  ): Promise<void> {
    for (let partIndex = 0; partIndex < partCount; partIndex++) {
      const start = partIndex * TICKET_IMPORT_MAX_CHUNK_BYTES;
      const end = Math.min(start + TICKET_IMPORT_MAX_CHUNK_BYTES, file.size);
      const blob = file.slice(start, end);
      if (projectId != null) {
        await firstValueFrom(this.api.uploadTicketImportPart(importId, partIndex, projectId, blob));
      } else {
        await firstValueFrom(this.api.uploadGlobalTicketImportPart(importId, partIndex, blob));
      }
      onProgress?.({ partsSent: partIndex + 1, partCount });
    }
  }

  applyMapping(projectId: number | null, importId: number, request: ApplyColumnMappingRequest): Observable<void> {
    if (projectId != null) {
      return this.api.applyTicketImportMapping(importId, projectId, request).pipe(map(() => undefined));
    }
    return this.api.applyGlobalTicketImportMapping(importId, request).pipe(map(() => undefined));
  }

  preview(projectId: number | null, importId: number): Observable<TicketImportPreview> {
    if (projectId != null) {
      return this.api.previewTicketImport(importId, projectId).pipe(map(asLoaded));
    }
    return this.api.previewGlobalTicketImport(importId).pipe(map(asLoaded));
  }

  correctRow(
    projectId: number | null,
    importId: number,
    rowId: number,
    request: CorrectImportRowRequest,
  ): Observable<TicketImportPreviewRow> {
    if (projectId != null) {
      return this.api.correctTicketImportRow(importId, projectId, rowId, request).pipe(map(asLoaded));
    }
    return this.api.correctGlobalTicketImportRow(importId, rowId, request).pipe(map(asLoaded));
  }

  execute(projectId: number | null, importId: number): Observable<TicketImportResult> {
    if (projectId != null) {
      return this.api.executeTicketImport(importId, projectId).pipe(map(asLoaded));
    }
    return this.api.executeGlobalTicketImport(importId).pipe(map(asLoaded));
  }
}
