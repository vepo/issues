import { inject, Injectable } from "@angular/core";
import { ChartData } from "chart.js";
import { catchError, map, Observable, of, retry, startWith, timer } from "rxjs";
import { DashboardApi } from "../../generated/api/dashboard.service";
import { DashboardType } from "../../generated/model/dashboardType";
import { KpiDataResponse } from "../../generated/model/kpiDataResponse";
import { PieChartDataResponse } from "../../generated/model/pieChartDataResponse";
import { TableDataResponse } from "../../generated/model/tableDataResponse";


export interface DataSupplier {
    loadData(projectId: number): Observable<ChartData<'pie', number[], string | string[]>>;
}

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
    layout: string[]; // Grid layout representation
}

export type PieChartData = ChartData<'pie', number[], string | string[]>;

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

@Injectable({ providedIn: 'root' })
export class AvailablesDashboards {
    private readonly dashboardApi = inject(DashboardApi);

    public all: DashboardWidget[] = [{
        id: 'tickets-by-day',
        title: 'Tickets por Dia',
        type: 'chart',
        chartType: 'pie',
        cols: 1,
        rows: 1
    }, {
        id: 'tickets-by-status',
        title: 'Tickets por Status',
        type: 'chart',
        chartType: 'pie',
        cols: 1,
        rows: 1
    }, {
        id: 'tickets-by-priority',
        title: 'Tickets por Prioridade',
        type: 'chart',
        chartType: 'pie',
        cols: 1,
        rows: 1
    }, {
        id: 'performance-kpi',
        title: 'KPIs de Performance',
        type: 'kpi',
        cols: 1,
        rows: 1
    }, {
        id: 'recent-tickets',
        title: 'Tickets Recentes',
        type: 'table',
        cols: 2,
        rows: 2
    }];

    public loadPieData(chart: DashboardWidget, projectId: number): Observable<PieChartData> {
        return this.dashboardApi.loadPieDashboard(this.toDashboardType(chart.id), projectId)
                              .pipe(
                                    map(response => this.toPieChartData(response)),
                                    retry(1), 
                                    startWith(this.getLoadingPieChartData()),
                                    catchError(error =>{
                                        console.log("Error requesting data", error);
                                        return of(this.getErrorPieChartData());
                                    }),
                                    retry({
                                        delay: (error, retryCount) => {
                                            // Retenta apenas para erros de rede/timeout
                                            if (error.status && error.status >= 400 && error.status < 500) {
                                                throw error; // Não retenta para erros 4xx
                                            }
  
                                            console.log(`Tentativa ${retryCount} falhou. Retentando...`);
                                            return timer(1000 * retryCount); // Delay crescente
                                        }
                                    })
                                );
    }

    public loadKpiData(chart: DashboardWidget, projectId: number): Observable<KpiData> {
        return this.dashboardApi.loadKpiDashboard(this.toDashboardType(chart.id), projectId)
                              .pipe(
                                    map(response => this.toKpiData(response)),
                                    retry(1), 
                                    startWith(this.getLoadingKpiChartData()),
                                    catchError(error =>{
                                        console.log("Error requesting data", error);
                                        return of(this.getErrorKpiChartData());
                                    }),
                                    retry({
                                        delay: (error, retryCount) => {
                                            // Retenta apenas para erros de rede/timeout
                                            if (error.status && error.status >= 400 && error.status < 500) {
                                                throw error; // Não retenta para erros 4xx
                                            }
  
                                            console.log(`Tentativa ${retryCount} falhou. Retentando...`);
                                            return timer(1000 * retryCount); // Delay crescente
                                        }
                                    })
                                );
    }

    public loadTableData(chart: DashboardWidget, projectId: number): Observable<TableChartData> {
        return this.dashboardApi.loadTableDashboard(this.toDashboardType(chart.id), projectId)
                              .pipe(
                                    map(response => this.toTableChartData(response)),
                                    retry(1), 
                                    startWith(this.getLoadingTableChartData()),
                                    catchError(error =>{
                                        console.log("Error requesting data", error);
                                        return of(this.getErrorTableChartData());
                                    }),
                                    retry({
                                        delay: (error, retryCount) => {
                                            // Retenta apenas para erros de rede/timeout
                                            if (error.status && error.status >= 400 && error.status < 500) {
                                                throw error; // Não retenta para erros 4xx
                                            }
  
                                            console.log(`Tentativa ${retryCount} falhou. Retentando...`);
                                            return timer(1000 * retryCount); // Delay crescente
                                        }
                                    })
                                );
    }

    private toDashboardType(widgetId: string): DashboardType {
        return widgetId.replaceAll('-', '_').toUpperCase() as DashboardType;
    }

    private toPieChartData(response: PieChartDataResponse): PieChartData {
        return {
            labels: response.labels ?? [],
            datasets: (response.datasets ?? []).map(dataset => ({
                data: dataset.data ?? [],
                backgroundColor: dataset.colors,
                label: dataset.label
            }))
        };
    }

    private toKpiData(response: KpiDataResponse): KpiData {
        return {
            total: response.total ?? 0,
            perStatus: new Map(Object.entries(response.perStatus ?? {}))
        };
    }

    private toTableChartData(response: TableDataResponse): TableChartData {
        return {
            columns: response.columns ?? [],
            rows: (response.rows ?? []).map(row => ({ data: row.data ?? [] }))
        };
    }

    private getLoadingPieChartData(): PieChartData {
        return {
            labels: ['Carregando...'],
            datasets: [{
            data: [100],
            backgroundColor: ['#e0e0e0'],
            label: 'Carregando dados'
            }]
        };
    }

    private getLoadingTableChartData(): TableChartData {
        return { columns: [], rows: [] };
    }


    private getLoadingKpiChartData(): KpiData {
        return { total: 0, perStatus: new Map() };
    }

    private getErrorTableChartData(): TableChartData {
        return { columns: [], rows: [] };
    }

    private getErrorKpiChartData(): KpiData {
        return { total: 0, perStatus: new Map() };
    }


    private getErrorPieChartData(): PieChartData {
        return {
            labels: ['Erro ao carregar', 'Tente novamente'],
            datasets: [{
            data: [70, 30],
            backgroundColor: ['#ff6b6b', '#ffe66d'],
            label: 'Status'
            }]
        };
    }
}