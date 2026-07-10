import { CdkDragDrop, CdkDropList, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { AsyncPipe, KeyValuePipe } from '@angular/common';
import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Observable, shareReplay } from 'rxjs';
import { Project } from '../../services/projects.service';
import { DashboardApi } from '../../generated/api/dashboard.service';
import { AvailablesDashboards, DashboardLayout, DashboardWidget, KpiData, PieChartData, TableChartData } from './availables.dashboards';
import { NormalizePipe } from '../pipes/normalize.pipe';

@Component({
  selector: 'app-dashboard.component',
  imports: [DragDropModule, MatButtonModule, MatIconModule, MatSelectModule, FormsModule, BaseChartDirective, AsyncPipe, KeyValuePipe, NormalizePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly availableDasboards = inject(AvailablesDashboards);
  private readonly dashboardApi = inject(DashboardApi);

  @ViewChild('availableList') availableList!: CdkDropList;
  @ViewChild('dashboardList') dashboardList!: CdkDropList;
  dashboardLayouts: DashboardLayout = {
      id: 'default',
      name: 'Layout Padrão',
      widgets: [],
      layout: []
    };

  pageLayout: DashboardLayout = this.dashboardLayouts;
  isEditing = false;
  project: Project | null;
  availableWidgets: DashboardWidget[];

  public pieChartOptions: ChartConfiguration['options'] = {
    plugins: {
      legend: {
        display: true,
        position: 'top',
      }
    },
  };

  constructor() {
    this.project = null;
    this.availableWidgets = this.availableDasboards.all;
  }

  ngOnInit() {
    this.activatedRoute.data.subscribe(({project}) => {
      this.project = project;
      this.loadDashboardConfig();
    });
  }
  
  private pieChartDataCache = new Map<string, { data: Observable<PieChartData>; timestamp: number; }>();
  private tableChartDataCache = new Map<string, { data: Observable<TableChartData>; timestamp: number; }>();
  private kpiChartDataCache = new Map<string, { data: Observable<KpiData>; timestamp: number; }>();

  loadTableData(chart: DashboardWidget): Observable<TableChartData> {
    const cacheKey = `${chart.id}-${this.project!.id}`;
    const now = Date.now();
    const cacheDuration = 15000;

    const cached = this.tableChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    const newData = this.availableDasboards.loadTableData(chart, this.project!.id).pipe(
      shareReplay(1)
    );

    this.tableChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  loadKpiData(chart: DashboardWidget): Observable<KpiData> {
    const cacheKey = `${chart.id}-${this.project!.id}`;
    const now = Date.now();
    const cacheDuration = 15000;

    const cached = this.kpiChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    const newData = this.availableDasboards.loadKpiData(chart, this.project!.id).pipe(
      shareReplay(1)
    );

    this.kpiChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  loadPieData(chart: DashboardWidget): Observable<PieChartData> {
    const cacheKey = `${chart.id}-${this.project!.id}`;
    const now = Date.now();
    const cacheDuration = 15000;

    const cached = this.pieChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    const newData = this.availableDasboards.loadPieData(chart, this.project!.id).pipe(
      shareReplay(1)
    );

    this.pieChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  onDrop(event: CdkDragDrop<any>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
    }
    this.saveDashboardConfig();
  }

  removeWidget(widgetIndex: number) {
    const removed = this.pageLayout.widgets.splice(widgetIndex, 1)[0];
    if (removed) {
      this.availableWidgets.push(removed);
    }
    this.saveDashboardConfig();
  }

  saveDashboardConfig() {
    if (!this.project) {
      return;
    }
    const widgetIds = this.pageLayout.widgets.map(widget => widget.id);
    this.dashboardApi.saveDashboardLayout(this.project.id, { widgetIds }).subscribe();
  }

  loadDashboardConfig() {
    if (!this.project) {
      return;
    }
    this.dashboardApi.getDashboardLayout(this.project.id).subscribe(layout => {
      const widgetIds = layout.widgetIds ?? [];
      const widgets = widgetIds
        .map(id => this.availableDasboards.all.find(widget => widget.id === id))
        .filter((widget): widget is DashboardWidget => !!widget)
        .map(widget => ({ ...widget }));
      this.pageLayout = {
        ...this.dashboardLayouts,
        widgets
      };
      const usedIds = new Set(widgets.map(widget => widget.id));
      this.availableWidgets = this.availableDasboards.all
        .filter(widget => !usedIds.has(widget.id))
        .map(widget => ({ ...widget }));
    });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
  }

  trackByWidgetId(index: number, widget: DashboardWidget) {
    return widget.id;
  }
}
