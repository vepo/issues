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

  // data: Map<string, ChartData<'pie', number[], string | string[]>> = new Map<string, ChartData<'pie', number[], string | string[]>>();

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

   // Pie
  public pieChartOptions: ChartConfiguration['options'] = {
    plugins: {
      legend: {
        display: true,
        position: 'top',
      }
    },
  };
  // public pieChartData: ChartData<'pie', number[], string | string[]> = {
  //   labels: [['Download', 'Sales'], ['In', 'Store', 'Sales'], 'Mail Sales'],
  //   datasets: [
  //     {
  //       data: [300, 500, 100],
  //     },
  //   ],
  // };

  constructor() {
    this.project = null;
    this.availableWidgets = this.availableDasboards.all;
  }

  ngOnInit() {
    this.activatedRoute.data.subscribe(({project}) => {
      console.log(project);
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
    const cacheDuration = 15000; // 15 segundos

    // Verifica se existe cache válido
    const cached = this.tableChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    // Cria novo observable com cache
    const newData = this.availableDasboards.loadTableData(chart, this.project!.id).pipe(
      shareReplay(1) // Compartilha a última emissão
    );

    // Atualiza cache
    this.tableChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  loadKpiData(chart: DashboardWidget): Observable<KpiData> {
    console.log("Updating data...", chart);
    const cacheKey = `${chart.id}-${this.project!.id}`;
    const now = Date.now();
    const cacheDuration = 15000; // 15 segundos

    // Verifica se existe cache válido
    const cached = this.kpiChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    // Cria novo observable com cache
    const newData = this.availableDasboards.loadKpiData(chart, this.project!.id).pipe(
      shareReplay(1) // Compartilha a última emissão
    );

    // Atualiza cache
    this.kpiChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  loadPieData(chart: DashboardWidget): Observable<PieChartData> {
    console.log("Updating data...", chart);
    const cacheKey = `${chart.id}-${this.project!.id}`;
    const now = Date.now();
    const cacheDuration = 15000; // 15 segundos

    // Verifica se existe cache válido
    const cached = this.pieChartDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp) < cacheDuration) {
      return cached.data;
    }

    // Cria novo observable com cache
    const newData = this.availableDasboards.loadPieData(chart, this.project!.id).pipe(
      shareReplay(1) // Compartilha a última emissão
    );

    // Atualiza cache
    this.pieChartDataCache.set(cacheKey, {
      data: newData,
      timestamp: now
    });

    return newData;
  }

  onDrop(event: CdkDragDrop<any>) {
    console.log('Event', event);
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      // Clone the widget to avoid reference issues
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
    console.log('Removed', widgetIndex);
    this.pageLayout.widgets.splice(widgetIndex, 1);
    this.availableWidgets.push(this.pageLayout.widgets[widgetIndex]);
    this.saveDashboardConfig();
  }

  saveDashboardConfig() {
    console.log("Saving... ", `project-layout-${this.project?.id}`)
    console.log(this.pageLayout)
    localStorage.setItem(`project-layout-${this.project?.id}`, JSON.stringify(this.pageLayout));
    console.log("Saved", this.pageLayout);
  }

  loadDashboardConfig() {
    console.log("Loading", `project-layout-${this.project?.id}`);
    const savedConfig = localStorage.getItem(`project-layout-${this.project?.id}`);
    console.log(savedConfig);
    if (savedConfig) {
      this.pageLayout = JSON.parse(savedConfig);
    }
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
  }

  trackByWidgetId(index: number, widget: DashboardWidget) {
    return widget.id;
  }
}