import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { QueryLanguageHelpComponent } from '../query-language-help/query-language-help.component';
import { ContextHintComponent } from '../context-hint/context-hint.component';
import { TicketExportState } from '../ticket-export-state';
import { TicketExportFormat, TicketExportService } from '../../services/ticket-export.service';
import { Ticket } from '../../services/ticket.service';
import { SavedQueryService } from '../../services/saved-query.service';

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatMenuModule,
    TranslocoPipe,
    QueryLanguageHelpComponent,
    ContextHintComponent,
  ],
  templateUrl: './advanced-search.component.html',
  styleUrl: './advanced-search.component.scss'
})
export class AdvancedSearchComponent {
  private readonly savedQueryService = inject(SavedQueryService);
  private readonly ticketExportService = inject(TicketExportService);
  private readonly router = inject(Router);

  queryText = '';
  tickets: Ticket[] = [];
  selectedTicket: Ticket | null = null;
  error = '';
  loading = false;
  searched = false;
  lastSubmittedQuery: string | null = null;
  readonly exportState = new TicketExportState();

  runSearch(): void {
    const submittedQuery = this.queryText;
    this.error = '';
    this.loading = true;
    this.searched = false;
    this.selectedTicket = null;
    this.lastSubmittedQuery = null;
    this.savedQueryService.searchByQuery(submittedQuery).subscribe({
      next: tickets => {
        this.tickets = tickets;
        this.selectedTicket = tickets[0] ?? null;
        this.loading = false;
        this.searched = true;
        this.lastSubmittedQuery = submittedQuery;
      },
      error: err => {
        this.error = err.error?.message ?? 'Consulta inválida';
        this.tickets = [];
        this.selectedTicket = null;
        this.loading = false;
        this.searched = true;
      }
    });
  }

  exportTickets(format: TicketExportFormat): void {
    if (this.lastSubmittedQuery == null) {
      return;
    }

    const submittedQuery = this.lastSubmittedQuery;
    this.exportState.download(() =>
      this.ticketExportService.download({ source: 'advanced', query: submittedQuery }, format)
    );
  }

  selectTicket(ticket: Ticket): void {
    this.selectedTicket = ticket;
  }

  isSelected(ticket: Ticket): boolean {
    return this.selectedTicket?.id === ticket.id;
  }

  saveQuery(): void {
    this.router.navigate(['/search/queries/new'], { queryParams: { q: this.queryText } });
  }
}
