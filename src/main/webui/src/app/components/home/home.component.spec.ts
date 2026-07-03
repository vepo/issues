import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { HomeService } from '../../services/home.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let homeService: jasmine.SpyObj<HomeService>;

  beforeEach(async () => {
    homeService = jasmine.createSpyObj('HomeService', ['listCurrentTickets', 'listAssignedTickets', 'listActivity']);
    homeService.listCurrentTickets.and.returnValue(of([]));
    homeService.listAssignedTickets.and.returnValue(of([]));
    homeService.listActivity.and.returnValue(of([]));

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
  });
});
