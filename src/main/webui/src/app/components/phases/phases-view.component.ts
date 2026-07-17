import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../services/projects.service';
import { Phase } from '../../services/phase.service';
import { phaseStatusLabel } from '../../core/system-labels';

@Component({
  selector: 'app-phases-view',
  imports: [TranslocoPipe, RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './phases-view.component.html'
})
export class PhasesViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  project?: Project;
  phases: Phase[] = [];

  ngOnInit(): void {
    this.route.data.subscribe(({ project, phases }) => {
      this.project = project;
      this.phases = phases ?? [];
    });
  }

  canManage(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }

  statusLabel(status: Phase['status']): string {
    return phaseStatusLabel(status);
  }
}
