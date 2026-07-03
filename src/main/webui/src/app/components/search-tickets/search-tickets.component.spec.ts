import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { ProjectsService } from '../../services/projects.service';
import { StatusService } from '../../services/status.service';
import { TicketService } from '../../services/ticket.service';
import { SearchTicketsComponent } from './search-tickets.component';

describe('SearchTicketsComponent', () => {
  let component: SearchTicketsComponent;
  let fixture: ComponentFixture<SearchTicketsComponent>;
  let router: Router;
  let ticketService: jasmine.SpyObj<TicketService>;

  beforeEach(async () => {
    ticketService = jasmine.createSpyObj('TicketService', ['search']);
    ticketService.search.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [SearchTicketsComponent],
      providers: [
        provideRouter([{ path: 'search', component: SearchTicketsComponent }]),
        { provide: TicketService, useValue: ticketService },
        {
          provide: StatusService,
          useValue: { findAll: () => of([{ id: 2, name: 'in_progress' }]) }
        },
        { provide: ProjectsService, useValue: { findAll: () => of([]) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchTicketsComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should navigate with status query param when chip is selected', async () => {
    await router.navigate(['/search'], { queryParams: { q: 'bug' } });
    fixture.detectChanges();

    const navigateSpy = spyOn(router, 'navigate').and.callThrough();
    component.selectStatus(2);

    expect(navigateSpy).toHaveBeenCalledWith(['/search'], {
      queryParams: { q: 'bug', status: 2 }
    });
  });

  it('should omit status param when Todos is selected', async () => {
    await router.navigate(['/search'], { queryParams: { q: 'bug', status: 2 } });
    fixture.detectChanges();

    const navigateSpy = spyOn(router, 'navigate').and.callThrough();
    component.selectStatus(-1);

    expect(navigateSpy).toHaveBeenCalledWith(['/search'], {
      queryParams: { q: 'bug' }
    });
  });
});
