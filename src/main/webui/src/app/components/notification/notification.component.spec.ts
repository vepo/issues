import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
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
      'unreadCount',
      'markAllAsRead',
      'disconnect',
      'markAsRead',
    ]);
    notificationService.listen.and.returnValue(liveEvents.asObservable());
    notificationService.reconnected.and.returnValue(reconnected.asObservable());
    notificationService.list.and.callFake((page: number) => of(page === 0 ? page0 : page1));
    notificationService.unreadCount.and.returnValue(of({ unread: 3 }));
    notificationService.markAllAsRead.and.returnValue(of({ updated: 3, unread: 0 }));

    await TestBed.configureTestingModule({
      imports: [
        NotificationComponent,
        TranslocoTestingModule.forRoot({
          langs: { pt: {} },
          translocoConfig: { availableLangs: ['pt'], defaultLang: 'pt' },
        }),
      ],
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
    expect(notificationService.unreadCount).toHaveBeenCalled();
    expect(component.events.length).toBe(2);
    expect(component.hasMore).toBeTrue();
    expect(component.unread).toBe(3);
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

  it('should reload page 0 and unread after SSE reconnect', () => {
    notificationService.list.calls.reset();
    notificationService.unreadCount.calls.reset();
    notificationService.list.and.returnValue(of(page0));
    notificationService.unreadCount.and.returnValue(of({ unread: 5 }));
    reconnected.next();
    expect(notificationService.list).toHaveBeenCalledWith(0, 20);
    expect(notificationService.unreadCount).toHaveBeenCalled();
    expect(component.unread).toBe(5);
  });

  it('should show badge count, 99+, and hide when zero', () => {
    component.unread = 3;
    expect(component.badgeLabel()).toBe('3');
    component.unread = 100;
    expect(component.badgeLabel()).toBe('99+');
    component.unread = 0;
    expect(component.badgeLabel()).toBeNull();
  });

  it('should mark all as read then reload list and unread', () => {
    notificationService.list.calls.reset();
    notificationService.unreadCount.calls.reset();
    notificationService.unreadCount.and.returnValue(of({ unread: 0 }));
    notificationService.list.and.returnValue(of({ ...page0, items: page0.items.map(i => ({ ...i, read: true })) }));

    component.markAllAsRead(new Event('click'));

    expect(notificationService.markAllAsRead).toHaveBeenCalled();
    expect(notificationService.unreadCount).toHaveBeenCalled();
    expect(notificationService.list).toHaveBeenCalledWith(0, 20);
    expect(component.unread).toBe(0);
  });
});
