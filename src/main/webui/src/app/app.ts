import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ContextBarComponent } from './components/context-bar/context-bar.component';
import { NotificationComponent } from './components/notification/notification.component';
import { ProjectMenuComponent } from './components/project-menu/project-menu.component';
import { RoleDirective } from './directives/role.directive';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    FormsModule,
    MatButtonModule,
    NotificationComponent,
    ProjectMenuComponent,
    MatIconModule,
    MatMenuModule,
    RoleDirective,
    ContextBarComponent,
    TranslocoPipe
  ],
  templateUrl: './app.html'
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  title = 'issues';
  searchTerm = '';

  onSearchSubmit(event: Event) {
    event.preventDefault();
    this.goToSearch(this.searchTerm.trim());
  }

  goToSearch(term: string) {
    const params: Record<string, string> = {};
    if (term.length > 0) {
      params['q'] = term;
    }
    this.router.navigate(['/search'], { queryParams: params });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['q'] !== undefined) {
        this.searchTerm = params['q'] || '';
      }
    });
  }

  isAuthenticated(): boolean {
    return this.authService.isLoggedIn();
  }

  userEmail(): string | null {
    return this.authService.getEmail();
  }

  hasAdminMenu(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
