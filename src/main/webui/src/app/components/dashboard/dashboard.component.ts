import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { AsyncPipe, KeyValuePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Observable, shareReplay } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { Project } from '../../services/projects.service';
import { ToastService } from '../../services/toast.service';
import { NormalizePipe } from '../pipes/normalize.pipe';
import {
  AvailablesDashboards,
  BarChartData,
  DashboardLayout,
  DashboardWidget,
  KpiData,
  PieChartData,
  TableChartData,
  WidgetLoadState,
} from './availables.dashboards';

@Component({
  selector: 'app-dashboard.component',
  imports: [
    DragDropModule,
    MatButtonModule,
    MatIconModule,
    BaseChartDirective,
    AsyncPipe,
    KeyValuePipe,
    NormalizePipe,
    RouterLink,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly availableDashboards = inject(AvailablesDashboards);
  private readonly dashboardService = inject(DashboardService);
  private readonly toast = inject(ToastService);

  private readonly dashboardLayouts: DashboardLayout = {
    id: 'default',
    name: 'Layout Padrão',
    widgets: [],
    layout: [],
  };

  pageLayout: DashboardLayout = this.dashboardLayouts;
  isEditing = false;
  project: Project | null = null;
  availableWidgets: DashboardWidget[] = [];

  readonly pieChartOptions: ChartConfiguration['options'] = {
    plugins: {
      legend: {
        display: true,
        position: 'top',
      },
    },
  };

  readonly barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    plugins: {
      legend: {
        display: true,
        position: 'top',
      },
    },
    scales: {
      y: {
        beginAtZero: true,
      },
    },
  };

  private chartCache = new Map<string, Observable<WidgetLoadState<PieChartData | BarChartData>>>();
  private tableCache = new Map<string, Observable<WidgetLoadState<TableChartData>>>();
  private kpiCache = new Map<string, Observable<WidgetLoadState<KpiData>>>();
  private retryTokens = new Map<string, number>();

  ngOnInit(): void {
    this.availableWidgets = this.availableDashboards.all.map((widget) => ({ ...widget }));
    this.activatedRoute.data.subscribe(({ project }) => {
      this.project = project;
      this.loadDashboardConfig();
    });
  }

  loadChartData(widget: DashboardWidget): Observable<WidgetLoadState<PieChartData | BarChartData>> {
    return this.cached(this.chartCache, widget, () =>
      this.availableDashboards.loadChartData(widget, this.project!.id).pipe(shareReplay(1)),
    );
  }

  loadKpiData(widget: DashboardWidget): Observable<WidgetLoadState<KpiData>> {
    return this.cached(this.kpiCache, widget, () =>
      this.availableDashboards.loadKpiData(widget, this.project!.id).pipe(shareReplay(1)),
    );
  }

  loadTableData(widget: DashboardWidget): Observable<WidgetLoadState<TableChartData>> {
    return this.cached(this.tableCache, widget, () =>
      this.availableDashboards.loadTableData(widget, this.project!.id).pipe(shareReplay(1)),
    );
  }

  retryWidget(widget: DashboardWidget): void {
    if (!this.project) {
      return;
    }
    const key = `${widget.id}-${this.project.id}`;
    this.retryTokens.set(key, (this.retryTokens.get(key) ?? 0) + 1);
    this.chartCache.delete(key);
    this.kpiCache.delete(key);
    this.tableCache.delete(key);
  }

  onDrop(event: CdkDragDrop<DashboardWidget[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    }
    this.saveDashboardConfig();
  }

  removeWidget(widgetIndex: number): void {
    const removed = this.pageLayout.widgets.splice(widgetIndex, 1)[0];
    if (removed) {
      this.availableWidgets.push(removed);
    }
    this.saveDashboardConfig();
  }

  saveDashboardConfig(): void {
    if (!this.project) {
      return;
    }
    const widgetIds = this.pageLayout.widgets.map((widget) => widget.id);
    this.dashboardService.saveLayout(this.project.id, widgetIds).subscribe({
      next: () => this.toast.success('Layout salvo'),
      error: () => this.toast.error('Não foi possível salvar o layout'),
    });
  }

  loadDashboardConfig(): void {
    if (!this.project) {
      return;
    }
    this.dashboardService.getLayout(this.project.id).subscribe((layout) => {
      const widgetIds = layout.widgetIds ?? [];
      const widgets = widgetIds
        .map((id) => this.availableDashboards.all.find((widget) => widget.id === id))
        .filter((widget): widget is DashboardWidget => !!widget)
        .map((widget) => ({ ...widget }));
      this.pageLayout = {
        ...this.dashboardLayouts,
        widgets,
      };
      const usedIds = new Set(widgets.map((widget) => widget.id));
      this.availableWidgets = this.availableDashboards.all
        .filter((widget) => !usedIds.has(widget.id))
        .map((widget) => ({ ...widget }));
      this.clearCaches();
    });
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
  }

  trackByWidgetId(_index: number, widget: DashboardWidget): string {
    return widget.id;
  }

  private cached<T>(
    cache: Map<string, Observable<T>>,
    widget: DashboardWidget,
    factory: () => Observable<T>,
  ): Observable<T> {
    const baseKey = `${widget.id}-${this.project!.id}`;
    const token = this.retryTokens.get(baseKey) ?? 0;
    const cacheKey = `${baseKey}#${token}`;
    const existing = cache.get(cacheKey);
    if (existing) {
      return existing;
    }
    const created = factory();
    cache.set(cacheKey, created);
    return created;
  }

  private clearCaches(): void {
    this.chartCache.clear();
    this.kpiCache.clear();
    this.tableCache.clear();
    this.retryTokens.clear();
  }
}
