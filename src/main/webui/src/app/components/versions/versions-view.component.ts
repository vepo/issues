import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../services/projects.service';
import { Version } from '../../services/version.service';

@Component({
  selector: 'app-versions-view',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './versions-view.component.html'
})
export class VersionsViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  project?: Project;
  versions: Version[] = [];

  ngOnInit(): void {
    this.route.data.subscribe(({ project, versions }) => {
      this.project = project;
      this.versions = versions ?? [];
    });
  }

  canManage(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }
}
