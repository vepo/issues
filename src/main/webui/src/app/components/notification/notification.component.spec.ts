import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { provideRouter } from '@angular/router';
import { NotificationComponent } from './notification.component';
import { NotificationService, UserNotification } from '../../services/notification.service';

describe('NotificationComponent', () => {
  let component: NotificationComponent;
  let fixture: ComponentFixture<NotificationComponent>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let liveEvents: Subject<UserNotification>;
  let reconnected: Subject<void>;

  const page0 = {
    items: [
      { id: 1, type: 'TICKET', read: false, content: 'First', ticketId: 10, timestamp: 2000 },
      { id: 2, type: 'TICKET', read: true, content: 'Second', ticketId: 11, timestamp: 1000 },
    ],
    total: 3,
    page: 0,
    size: 20,
    hasMore: true,
  };

  const page1 = {
    items: [
      { id: 3, type: 'TICKET', read: false, content: 'Third', ticketId: 12, timestamp: 500 },
    ],
    total: 3,
    page: 1,
    size: 20,
    hasMore: false,
  };

  beforeEach(async () => {
    liveEvents = new Subject<UserNotification>();
    reconnected = new Subject<void>();
    notificationService = jasmine.createSpyObj('NotificationService', [
      'connect',
      'listen',
      'reconnected',
      'list',
      'disconnect',
      'markAsRead',
    ]);
    notificationService.listen.and.returnValue(liveEvents.asObservable());
    notificationService.reconnected.and.returnValue(reconnected.asObservable());
    notificationService.list.and.callFake((page: number) => of(page === 0 ? page0 : page1));

    await TestBed.configureTestingModule({
      imports: [NotificationComponent],
      providers: [
        provideRouter([]),
        { provide: NotificationService, useValue: notificationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load first page', () => {
    expect(component).toBeTruthy();
    expect(notificationService.list).toHaveBeenCalledWith(0, 20);
    expect(component.events.length).toBe(2);
    expect(component.hasMore).toBeTrue();
  });

  it('should load next page on scroll near bottom', () => {
    const target = {
      scrollTop: 100,
      clientHeight: 200,
      scrollHeight: 320,
    } as unknown as HTMLElement;
    component.onScroll({ target } as unknown as Event);
    expect(notificationService.list).toHaveBeenCalledWith(1, 20);
    expect(component.events.length).toBe(3);
    expect(component.hasMore).toBeFalse();
  });

  it('should reload page 0 after SSE reconnect', () => {
    notificationService.list.calls.reset();
    notificationService.list.and.returnValue(of(page0));
    reconnected.next();
    expect(notificationService.list).toHaveBeenCalledWith(0, 20);
  });
});
