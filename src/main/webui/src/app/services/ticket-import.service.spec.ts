import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TicketApi } from '../generated/api/ticket.service';
import {
  TICKET_IMPORT_MAX_CHUNK_BYTES,
  TICKET_IMPORT_MAX_FILE_BYTES,
  TicketImportService,
} from './ticket-import.service';

describe('TicketImportService', () => {
  let service: TicketImportService;
  let api: jasmine.SpyObj<TicketApi>;

  beforeEach(() => {
    api = jasmine.createSpyObj('TicketApi', [
      'initTicketImportUpload',
      'initGlobalTicketImportUpload',
      'uploadTicketImportPart',
      'uploadGlobalTicketImportPart',
      'completeTicketImportUpload',
      'completeGlobalTicketImportUpload',
    ]);
    TestBed.configureTestingModule({
      providers: [
        TicketImportService,
        { provide: TicketApi, useValue: api },
      ],
    });
    service = TestBed.inject(TicketImportService);
  });

  it('should reject files larger than 5 MB before calling the API', (done) => {
    const file = new File([new Uint8Array(TICKET_IMPORT_MAX_FILE_BYTES + 1)], 'big.csv', { type: 'text/csv' });
    service.upload(1, file).subscribe({
      next: () => done.fail('expected error'),
      error: (err: Error) => {
        expect(err.message).toBe('FILE_TOO_LARGE');
        expect(api.initTicketImportUpload).not.toHaveBeenCalled();
        done();
      },
    });
  });

  it('should init, upload parts, and complete with progress', (done) => {
    const bytes = new Uint8Array(TICKET_IMPORT_MAX_CHUNK_BYTES + 10);
    const file = new File([bytes], 'tickets.csv', { type: 'text/csv' });
    const progress: { partsSent: number; partCount: number }[] = [];

    api.initTicketImportUpload.and.returnValue(of({ importId: 42 }) as never);
    api.uploadTicketImportPart.and.returnValue(of(undefined) as never);
    api.completeTicketImportUpload.and.returnValue(of({
      id: 42,
      fileName: 'tickets.csv',
      headers: ['Title'],
      rowCount: 1,
      truncated: false,
      sampleRows: [],
      projectScoped: true,
    }) as never);

    service.upload(7, file, p => progress.push({ ...p })).subscribe({
      next: result => {
        expect(result.id).toBe(42);
        expect(api.initTicketImportUpload).toHaveBeenCalledWith(7, {
          fileName: 'tickets.csv',
          totalBytes: file.size,
          chunkCount: 2,
        });
        expect(api.uploadTicketImportPart).toHaveBeenCalledTimes(2);
        expect(api.completeTicketImportUpload).toHaveBeenCalledWith(42, 7);
        expect(progress).toEqual([
          { partsSent: 1, partCount: 2 },
          { partsSent: 2, partCount: 2 },
        ]);
        done();
      },
      error: done.fail,
    });
  });

  it('should use global chunk APIs when projectId is null', (done) => {
    const file = new File(['Title,Description\na,b'], 'g.csv', { type: 'text/csv' });
    api.initGlobalTicketImportUpload.and.returnValue(of({ importId: 9 }) as never);
    api.uploadGlobalTicketImportPart.and.returnValue(of(undefined) as never);
    api.completeGlobalTicketImportUpload.and.returnValue(of({
      id: 9,
      fileName: 'g.csv',
      headers: ['Title'],
      rowCount: 1,
      truncated: false,
      sampleRows: [],
      projectScoped: false,
    }) as never);

    service.upload(null, file).subscribe({
      next: () => {
        expect(api.initGlobalTicketImportUpload).toHaveBeenCalled();
        expect(api.uploadGlobalTicketImportPart).toHaveBeenCalled();
        expect(api.completeGlobalTicketImportUpload).toHaveBeenCalledWith(9);
        done();
      },
      error: done.fail,
    });
  });

  it('should surface init failures', (done) => {
    const file = new File(['a'], 'a.csv', { type: 'text/csv' });
    api.initTicketImportUpload.and.returnValue(throwError(() => new Error('boom')) as never);
    service.upload(1, file).subscribe({
      next: () => done.fail('expected error'),
      error: (err: Error) => {
        expect(err.message).toBe('boom');
        done();
      },
    });
  });
});
