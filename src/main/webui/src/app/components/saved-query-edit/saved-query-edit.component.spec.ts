import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { SavedQueryService } from '../../services/saved-query.service';
import { SavedQueryEditComponent } from './saved-query-edit.component';

describe('SavedQueryEditComponent', () => {
  let fixture: ComponentFixture<SavedQueryEditComponent>;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;

  beforeEach(async () => {
    savedQueryService = jasmine.createSpyObj('SavedQueryService', ['list', 'create', 'update', 'delete']);
    savedQueryService.list.and.returnValue(of([]));
    savedQueryService.create.and.returnValue(of({
      id: 2,
      slug: 'new-slug',
      name: 'New',
      query: 'status = "TODO"',
      showAtHome: false,
      ownerId: 1,
      ownerName: 'User',
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01'
    }));

    await TestBed.configureTestingModule({
      imports: [SavedQueryEditComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'new' },
              queryParamMap: { get: () => 'status = "TODO"' }
            }
          }
        },
        { provide: SavedQueryService, useValue: savedQueryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SavedQueryEditComponent);
    fixture.detectChanges();
  });

  it('should prefill query from query params on new form', () => {
    expect(fixture.componentInstance.queryText).toBe('status = "TODO"');
  });

  it('should create saved query on save', () => {
    fixture.componentInstance.name = 'New query';
    fixture.componentInstance.queryText = 'status = "TODO"';
    fixture.componentInstance.save();
    expect(savedQueryService.create).toHaveBeenCalled();
  });
});
