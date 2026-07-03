import { MatButtonModule } from '@angular/material/button';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SavedQuery, SavedQueryService } from '../../services/saved-query.service';

@Component({
  selector: 'app-saved-query-list',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, MatButtonModule],
  templateUrl: './saved-query-list.component.html'
})
export class SavedQueryListComponent implements OnInit {
  private readonly savedQueryService = inject(SavedQueryService);

  queries: SavedQuery[] = [];

  ngOnInit(): void {
    this.savedQueryService.list().subscribe(queries => (this.queries = queries));
  }

  deleteQuery(query: SavedQuery): void {
    if (!confirm(`Excluir consulta "${query.name}"?`)) {
      return;
    }
    this.savedQueryService.delete(query.id).subscribe(() => {
      this.queries = this.queries.filter(q => q.id !== query.id);
    });
  }
}
