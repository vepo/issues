import { Observable, finalize } from 'rxjs';

export class TicketExportState {
  loading = false;
  errorKey: string | null = null;

  download(startDownload: () => Observable<unknown>): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorKey = null;
    startDownload()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        error: error => {
          this.errorKey = error?.status === 413 ? 'ticketExport.errorLimit' : 'ticketExport.errorGeneral';
        },
      });
  }
}
