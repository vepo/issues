import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { SavedQuery, SavedQueryService, SavedQueryWithResults } from '../../services/saved-query.service';
import { Ticket } from '../../services/ticket.service';

@Component({
  selector: 'app-saved-query-view',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule],
  templateUrl: './saved-query-view.component.html'
})
export class SavedQueryViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly savedQueryService = inject(SavedQueryService);
  private readonly authService = inject(AuthService);

  savedQuery: SavedQuery | null = null;
  tickets: Ticket[] = [];
  isOwner = false;

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      return;
    }
    this.savedQueryService.findBySlug(slug).subscribe((result: SavedQueryWithResults) => {
      this.savedQuery = result.savedQuery;
      this.tickets = result.tickets;
      this.authService.me().subscribe(me => {
        this.isOwner = me.id === result.savedQuery.ownerId;
      });
    });
  }

  copyLink(): void {
    navigator.clipboard.writeText(window.location.href);
  }

  cloneQuery(): void {
    if (!this.savedQuery) {
      return;
    }
    this.savedQueryService.clone(this.savedQuery.id).subscribe(cloned =>
      this.router.navigate(['/search/queries', cloned.id, 'edit'])
    );
  }
}
