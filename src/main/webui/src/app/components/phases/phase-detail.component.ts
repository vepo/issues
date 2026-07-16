import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../services/projects.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { Version, VersionService } from '../../services/version.service';
import { phaseStatusLabel } from '../../core/system-labels';

@Component({
  selector: 'app-phase-detail',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './phase-detail.component.html'
})
export class PhaseDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly phaseService = inject(PhaseService);
  private readonly versionService = inject(VersionService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  project?: Project;
  phase?: Phase;
  versions: Version[] = [];
  createMode = false;
  isSaving = false;
  isTransitioning = false;

  form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(128)]],
    objective: [''],
    startDate: [''],
    endDate: [''],
    deliverableVersionId: [null as number | null],
    deliverables: this.formBuilder.array([])
  });

  ngOnInit(): void {
    this.route.data.subscribe(({ project, phase }) => {
      this.project = project;
      this.createMode = this.route.snapshot.paramMap.get('phaseId') === 'new';
      if (!this.createMode && phase) {
        this.phase = phase;
        this.populateForm();
      } else if (this.createMode) {
        this.addDeliverable();
      }
      if (this.project?.id) {
        this.versionService.list(this.project.id).subscribe(versions => this.versions = versions);
      }
    });
  }

  deliverablesArray(): FormArray {
    return this.form.get('deliverables') as FormArray;
  }

  addDeliverable(value = ''): void {
    this.deliverablesArray().push(this.formBuilder.control(value, Validators.required));
  }

  removeDeliverable(index: number): void {
    this.deliverablesArray().removeAt(index);
  }

  canManage(): boolean {
    return this.authService.hasRole('admin') || this.authService.hasRole('project-manager');
  }

  statusLabel(status: Phase['status']): string {
    return phaseStatusLabel(status);
  }

  populateForm(): void {
    if (!this.phase) {
      return;
    }
    this.deliverablesArray().clear();
    for (const deliverable of this.phase.deliverables ?? []) {
      this.addDeliverable(deliverable.text);
    }
    if (this.deliverablesArray().length === 0) {
      this.addDeliverable();
    }
    this.form.patchValue({
      name: this.phase.name,
      objective: this.phase.objective ?? '',
      startDate: this.phase.startDate ?? '',
      endDate: this.phase.endDate ?? '',
      deliverableVersionId: this.phase.deliverableVersionId ?? null
    });
  }

  buildRequest() {
    const value = this.form.value;
    return {
      name: value.name.trim(),
      objective: value.objective?.trim() || null,
      startDate: value.startDate || null,
      endDate: value.endDate || null,
      deliverableVersionId: value.deliverableVersionId ?? null,
      deliverables: (value.deliverables as string[])
        .map(text => text?.trim())
        .filter(text => !!text)
    };
  }

  save(): void {
    if (!this.project?.id || this.form.invalid) {
      return;
    }
    this.isSaving = true;
    const request = this.buildRequest();

    if (this.createMode) {
      this.phaseService.create(this.project.id, request).subscribe({
        next: (created) => {
          this.isSaving = false;
          this.router.navigate(['/project', this.project!.id, 'phases', created.id]);
        },
        error: () => {
          this.isSaving = false;
        }
      });
      return;
    }

    if (!this.phase?.id) {
      return;
    }
    this.phaseService.update(this.project.id, this.phase.id, request).subscribe({
      next: (updated) => {
        this.isSaving = false;
        this.phase = updated;
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }

  activate(): void {
    if (!this.project?.id || !this.phase?.id) {
      return;
    }
    this.isTransitioning = true;
    this.phaseService.activate(this.project.id, this.phase.id).subscribe({
      next: (updated) => {
        this.isTransitioning = false;
        this.phase = updated;
      },
      error: () => {
        this.isTransitioning = false;
      }
    });
  }

  complete(): void {
    if (!this.project?.id || !this.phase?.id) {
      return;
    }
    this.isTransitioning = true;
    this.phaseService.complete(this.project.id, this.phase.id).subscribe({
      next: (updated) => {
        this.isTransitioning = false;
        this.phase = updated;
      },
      error: () => {
        this.isTransitioning = false;
      }
    });
  }
}
