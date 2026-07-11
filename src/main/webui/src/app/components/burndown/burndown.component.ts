import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Burndown, BurndownService } from '../../services/burndown.service';
import { Phase, PhaseService } from '../../services/phase.service';
import { Project } from '../../services/projects.service';

const DATES_INCOMPLETE_TOOLTIP = 'Defina data de início e fim na fase para habilitar o Burndown.';

@Component({
  selector: 'app-burndown',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatTooltipModule,
    BaseChartDirective,
  ],
  templateUrl: './burndown.component.html',
  styleUrl: './burndown.component.scss',
})
export class BurndownComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly phaseService = inject(PhaseService);
  private readonly burndownService = inject(BurndownService);

  project!: Project;
  phases: Phase[] = [];
  selectedPhaseId: number | null = null;
  burndown: Burndown | null = null;
  loading = false;
  loadError = '';
  chartVisible = false;

  readonly datesIncompleteTooltip = DATES_INCOMPLETE_TOOLTIP;

  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
  readonly lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'bottom' },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: { display: true, text: 'Story points' },
      },
    },
  };

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
      this.loadPhases();
    });
  }

  get selectedPhase(): Phase | null {
    if (this.selectedPhaseId == null) {
      return null;
    }
    return this.phases.find(p => p.id === this.selectedPhaseId) ?? null;
  }

  get datesComplete(): boolean {
    const phase = this.selectedPhase;
    return !!phase?.startDate && !!phase?.endDate;
  }

  get chartEnabled(): boolean {
    return this.datesComplete && this.selectedPhaseId != null;
  }

  onPhaseChange(phaseId: number | null): void {
    this.selectedPhaseId = phaseId;
    this.chartVisible = false;
    this.burndown = null;
    this.loadError = '';
  }

  showBurndown(): void {
    if (!this.chartEnabled || this.selectedPhaseId == null) {
      return;
    }
    this.loading = true;
    this.loadError = '';
    this.burndownService.load(this.project.id, this.selectedPhaseId).subscribe({
      next: burndown => {
        this.burndown = burndown;
        this.chartVisible = burndown.datesComplete;
        this.updateChart(burndown);
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Não foi possível carregar o Burndown.';
        this.loading = false;
      },
    });
  }

  private loadPhases(): void {
    this.phaseService.list(this.project.id).subscribe(phases => {
      this.phases = phases;
      const active = phases.find(p => p.status === 'ACTIVE');
      this.selectedPhaseId = active?.id ?? null;
    });
  }

  private updateChart(burndown: Burndown): void {
    const series = burndown.series ?? [];
    this.lineChartData = {
      labels: series.map(point => point.date ?? ''),
      datasets: [
        {
          label: 'Ideal',
          data: series.map(point => point.ideal ?? 0),
          borderColor: '#5c6bc0',
          backgroundColor: 'transparent',
          tension: 0,
          pointRadius: 2,
        },
        {
          label: 'Remaining',
          data: series.map(point => point.remaining ?? 0),
          borderColor: '#00897b',
          backgroundColor: 'transparent',
          tension: 0.1,
          pointRadius: 3,
        },
      ],
    };
  }
}
