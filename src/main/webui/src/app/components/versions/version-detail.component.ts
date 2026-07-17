import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../services/projects.service';
import { Version, VersionChangelog, VersionService } from '../../services/version.service';
import { RuntimeDatePipe } from '../../core/runtime-locale.pipes';

@Component({
  selector: 'app-version-detail',
  imports: [
    TranslocoPipe,
    RuntimeDatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './version-detail.component.html'
})
export class VersionDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly versionService = inject(VersionService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  project?: Project;
  version?: Version;
  changelog?: VersionChangelog;
  createMode = false;
  isSaving = false;

  form: FormGroup = this.formBuilder.group({
    label: ['', [Validators.required, Validators.maxLength(64)]],
    description: ['']
  });

  ngOnInit(): void {
    this.route.data.subscribe(({ project, version }) => {
      this.project = project;
      this.createMode = this.route.snapshot.paramMap.get('versionId') === 'new';
      if (!this.createMode && version) {
        this.version = version;
        this.form.patchValue({
          label: version.label,
          description: version.description ?? ''
        });
        this.loadChangelog();
      }
    });
  }

  canManage(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }

  loadChangelog(): void {
    if (!this.project?.id || !this.version?.id) {
      return;
    }
    this.versionService.changelog(this.project.id, this.version.id)
                       .subscribe(changelog => this.changelog = changelog);
  }

  save(): void {
    if (!this.project?.id || this.form.invalid) {
      return;
    }
    this.isSaving = true;
    const value = this.form.value;
    const request = { label: value.label.trim(), description: value.description?.trim() || null };

    if (this.createMode) {
      this.versionService.create(this.project.id, request).subscribe({
        next: (created) => {
          this.isSaving = false;
          this.router.navigate(['/project', this.project!.id, 'versions', created.id]);
        },
        error: () => {
          this.isSaving = false;
        }
      });
      return;
    }

    if (!this.version?.id) {
      return;
    }
    this.versionService.update(this.project.id, this.version.id, request).subscribe({
      next: (updated) => {
        this.isSaving = false;
        this.version = updated;
        this.loadChangelog();
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }
}
