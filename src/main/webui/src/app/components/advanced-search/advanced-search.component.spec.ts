import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SavedQueryService } from '../../services/saved-query.service';
import { Ticket } from '../../services/ticket.service';
import { AdvancedSearchComponent } from './advanced-search.component';

describe('AdvancedSearchComponent', () => {
  let fixture: ComponentFixture<AdvancedSearchComponent>;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;

  const sampleTickets = [
    { id: 1, identifier: 'ISS-1', title: 'First', description: 'First description', priority: 'HIGH' },
    { id: 2, identifier: 'ISS-2', title: 'Second', description: 'Second description', priority: 'LOW' }
  ] as Ticket[];

  beforeEach(async () => {
    savedQueryService = jasmine.createSpyObj('SavedQueryService', ['searchByQuery']);
    savedQueryService.searchByQuery.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AdvancedSearchComponent],
      providers: [
        provideRouter([]),
        { provide: SavedQueryService, useValue: savedQueryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdvancedSearchComponent);
    fixture.detectChanges();
  });

  it('should execute query on runSearch', () => {
    fixture.componentInstance.queryText = 'status = "TODO"';
    fixture.componentInstance.runSearch();
    expect(savedQueryService.searchByQuery).toHaveBeenCalledWith('status = "TODO"');
  });

  it('should show error when query fails', () => {
    savedQueryService.searchByQuery.and.returnValue(throwError(() => ({ error: { message: 'Syntax error' } })));
    fixture.componentInstance.queryText = 'bad query';
    fixture.componentInstance.runSearch();
    expect(fixture.componentInstance.error).toBe('Syntax error');
  });

  it('should select first ticket after successful search', () => {
    savedQueryService.searchByQuery.and.returnValue(of(sampleTickets));
    fixture.componentInstance.queryText = 'status = "TODO"';
    fixture.componentInstance.runSearch();
    expect(fixture.componentInstance.selectedTicket).toEqual(sampleTickets[0]);
  });

  it('should update selected ticket when user selects another', () => {
    savedQueryService.searchByQuery.and.returnValue(of(sampleTickets));
    fixture.componentInstance.queryText = 'status = "TODO"';
    fixture.componentInstance.runSearch();
    fixture.componentInstance.selectTicket(sampleTickets[1]);
    expect(fixture.componentInstance.selectedTicket).toEqual(sampleTickets[1]);
    expect(fixture.componentInstance.isSelected(sampleTickets[1])).toBeTrue();
  });
});
