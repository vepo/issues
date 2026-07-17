import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { SavedQuery, SavedQueryService } from '../../services/saved-query.service';
import { RuntimeDatePipe } from '../../core/runtime-locale.pipes';

@Component({
  selector: 'app-saved-query-list',
  standalone: true,
  imports: [CommonModule, RouterLink, RuntimeDatePipe, MatButtonModule, TranslocoPipe],
  templateUrl: './saved-query-list.component.html'
})
export class SavedQueryListComponent implements OnInit {
  private readonly savedQueryService = inject(SavedQueryService);
  private readonly transloco = inject(TranslocoService);

  queries: SavedQuery[] = [];

  ngOnInit(): void {
    this.savedQueryService.list().subscribe(queries => (this.queries = queries));
  }

  deleteQuery(query: SavedQuery): void {
    if (!confirm(this.transloco.translate('search.saved.confirmDelete', { name: query.name }))) {
      return;
    }
    this.savedQueryService.delete(query.id).subscribe(() => {
      this.queries = this.queries.filter(q => q.id !== query.id);
    });
  }
}
