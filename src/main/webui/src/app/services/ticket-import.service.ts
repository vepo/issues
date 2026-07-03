import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
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

@Injectable({
  providedIn: 'root'
})
export class TicketImportService {
  private readonly api = inject(TicketApi);

  upload(projectId: number | null, file: File): Observable<TicketImportUpload> {
    if (projectId != null) {
      return this.api.uploadTicketImport(projectId, file, file.name).pipe(map(asLoaded));
    }
    return this.api.uploadGlobalTicketImport(file, file.name).pipe(map(asLoaded));
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
