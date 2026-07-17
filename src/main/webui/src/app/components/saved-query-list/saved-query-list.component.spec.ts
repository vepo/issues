import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
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
      imports: [
        SavedQueryListComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            pt: {
              search: {
                saved: {
                  title: 'Minhas consultas',
                  create: 'Nova consulta',
                  query: 'Consulta',
                  open: 'Abrir',
                },
              },
            },
            en: {
              search: {
                saved: {
                  title: 'My queries',
                  create: 'New query',
                  query: 'Query',
                  open: 'Open',
                },
              },
            },
          },
          translocoConfig: {
            availableLangs: ['pt', 'en'],
            defaultLang: 'pt',
            reRenderOnLangChange: true,
          },
          preloadLangs: true,
        }),
      ],
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

  it('should rerender search copy while preserving saved query content', async () => {
    const transloco = TestBed.inject(TranslocoService);
    expect(fixture.nativeElement.textContent).toContain('Minhas consultas');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My queries');
    expect(text).toContain('New query');
    expect(text).toContain('Query');
    expect(text).toContain('Open');
    expect(text).toContain('My query');
    expect(text).toContain('status = "TODO"');
    expect(text).not.toContain('Minhas consultas');
  });
});
