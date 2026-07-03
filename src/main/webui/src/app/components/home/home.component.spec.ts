import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { HomeService } from '../../services/home.service';
import { Ticket } from '../../services/ticket.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let homeService: jasmine.SpyObj<HomeService>;

  beforeEach(async () => {
    homeService = jasmine.createSpyObj('HomeService', [
      'listCurrentTickets',
      'listAssignedTickets',
      'listActivity',
      'listSavedQuerySections'
    ]);
    homeService.listCurrentTickets.and.returnValue(of([]));
    homeService.listAssignedTickets.and.returnValue(of([]));
    homeService.listActivity.and.returnValue(of([]));
    homeService.listSavedQuerySections.and.returnValue(of([
      {
        savedQuery: {
          id: 1,
          slug: 'home-q',
          name: 'Home query',
          query: 'status = "TODO"',
          showAtHome: true,
          ownerId: 1,
          ownerName: 'User',
          createdAt: '2026-01-01',
          updatedAt: '2026-01-01'
        },
        tickets: [{ id: 1, identifier: 'ISS-1', title: 'Ticket', priority: 'MEDIUM' } as Ticket]
      }
    ]));

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        { provide: HomeService, useValue: homeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
  });

  it('loads home sections on init', () => {
    expect(homeService.listCurrentTickets).toHaveBeenCalled();
    expect(homeService.listAssignedTickets).toHaveBeenCalled();
    expect(homeService.listActivity).toHaveBeenCalled();
    expect(homeService.listSavedQuerySections).toHaveBeenCalled();
  });

  it('renders saved query sections with tickets', () => {
    expect(fixture.componentInstance.savedQuerySections.length).toBe(1);
    expect(fixture.componentInstance.savedQuerySections[0].savedQuery.name).toBe('Home query');
  });
});
