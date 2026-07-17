import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { BurndownComponent } from './burndown.component';
import { BurndownService } from '../../services/burndown.service';
import { PhaseService } from '../../services/phase.service';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

describe('BurndownComponent', () => {
  let component: BurndownComponent;
  let fixture: ComponentFixture<BurndownComponent>;
  let burndownService: jasmine.SpyObj<BurndownService>;
  let phaseService: jasmine.SpyObj<PhaseService>;

  beforeEach(async () => {
    burndownService = jasmine.createSpyObj('BurndownService', ['load']);
    phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    phaseService.list.and.returnValue(of([
      {
        id: 1,
        projectId: 7,
        name: 'Sprint A',
        status: 'ACTIVE',
        startDate: null,
        endDate: null,
        createdAt: '2026-07-01T00:00:00',
        deliverables: [],
      },
      {
        id: 2,
        projectId: 7,
        name: 'Sprint B',
        status: 'PLANNED',
        startDate: '2026-07-01',
        endDate: '2026-07-15',
        createdAt: '2026-07-01T00:00:00',
        deliverables: [],
      },
    ]));

    await TestBed.configureTestingModule({
      imports: [
        BurndownComponent,
        createTranslocoTestingModule(
          { burndown: { selectPhaseAria: 'Selecionar fase' } },
          { burndown: { selectPhaseAria: 'Select phase' } },
        ),
      ],
      providers: [
        provideCharts(withDefaultRegisterables()),
        { provide: BurndownService, useValue: burndownService },
        { provide: PhaseService, useValue: phaseService },
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ project: { id: 7, name: 'Demo' } }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BurndownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should default to active phase', () => {
    expect(component.selectedPhaseId).toBe(1);
  });

  it('should disable chart control when phase dates are incomplete', () => {
    expect(component.chartEnabled).toBeFalse();
    expect(component.datesIncompleteTooltip).toContain('Defina data de início e fim');
  });

  it('should enable chart when phase has start and end', () => {
    component.onPhaseChange(2);
    expect(component.chartEnabled).toBeTrue();
  });

  it('should load warnings and chart data when showing burndown', () => {
    component.onPhaseChange(2);
    burndownService.load.and.returnValue(of({
      phaseId: 2,
      phaseName: 'Sprint B',
      startDate: '2026-07-01',
      endDate: '2026-07-15',
      datesComplete: true,
      series: [
        { date: '2026-07-01', ideal: 10, remaining: 10 },
        { date: '2026-07-02', ideal: 5, remaining: 8 },
      ],
      warnings: [{ ticketId: 9, identifier: 'PRJ-9', code: 'MISSING_STORY_POINTS' }],
      commitmentPoints: 10,
      remainingPoints: 8,
    }));

    component.showBurndown();

    expect(burndownService.load).toHaveBeenCalledWith(7, 2);
    expect(component.burndown?.warnings?.length).toBe(1);
    expect(component.chartVisible).toBeTrue();
    expect(component.lineChartData.labels).toEqual(['2026-07-01', '2026-07-02']);
    expect(component.lineChartData.datasets[0].data).toEqual([10, 5]);
    expect(component.lineChartData.datasets[1].data).toEqual([10, 8]);
  });
});
