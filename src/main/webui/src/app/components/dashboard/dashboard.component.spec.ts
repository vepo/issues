import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { ToastService } from '../../services/toast.service';
import { AvailablesDashboards } from './availables.dashboards';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: jasmine.SpyObj<DashboardService>;
  let toast: jasmine.SpyObj<ToastService>;
  let availableDashboards: jasmine.SpyObj<AvailablesDashboards>;

  const project = {
    id: 7,
    name: 'Issues',
    prefix: 'ISS',
    description: '',
    workflow: { id: 1, name: 'Agile' },
    owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
    securityLevel: 'INTERNAL' as const,
    ticketTemplate: { enabled: false },
    phaseTemplate: { deliverables: [] },
    prefixLocked: false,
  };

  const allWidgets = [
    { id: 'tickets-by-day', title: 'Tickets por Dia', type: 'chart' as const, chartType: 'bar' as const, cols: 1, rows: 1 },
    { id: 'tickets-by-status', title: 'Tickets por Status', type: 'chart' as const, chartType: 'pie' as const, cols: 1, rows: 1 },
    { id: 'tickets-by-priority', title: 'Tickets por Prioridade', type: 'chart' as const, chartType: 'pie' as const, cols: 1, rows: 1 },
    { id: 'performance-kpi', title: 'Tickets por status', type: 'kpi' as const, cols: 1, rows: 1 },
    { id: 'recent-tickets', title: 'Tickets Recentes', type: 'table' as const, cols: 2, rows: 2 },
  ];

  beforeEach(async () => {
    dashboardService = jasmine.createSpyObj('DashboardService', [
      'getLayout',
      'saveLayout',
      'loadPie',
      'loadTable',
      'loadKpi',
    ]);
    dashboardService.getLayout.and.returnValue(of({
      widgetIds: ['tickets-by-status', 'recent-tickets'],
    }));
    dashboardService.saveLayout.and.returnValue(of({
      widgetIds: ['recent-tickets', 'tickets-by-status'],
    }));

    toast = jasmine.createSpyObj('ToastService', ['success', 'error']);

    availableDashboards = jasmine.createSpyObj(
      'AvailablesDashboards',
      ['loadChartData', 'loadTableData', 'loadKpiData'],
      { all: allWidgets },
    );
    availableDashboards.loadChartData.and.returnValue(of({ kind: 'empty' }));
    availableDashboards.loadTableData.and.returnValue(of({ kind: 'empty' }));
    availableDashboards.loadKpiData.and.returnValue(of({ kind: 'loading' }));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { data: of({ project }) } },
        { provide: DashboardService, useValue: dashboardService },
        { provide: ToastService, useValue: toast },
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

    expect(dashboardService.getLayout).toHaveBeenCalledWith(7);
    expect(component.pageLayout.widgets.map((widget) => widget.id)).toEqual([
      'tickets-by-status',
      'recent-tickets',
    ]);
    expect(localStorage.getItem).not.toHaveBeenCalled();
    expect(localStorage.setItem).not.toHaveBeenCalled();
  });

  it('should autosave layout with toast on success', () => {
    component.pageLayout.widgets = [
      { id: 'recent-tickets', title: 'Tickets Recentes', type: 'table', cols: 2, rows: 2 },
      { id: 'tickets-by-status', title: 'Tickets por Status', type: 'chart', chartType: 'pie', cols: 1, rows: 1 },
    ];

    component.saveDashboardConfig();

    expect(dashboardService.saveLayout).toHaveBeenCalledWith(7, ['recent-tickets', 'tickets-by-status']);
    expect(toast.success).toHaveBeenCalledWith('Layout salvo');
  });

  it('should toast error when autosave fails', () => {
    dashboardService.saveLayout.and.returnValue(throwError(() => new Error('fail')));
    component.saveDashboardConfig();
    expect(toast.error).toHaveBeenCalledWith('Não foi possível salvar o layout');
  });

  it('should use Concluir label while editing', () => {
    expect(component.isEditing).toBeFalse();
    component.toggleEdit();
    expect(component.isEditing).toBeTrue();
    fixture.detectChanges();
    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('.page-header__actions .btn') as NodeListOf<HTMLElement>,
    );
    const editButton = buttons.find((button) => (button.textContent ?? '').includes('Concluir'));
    expect(editButton).toBeTruthy();
  });

  it('should expose tickets-by-day as bar in catalog', () => {
    const day = allWidgets.find((widget) => widget.id === 'tickets-by-day');
    expect(day?.chartType).toBe('bar');
  });
});
