import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { phaseStatusLabel, priorityLabel, ticketTypeLabel } from './system-labels';

@Component({
  template: '<span class="priority-label">{{ criticalPriorityLabel() }}</span>',
})
class SystemLabelHostComponent {
  criticalPriorityLabel(): string {
    return priorityLabel('CRITICAL');
  }
}

describe('system-labels', () => {
  it('should translate priority labels', () => {
    expect(priorityLabel('CRITICAL')).toContain('Crít');
    expect(priorityLabel('HIGH')).toBeTruthy();
    expect(priorityLabel('MEDIUM')).toBeTruthy();
    expect(priorityLabel('LOW')).toBeTruthy();
  });

  it('should translate phase status labels', () => {
    expect(phaseStatusLabel('PLANNED')).toBeTruthy();
    expect(phaseStatusLabel('ACTIVE')).toBeTruthy();
    expect(phaseStatusLabel('COMPLETED')).toBeTruthy();
  });

  it('should translate ticket type labels', () => {
    expect(ticketTypeLabel('EPIC')).toBeTruthy();
    expect(ticketTypeLabel('STORY')).toBeTruthy();
    expect(ticketTypeLabel('TASK')).toBeTruthy();
  });

  it('should rerender a priority system label through its stable runtime key', async () => {
    await TestBed.configureTestingModule({
      imports: [
        SystemLabelHostComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            pt: {
              system: {
                priority: {
                  critical: 'Crítica',
                },
              },
            },
            en: {
              system: {
                priority: {
                  critical: 'Critical',
                },
              },
            },
          },
          translocoConfig: {
            availableLangs: ['pt', 'en'],
            defaultLang: 'pt',
            reRenderOnLangChange: true,
          },
          preloadLangs: true,
        }),
      ],
    }).compileComponents();
    const fixture: ComponentFixture<SystemLabelHostComponent> =
      TestBed.createComponent(SystemLabelHostComponent);
    const transloco = TestBed.inject(TranslocoService);
    const currentPath = window.location.pathname;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Crítica');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Critical');
    expect(fixture.nativeElement.textContent).not.toContain('Crítica');
    expect(window.location.pathname).toBe(currentPath);
    transloco.setActiveLang('pt');
  });
});
