import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { SavedQueryService } from '../../services/saved-query.service';
import { SavedQueryViewComponent } from './saved-query-view.component';

describe('SavedQueryViewComponent', () => {
  let fixture: ComponentFixture<SavedQueryViewComponent>;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;

  beforeEach(async () => {
    savedQueryService = jasmine.createSpyObj('SavedQueryService', ['findBySlug', 'clone']);
    savedQueryService.findBySlug.and.returnValue(of({
      savedQuery: {
        id: 1,
        slug: 'abc',
        name: 'Shared',
        query: 'status = "TODO"',
        showAtHome: false,
        ownerId: 99,
        ownerName: 'Owner',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      },
      tickets: []
    }));
    savedQueryService.clone.and.returnValue(of({
      id: 2,
      slug: 'clone',
      name: 'Shared (cópia)',
      query: 'status = "TODO"',
      showAtHome: false,
      ownerId: 1,
      ownerName: 'Me',
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01'
    }));

    await TestBed.configureTestingModule({
      imports: [SavedQueryViewComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'abc' } }
          }
        },
        {
          provide: AuthService,
          useValue: { me: () => of({ id: 1, name: 'Me', email: 'me@test.dev' }) }
        },
        { provide: SavedQueryService, useValue: savedQueryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SavedQueryViewComponent);
    fixture.detectChanges();
  });

  it('should load saved query by slug', () => {
    expect(savedQueryService.findBySlug).toHaveBeenCalledWith('abc');
    expect(fixture.componentInstance.savedQuery?.name).toBe('Shared');
  });

  it('should show clone for non-owner', () => {
    expect(fixture.componentInstance.isOwner).toBeFalse();
  });
});
