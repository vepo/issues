import { phaseStatusLabel, priorityLabel, ticketTypeLabel } from './system-labels';

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
});
