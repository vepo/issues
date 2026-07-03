import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QueryLanguageHelpComponent } from '../query-language-help/query-language-help.component';
import { SavedQueryService } from '../../services/saved-query.service';

@Component({
  selector: 'app-saved-query-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, QueryLanguageHelpComponent],
  templateUrl: './saved-query-edit.component.html'
})
export class SavedQueryEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly savedQueryService = inject(SavedQueryService);

  id: number | null = null;
  name = '';
  queryText = '';
  showAtHome = false;
  error = '';

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap.get('q');
    if (q) {
      this.queryText = q;
    }
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.id = Number(idParam);
      this.savedQueryService.list().subscribe(queries => {
        const found = queries.find(item => item.id === this.id);
        if (found) {
          this.name = found.name;
          this.queryText = found.query;
          this.showAtHome = found.showAtHome;
        }
      });
    }
  }

  save(): void {
    this.error = '';
    const payload = { name: this.name, query: this.queryText, showAtHome: this.showAtHome };
    const request$ = this.id
      ? this.savedQueryService.update(this.id, payload)
      : this.savedQueryService.create(payload);
    request$.subscribe({
      next: saved => this.router.navigate(['/search/q', saved.slug]),
      error: err => (this.error = err.error?.message ?? 'Erro ao salvar')
    });
  }

  deleteQuery(): void {
    if (!this.id || !confirm('Excluir esta consulta?')) {
      return;
    }
    this.savedQueryService.delete(this.id).subscribe(() => this.router.navigate(['/search/queries']));
  }
}
