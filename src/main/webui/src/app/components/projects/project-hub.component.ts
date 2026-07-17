import { Component, OnInit, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { Project } from '../../services/projects.service';
import { Version, VersionService } from '../../services/version.service';
import { phaseStatusLabel } from '../../core/system-labels';

@Component({
  selector: 'app-project-hub',
  imports: [TranslocoPipe, RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './project-hub.component.html',
  styleUrl: './project-hub.component.scss'
})
export class ProjectHubComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly phaseService = inject(PhaseService);
  private readonly versionService = inject(VersionService);

  project?: Project & { owner?: { id: number } };
  phases: Phase[] = [];
  versions: Version[] = [];

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
      if (project?.id) {
        this.loadPlanning(project.id);
      }
    });
  }

  canManage(): boolean {
    if (!this.project) {
      return false;
    }
    if (this.authService.hasRole('admin')) {
      return true;
    }
    const ownerId = (this.project as Project & { owner?: { id: number } }).owner?.id;
    return ownerId != null && ownerId === this.authService.getAuthUserId();
  }

  statusLabel(status: Phase['status']): string {
    return phaseStatusLabel(status);
  }

  private loadPlanning(projectId: number): void {
    forkJoin({
      phases: this.phaseService.list(projectId),
      versions: this.versionService.list(projectId)
    }).subscribe(({ phases, versions }) => {
      this.phases = phases;
      this.versions = versions;
    });
  }
}
