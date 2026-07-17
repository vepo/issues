import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { SavedQueryService } from '../../services/saved-query.service';
import { TicketExportService } from '../../services/ticket-export.service';
import { SavedQueryViewComponent } from './saved-query-view.component';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('SavedQueryViewComponent', () => {
  let fixture: ComponentFixture<SavedQueryViewComponent>;
  let router: Router;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;
  let ticketExportService: jasmine.SpyObj<TicketExportService>;

  function buttonWithText(text: string, root: ParentNode = document): HTMLButtonElement {
    const button = Array.from(root.querySelectorAll('button')).find(candidate => candidate.textContent?.trim() === text);
    if (button == null) {
      throw new Error(`button "${text}" should be rendered`);
    }
    return button as HTMLButtonElement;
  }

  function openExportMenu(): HTMLButtonElement {
    const trigger = buttonWithText('Exportar', fixture.nativeElement);
    trigger.click();
    fixture.detectChanges();
    return trigger;
  }

  function chooseExportFormat(format: 'CSV' | 'JSON'): HTMLButtonElement {
    const trigger = openExportMenu();
    buttonWithText(format).click();
    fixture.detectChanges();
    return trigger;
  }

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
    ticketExportService = jasmine.createSpyObj('TicketExportService', ['download']);
    ticketExportService.download.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [
        createTranslocoTestingModule(
          {
            ticketExport: {
              action: 'Exportar',
              errorGeneral: 'Não foi possível exportar os tickets.',
            },
          },
          {
            ticketExport: {
              action: 'Export',
              errorGeneral: 'Could not export tickets.',
            },
          },
        ),
        SavedQueryViewComponent,
      ],
      providers: [
        provideRouter([{ path: 'search/q/:slug', component: SavedQueryViewComponent }]),
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
        { provide: SavedQueryService, useValue: savedQueryService },
        { provide: TicketExportService, useValue: ticketExportService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SavedQueryViewComponent);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should load saved query by slug', () => {
    expect(savedQueryService.findBySlug).toHaveBeenCalledWith('abc');
    expect(fixture.componentInstance.savedQuery?.name).toBe('Shared');
  });

  it('should show clone for non-owner', () => {
    expect(fixture.componentInstance.isOwner).toBeFalse();
  });

  it('should place one CSV and JSON export menu beside the existing saved-query actions', () => {
    const actionLabels = Array.from(
      fixture.nativeElement.querySelectorAll(
        '.page-header__actions button, .page-header__actions a',
      ) as NodeListOf<Element>,
    ).map(action => action.textContent?.trim());

    expect(actionLabels).toEqual(['Copiar link', 'Clonar', 'Exportar']);

    openExportMenu();
    const formats = Array.from(document.querySelectorAll('[role="menuitem"]')).map(item => item.textContent?.trim());
    expect(formats).toEqual(['CSV', 'JSON']);
  });

  it('should export CSV and JSON by the slug loaded from the saved-query route', () => {
    chooseExportFormat('CSV');
    chooseExportFormat('JSON');

    expect(ticketExportService.download.calls.allArgs()).toEqual([
      [{ source: 'saved', savedQuerySlug: 'abc' }, 'CSV'],
      [{ source: 'saved', savedQuerySlug: 'abc' }, 'JSON'],
    ]);
  });

  it('should prevent duplicate saved-query export while loading and restore the action after success', () => {
    const exportCompletion = new Subject<void>();
    ticketExportService.download.and.returnValue(exportCompletion);

    const trigger = chooseExportFormat('JSON');
    trigger.click();
    fixture.detectChanges();

    expect(trigger.disabled).toBeTrue();
    expect(ticketExportService.download).toHaveBeenCalledTimes(1);

    exportCompletion.next();
    exportCompletion.complete();
    fixture.detectChanges();

    expect(trigger.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('should show saved-query export failure feedback and restore the action', () => {
    ticketExportService.download.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Export failed' })),
    );

    const trigger = chooseExportFormat('CSV');
    const feedback = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;

    expect(feedback?.textContent).toContain('Não foi possível exportar os tickets.');
    expect(trigger.disabled).toBeFalse();
  });

  it('should rerender saved-query export copy from Portuguese to English without changing route', async () => {
    await router.navigateByUrl('/search/q/abc');
    fixture.detectChanges();
    const urlBeforeLocaleChange = router.url;
    openExportMenu();
    expect(fixture.nativeElement.textContent).toContain('Exportar');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Export');
    expect(fixture.nativeElement.textContent).not.toContain('Exportar');
    expect(router.url).toBe(urlBeforeLocaleChange);
  });
});
