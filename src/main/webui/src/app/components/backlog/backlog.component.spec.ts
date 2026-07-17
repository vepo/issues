import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { BacklogComponent } from './backlog.component';
import { BacklogService, BacklogTicket } from '../../services/backlog.service';
import { AuthService } from '../../services/auth.service';
import { TranslocoService } from '@jsverse/transloco';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('BacklogComponent', () => {
  let component: BacklogComponent;
  let fixture: ComponentFixture<BacklogComponent>;
  let backlogService: jasmine.SpyObj<BacklogService>;
  let authService: jasmine.SpyObj<AuthService>;

  const ticketA: BacklogTicket = {
    id: 1,
    identifier: 'ISS-1',
    title: 'First',
    statusId: 10,
    statusName: 'TODO',
    priority: 'HIGH',
    assigneeId: 2,
    assigneeName: 'Alice',
    backlogRank: 1,
  };

  const ticketB: BacklogTicket = {
    id: 2,
    identifier: 'ISS-2',
    title: 'Second',
    statusId: 10,
    statusName: 'TODO',
    priority: 'MEDIUM',
    assigneeId: null as unknown as number,
    assigneeName: null as unknown as string,
    backlogRank: 2,
  };

  const page0 = {
    items: [ticketA, ticketB],
    total: 3,
    page: 0,
    size: 20,
    hasMore: true,
  };

  const page1 = {
    items: [{
      id: 3,
      identifier: 'ISS-3',
      title: 'Third',
      statusId: 11,
      statusName: 'IN_PROGRESS',
      priority: 'LOW',
      assigneeId: null as unknown as number,
      assigneeName: null as unknown as string,
      backlogRank: 3,
    }],
    total: 3,
    page: 1,
    size: 20,
    hasMore: false,
  };

  beforeEach(async () => {
    backlogService = jasmine.createSpyObj('BacklogService', ['list', 'reorder']);
    backlogService.list.and.callFake((_: number, page: number) => of(page === 0 ? page0 : page1));
    backlogService.reorder.and.returnValue(of(ticketA));

    authService = jasmine.createSpyObj('AuthService', ['hasRole']);
    authService.hasRole.and.callFake((role: string) => role === 'project-manager');

    await TestBed.configureTestingModule({
      imports: [
        BacklogComponent,
        createTranslocoTestingModule(
          { backlog: { title: 'Backlog', subtitle: 'Tickets ordenados por rank de planejamento (não por prioridade)' } },
          { backlog: { title: 'Backlog', subtitle: 'Tickets ordered by planning rank (not priority)' } },
        ),
      ],
      providers: [
        provideRouter([]),
        { provide: BacklogService, useValue: backlogService },
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ project: { id: 42, name: 'Demo' } }),
            snapshot: { paramMap: convertToParamMap({ projectId: '42' }) },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BacklogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load first page on init', () => {
    expect(backlogService.list).toHaveBeenCalledWith(42, 0, 20);
    expect(component.tickets.length).toBe(2);
    expect(component.hasMore).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Backlog');
    expect(fixture.nativeElement.textContent).toContain('ISS-1');
  });

  it('should rerender backlog guidance immediately when locale changes from Portuguese to English', async () => {
    expect(fixture.nativeElement.textContent).toContain('Tickets ordenados por rank de planejamento');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Tickets ordered by planning rank');
    expect(fixture.nativeElement.textContent).not.toContain('Tickets ordenados por rank de planejamento');
  });

  it('should load next page on scroll near bottom', () => {
    const target = {
      scrollTop: 100,
      clientHeight: 200,
      scrollHeight: 320,
    };
    component.onScroll({ target } as unknown as Event);
    expect(backlogService.list).toHaveBeenCalledWith(42, 1, 20);
    expect(component.tickets.length).toBe(3);
    expect(component.hasMore).toBeFalse();
  });

  it('should call reorder on drag drop for PM', () => {
    const event = {
      previousIndex: 1,
      currentIndex: 0,
    } as CdkDragDrop<BacklogTicket[]>;
    component.drop(event);
    expect(backlogService.reorder).toHaveBeenCalledWith(42, {
      ticketId: 2,
      beforeTicketId: 1,
    });
  });

  it('should not reorder when user cannot reorder', () => {
    authService.hasRole.and.returnValue(false);
    backlogService.reorder.calls.reset();
    component.drop({ previousIndex: 1, currentIndex: 0 } as CdkDragDrop<BacklogTicket[]>);
    expect(backlogService.reorder).not.toHaveBeenCalled();
  });
});
