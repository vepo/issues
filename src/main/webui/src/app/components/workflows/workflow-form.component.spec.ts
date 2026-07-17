import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { WorkflowFormComponent } from './workflow-form.component';
import { TranslocoService } from '@jsverse/transloco';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('WorkflowFormComponent', () => {
  let fixture: ComponentFixture<WorkflowFormComponent>;
  let component: WorkflowFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        WorkflowFormComponent,
        NoopAnimationsModule,
        createTranslocoTestingModule(
          {
            workflow: {
              initialStatus: 'Status inicial',
              addStatus: 'Adicionar status',
              addFinishStatus: 'Adicionar status de conclusão',
            },
          },
          {
            workflow: {
              initialStatus: 'Initial status',
              addStatus: 'Add status',
              addFinishStatus: 'Add finish status',
            },
          },
        ),
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkflowFormComponent);
    component = fixture.componentInstance;
  });

  it('should unlock status editing in edit mode and emit statuses on submit', () => {
    component.mode = 'edit';
    component.initialWorkflow = {
      id: 1,
      name: 'Flow Name',
      statuses: ['Open', 'Doing', 'Done'],
      start: 'Open',
      phaseStart: null,
      transitions: [{ from: 'Open', to: 'Doing' }, { from: 'Doing', to: 'Done' }],
      finishStatuses: [],
      wipLimits: []
    } as never;
    fixture.detectChanges();

    expect(component.statuses.at(0).enabled).toBeTrue();
    expect(component.statusNames()).toEqual(['Open', 'Doing', 'Done']);

    const emitted: unknown[] = [];
    component.submitted.subscribe(value => emitted.push(value));
    component.submit();

    expect(emitted.length).toBe(1);
    const value = emitted[0] as { statuses: string[]; statusReplacements: unknown[] };
    expect(value.statuses).toEqual(['Open', 'Doing', 'Done']);
    expect(value.statusReplacements).toEqual([]);
  });

  it('should rerender workflow labels immediately when locale changes from Portuguese to English', async () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Status inicial');
    expect(fixture.nativeElement.textContent).toContain('Adicionar status');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Initial status');
    expect(fixture.nativeElement.textContent).toContain('Add status');
    expect(fixture.nativeElement.textContent).not.toContain('Adicionar status');
  });

  it('should require replacement when removing an existing status in edit mode', () => {
    component.mode = 'edit';
    component.initialWorkflow = {
      id: 2,
      name: 'Remap Flow',
      statuses: ['Open', 'Doing', 'Done'],
      start: 'Open',
      phaseStart: null,
      transitions: [{ from: 'Open', to: 'Doing' }, { from: 'Doing', to: 'Done' }],
      finishStatuses: [],
      wipLimits: []
    } as never;
    fixture.detectChanges();

    component.removeStatus(1);
    expect(component.pendingRemoval?.name).toBe('Doing');
    expect(component.statuses.length).toBe(3);

    component.pendingRemoval!.replacement = 'Done';
    component.confirmPendingRemoval();

    expect(component.pendingRemoval).toBeNull();
    expect(component.statuses.length).toBe(2);
    expect(component.statusNames()).toEqual(['Open', 'Done']);
    expect(component.statusReplacements).toEqual([{ from: 'Doing', to: 'Done' }]);

    const emitted: unknown[] = [];
    component.submitted.subscribe(value => emitted.push(value));
    component.submit();
    const value = emitted[0] as { statusReplacements: { from: string; to: string }[] };
    expect(value.statusReplacements).toEqual([{ from: 'Doing', to: 'Done' }]);
  });

  it('should treat single rename as automatic status replacement', () => {
    component.mode = 'edit';
    component.initialWorkflow = {
      id: 3,
      name: 'Rename Flow',
      statuses: ['Open', 'Doing'],
      start: 'Open',
      phaseStart: null,
      transitions: [{ from: 'Open', to: 'Doing' }],
      finishStatuses: [],
      wipLimits: []
    } as never;
    fixture.detectChanges();

    component.statuses.at(1).setValue('Review');
    const emitted: unknown[] = [];
    component.submitted.subscribe(value => emitted.push(value));
    component.submit();
    const value = emitted[0] as { statuses: string[]; statusReplacements: { from: string; to: string }[] };
    expect(value.statuses).toEqual(['Open', 'Review']);
    expect(value.statusReplacements).toEqual([{ from: 'Doing', to: 'Review' }]);
  });
});
