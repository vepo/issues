import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../../services/auth.service';
import { Project, ProjectsService } from '../../services/projects.service';

@Component({
  selector: 'app-project-menu',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule, RouterLink, TranslocoPipe],
  templateUrl: './project-menu.component.html'
})
export class ProjectMenuComponent implements OnInit {
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  projects: Project[] = [];
  get emptyTooltip(): string {
    return this.transloco.translate('shell.noProjects');
  }

  ngOnInit(): void {
    this.projectsService.findAll().subscribe(projects => {
      this.projects = projects;
    });
  }

  hasProjects(): boolean {
    return this.projects.length > 0;
  }

  canManageProjects(): boolean {
    return this.authService.hasRole('project-manager') || this.authService.hasRole('admin');
  }

  kanbanLink(project: Project): (string | number)[] {
    return ['/project', project.id, 'kanban'];
  }
}
