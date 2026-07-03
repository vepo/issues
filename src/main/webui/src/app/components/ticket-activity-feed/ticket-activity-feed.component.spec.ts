import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TicketActivityFeedComponent } from './ticket-activity-feed.component';
import { ActivityItem } from './activity-feed.utils';

describe('TicketActivityFeedComponent', () => {
  let fixture: ComponentFixture<TicketActivityFeedComponent>;

  const items: ActivityItem[] = [
    {
      kind: 'change',
      id: 1,
      timestamp: 1000,
      userName: 'Alice',
      action: 'CREATED',
    },
    {
      kind: 'comment',
      id: 2,
      timestamp: 2000,
      userName: 'Bob',
      content: '<p>Test</p>',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketActivityFeedComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketActivityFeedComponent);
    fixture.componentRef.setInput('items', items);
    fixture.detectChanges();
  });

  it('should render change and comment rows', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('.activity-item').length).toBe(2);
    expect(element.textContent).toContain('Alice');
    expect(element.textContent).toContain('Bob');
    expect(element.textContent).toContain('comentou');
  });

  it('should show empty state when no items', () => {
    fixture.componentRef.setInput('items', []);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.empty-state')).toBeTruthy();
  });
});
