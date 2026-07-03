import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { QueryLanguageHelpComponent } from '../query-language-help/query-language-help.component';
import { ContextHintComponent } from '../context-hint/context-hint.component';
import { Ticket } from '../../services/ticket.service';
import { SavedQueryService } from '../../services/saved-query.service';

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, QueryLanguageHelpComponent, ContextHintComponent],
  templateUrl: './advanced-search.component.html',
  styleUrl: './advanced-search.component.scss'
})
export class AdvancedSearchComponent {
  private readonly savedQueryService = inject(SavedQueryService);
  private readonly router = inject(Router);

  queryText = '';
  tickets: Ticket[] = [];
  selectedTicket: Ticket | null = null;
  error = '';
  loading = false;
  searched = false;

  runSearch(): void {
    this.error = '';
    this.loading = true;
    this.searched = false;
    this.selectedTicket = null;
    this.savedQueryService.searchByQuery(this.queryText).subscribe({
      next: tickets => {
        this.tickets = tickets;
        this.selectedTicket = tickets[0] ?? null;
        this.loading = false;
        this.searched = true;
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
