import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { Project } from '../../services/projects.service';
import { TicketExpanded } from '../../services/ticket.service';

interface ContextCrumb {
  label: string;
  link?: string[];
}

@Component({
  selector: 'app-context-bar',
  imports: [RouterLink, MatIconModule],
  template: `
    @if (crumbs().length > 0) {
      <nav class="context-bar" aria-label="Navegação contextual">
        <div class="shell-inner">
          <nav class="breadcrumb">
            @for (crumb of crumbs(); track $index; let last = $last) {
              @if (!last && crumb.link) {
                <a [routerLink]="crumb.link">{{ crumb.label }}</a>
                <mat-icon fontIcon="chevron_right" aria-hidden="true"></mat-icon>
              } @else {
                <span class="breadcrumb__current">{{ crumb.label }}</span>
              }
            }
          </nav>
        </div>
      </nav>
    }
  `
})
export class ContextBarComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  crumbs = signal<ContextCrumb[]>([]);
  private navigationSubscription?: Subscription;

  ngOnInit(): void {
    this.updateCrumbs();
    this.navigationSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateCrumbs());
  }

  ngOnDestroy(): void {
    this.navigationSubscription?.unsubscribe();
  }

  private updateCrumbs(): void {
    const path = this.router.url.split('?')[0];
    const data = this.collectRouteData();

    const kanbanMatch = /^\/project\/\d+\/kanban$/.exec(path);
    if (kanbanMatch) {
      const project = data['project'] as Project | undefined;
      if (project) {
        this.crumbs.set([
          { label: 'Início', link: ['/'] },
          { label: project.name, link: ['/project', String(project.id), 'kanban'] },
          { label: 'Kanban' }
        ]);
        return;
      }
    }

    const dashboardMatch = /^\/project\/\d+\/dashboard$/.exec(path);
    if (dashboardMatch) {
      const project = data['project'] as Project | undefined;
      if (project) {
        this.crumbs.set([
          { label: 'Início', link: ['/'] },
          { label: project.name, link: ['/project', String(project.id), 'kanban'] },
          { label: 'Painel' }
        ]);
        return;
      }
    }

    const ticketMatch = /^\/ticket\/(.+)$/.exec(path);
    if (ticketMatch) {
      const ticket = data['ticket'] as TicketExpanded | undefined;
      const projectName = ticket?.project?.name;
      const projectId = ticket?.project?.id;
      if (ticket && projectName && projectId != null) {
        this.crumbs.set([
          { label: 'Início', link: ['/'] },
          { label: projectName, link: ['/project', String(projectId), 'kanban'] },
          { label: `#${ticket.identifier}` }
        ]);
        return;
      }
    }

    this.crumbs.set([]);
  }

  private collectRouteData(): Record<string, unknown> {
    let child = this.route.root;
    let merged: Record<string, unknown> = {};
    while (child?.firstChild) {
      child = child.firstChild;
      merged = { ...merged, ...child.snapshot.data };
    }
    return merged;
  }
}
