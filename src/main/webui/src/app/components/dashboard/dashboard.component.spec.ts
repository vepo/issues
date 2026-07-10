import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { DashboardApi } from '../../generated/api/dashboard.service';
import { AvailablesDashboards } from './availables.dashboards';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardApi: jasmine.SpyObj<DashboardApi>;
  let availableDashboards: jasmine.SpyObj<AvailablesDashboards>;

  const project = {
    id: 7,
    name: 'Issues',
    prefix: 'ISS',
    description: '',
    workflow: { id: 1, name: 'Agile' },
    owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
    ticketTemplate: { enabled: false },
    phaseTemplate: { deliverables: [] },
  };

  const allWidgets = [
    { id: 'tickets-by-day', title: 'Tickets por Dia', type: 'chart' as const, chartType: 'pie' as const, cols: 1, rows: 1 },
    { id: 'tickets-by-status', title: 'Tickets por Status', type: 'chart' as const, chartType: 'pie' as const, cols: 1, rows: 1 },
    { id: 'tickets-by-priority', title: 'Tickets por Prioridade', type: 'chart' as const, chartType: 'pie' as const, cols: 1, rows: 1 },
    { id: 'performance-kpi', title: 'KPIs de Performance', type: 'kpi' as const, cols: 1, rows: 1 },
    { id: 'recent-tickets', title: 'Tickets Recentes', type: 'table' as const, cols: 2, rows: 2 },
  ];

  beforeEach(async () => {
    dashboardApi = jasmine.createSpyObj('DashboardApi', [
      'getDashboardLayout',
      'saveDashboardLayout',
      'loadPieDashboard',
      'loadTableDashboard',
      'loadKpiDashboard',
    ]);
    dashboardApi.getDashboardLayout.and.returnValue(of({
      widgetIds: ['tickets-by-status', 'recent-tickets']
    }) as any);
    dashboardApi.saveDashboardLayout.and.returnValue(of({
      widgetIds: ['recent-tickets', 'tickets-by-status']
    }) as any);

    availableDashboards = jasmine.createSpyObj('AvailablesDashboards', [
      'loadPieData',
      'loadTableData',
      'loadKpiData',
    ], { all: allWidgets });
    availableDashboards.loadPieData.and.returnValue(of({ labels: [], datasets: [] }));
    availableDashboards.loadTableData.and.returnValue(of({ columns: [], rows: [] }));
    availableDashboards.loadKpiData.and.returnValue(of({ total: 0, perStatus: new Map() }));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ project }) },
        },
        { provide: DashboardApi, useValue: dashboardApi },
        { provide: AvailablesDashboards, useValue: availableDashboards },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load layout from server without using localStorage', () => {
    spyOn(localStorage, 'getItem');
    spyOn(localStorage, 'setItem');

    component.loadDashboardConfig();

    expect(dashboardApi.getDashboardLayout).toHaveBeenCalledWith(7);
    expect(component.pageLayout.widgets.map(widget => widget.id)).toEqual([
      'tickets-by-status',
      'recent-tickets',
    ]);
    expect(localStorage.getItem).not.toHaveBeenCalled();
    expect(localStorage.setItem).not.toHaveBeenCalled();
  });

  it('should save layout to server without using localStorage', () => {
    spyOn(localStorage, 'setItem');
    component.pageLayout.widgets = [
      { id: 'recent-tickets', title: 'Tickets Recentes', type: 'table', cols: 2, rows: 2 },
      { id: 'tickets-by-status', title: 'Tickets por Status', type: 'chart', chartType: 'pie', cols: 1, rows: 1 },
    ];

    component.saveDashboardConfig();

    expect(dashboardApi.saveDashboardLayout).toHaveBeenCalledWith(7, {
      widgetIds: ['recent-tickets', 'tickets-by-status'],
    });
    expect(localStorage.setItem).not.toHaveBeenCalled();
  });
});
