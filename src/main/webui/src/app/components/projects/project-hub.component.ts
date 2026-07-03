import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../services/projects.service';

@Component({
  selector: 'app-project-hub',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './project-hub.component.html'
})
export class ProjectHubComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  project?: Project & { owner?: { id: number } };

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
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
}
