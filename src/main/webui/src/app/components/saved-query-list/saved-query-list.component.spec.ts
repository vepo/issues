import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SavedQueryService } from '../../services/saved-query.service';
import { SavedQueryListComponent } from './saved-query-list.component';

describe('SavedQueryListComponent', () => {
  let fixture: ComponentFixture<SavedQueryListComponent>;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;

  beforeEach(async () => {
    savedQueryService = jasmine.createSpyObj('SavedQueryService', ['list', 'delete']);
    savedQueryService.list.and.returnValue(of([
      {
        id: 1,
        slug: 'abc',
        name: 'My query',
        query: 'status = "TODO"',
        showAtHome: true,
        ownerId: 1,
        ownerName: 'User',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }
    ]));
    savedQueryService.delete.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [SavedQueryListComponent],
      providers: [
        provideRouter([]),
        { provide: SavedQueryService, useValue: savedQueryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SavedQueryListComponent);
    fixture.detectChanges();
  });

  it('should load saved queries on init', () => {
    expect(savedQueryService.list).toHaveBeenCalled();
    expect(fixture.componentInstance.queries.length).toBe(1);
  });

  it('should delete query after confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const query = fixture.componentInstance.queries[0];
    fixture.componentInstance.deleteQuery(query);
    expect(savedQueryService.delete).toHaveBeenCalledWith(1);
  });
});
