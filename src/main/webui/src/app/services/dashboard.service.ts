import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardApi } from '../generated/api/dashboard.service';
import { DashboardLayoutResponse } from '../generated/model/dashboardLayoutResponse';
import { DashboardType } from '../generated/model/dashboardType';
import { KpiDataResponse } from '../generated/model/kpiDataResponse';
import { PieChartDataResponse } from '../generated/model/pieChartDataResponse';
import { SaveDashboardLayoutRequest } from '../generated/model/saveDashboardLayoutRequest';
import { TableDataResponse } from '../generated/model/tableDataResponse';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(DashboardApi);

  getLayout(projectId: number): Observable<DashboardLayoutResponse> {
    return this.api.getDashboardLayout(projectId);
  }

  saveLayout(projectId: number, widgetIds: string[]): Observable<DashboardLayoutResponse> {
    return this.api.saveDashboardLayout(projectId, { widgetIds } as SaveDashboardLayoutRequest);
  }

  loadPie(projectId: number, widgetId: string): Observable<PieChartDataResponse> {
    return this.api.loadPieDashboard(this.toDashboardType(widgetId), projectId);
  }

  loadKpi(projectId: number, widgetId: string): Observable<KpiDataResponse> {
    return this.api.loadKpiDashboard(this.toDashboardType(widgetId), projectId);
  }

  loadTable(projectId: number, widgetId: string): Observable<TableDataResponse> {
    return this.api.loadTableDashboard(this.toDashboardType(widgetId), projectId);
  }

  private toDashboardType(widgetId: string): DashboardType {
    return widgetId.replaceAll('-', '_').toUpperCase() as DashboardType;
  }
}
