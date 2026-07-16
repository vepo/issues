import { inject, Injectable } from '@angular/core';
import { ChartData } from 'chart.js';
import { catchError, map, Observable, of, startWith } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';

export interface DashboardWidget {
  id: string;
  title: string;
  type: 'chart' | 'table' | 'kpi';
  chartType?: 'line' | 'bar' | 'pie' | 'doughnut';
  cols: number;
  rows: number;
}

export interface DashboardLayout {
  id: string;
  name: string;
  widgets: DashboardWidget[];
  layout: string[];
}

export type PieChartData = ChartData<'pie', number[], string | string[]>;
export type BarChartData = ChartData<'bar', number[], string | string[]>;

export interface TableChartRowData {
  data: string[];
}

export interface TableChartData {
  columns: string[];
  rows: TableChartRowData[];
}

export interface KpiData {
  total: number;
  perStatus: Map<string, number>;
}

export type WidgetLoadState<T> =
  | { kind: 'loading' }
  | { kind: 'empty' }
  | { kind: 'error' }
  | { kind: 'ready'; data: T };

@Injectable({ providedIn: 'root' })
export class AvailablesDashboards {
  private readonly dashboardService = inject(DashboardService);

  public all: DashboardWidget[] = [
    {
      id: 'tickets-by-day',
      title: 'Tickets por Dia',
      type: 'chart',
      chartType: 'bar',
      cols: 1,
      rows: 1,
    },
    {
      id: 'tickets-by-status',
      title: 'Tickets por Status',
      type: 'chart',
      chartType: 'pie',
      cols: 1,
      rows: 1,
    },
    {
      id: 'tickets-by-priority',
      title: 'Tickets por Prioridade',
      type: 'chart',
      chartType: 'pie',
      cols: 1,
      rows: 1,
    },
    {
      id: 'performance-kpi',
      title: 'Tickets por status',
      type: 'kpi',
      cols: 1,
      rows: 1,
    },
    {
      id: 'recent-tickets',
      title: 'Tickets Recentes',
      type: 'table',
      cols: 2,
      rows: 2,
    },
  ];

  loadChartData(widget: DashboardWidget, projectId: number): Observable<WidgetLoadState<PieChartData | BarChartData>> {
    return this.dashboardService.loadPie(projectId, widget.id).pipe(
      map((response) => {
        const labels = response.labels ?? [];
        const datasets = (response.datasets ?? []).map((dataset) => ({
          data: dataset.data ?? [],
          backgroundColor: dataset.colors,
          label: dataset.label,
        }));
        const empty = labels.length === 0 || datasets.every((d) => (d.data?.length ?? 0) === 0);
        if (empty) {
          return { kind: 'empty' } as const;
        }
        return {
          kind: 'ready',
          data: { labels, datasets },
        } as const;
      }),
      startWith({ kind: 'loading' } as const),
      catchError(() => of({ kind: 'error' } as const)),
    );
  }

  loadKpiData(widget: DashboardWidget, projectId: number): Observable<WidgetLoadState<KpiData>> {
    return this.dashboardService.loadKpi(projectId, widget.id).pipe(
      map((response) => {
        const perStatus = new Map(Object.entries(response.perStatus ?? {}));
        const total = response.total ?? 0;
        if (total === 0 && perStatus.size === 0) {
          return { kind: 'empty' } as const;
        }
        return {
          kind: 'ready',
          data: { total, perStatus },
        } as const;
      }),
      startWith({ kind: 'loading' } as const),
      catchError(() => of({ kind: 'error' } as const)),
    );
  }

  loadTableData(widget: DashboardWidget, projectId: number): Observable<WidgetLoadState<TableChartData>> {
    return this.dashboardService.loadTable(projectId, widget.id).pipe(
      map((response) => {
        const columns = response.columns ?? [];
        const rows = (response.rows ?? []).map((row) => ({ data: row.data ?? [] }));
        if (rows.length === 0) {
          return { kind: 'empty' } as const;
        }
        return {
          kind: 'ready',
          data: { columns, rows },
        } as const;
      }),
      startWith({ kind: 'loading' } as const),
      catchError(() => of({ kind: 'error' } as const)),
    );
  }
}
