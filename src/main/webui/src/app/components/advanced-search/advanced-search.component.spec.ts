import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { of, Subject, throwError } from 'rxjs';
import { SavedQueryService } from '../../services/saved-query.service';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';
import { TicketExportService } from '../../services/ticket-export.service';
import { Ticket } from '../../services/ticket.service';
import { AdvancedSearchComponent } from './advanced-search.component';

describe('AdvancedSearchComponent', () => {
  let fixture: ComponentFixture<AdvancedSearchComponent>;
  let router: Router;
  let savedQueryService: jasmine.SpyObj<SavedQueryService>;
  let ticketExportService: jasmine.SpyObj<TicketExportService>;

  const sampleTickets = [
    { id: 1, identifier: 'ISS-1', title: 'First', description: 'First description', priority: 'HIGH' },
    { id: 2, identifier: 'ISS-2', title: 'Second', description: 'Second description', priority: 'LOW' }
  ] as Ticket[];

  function buttonWithText(text: string, root: ParentNode = document): HTMLButtonElement {
    const button = Array.from(root.querySelectorAll('button')).find(candidate => candidate.textContent?.trim() === text);
    if (button == null) {
      throw new Error(`button "${text}" should be rendered`);
    }
    return button as HTMLButtonElement;
  }

  function runSuccessfulQuery(query: string): void {
    savedQueryService.searchByQuery.and.returnValue(of(sampleTickets));
    fixture.componentInstance.queryText = query;
    fixture.componentInstance.runSearch();
    fixture.detectChanges();
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
    savedQueryService = jasmine.createSpyObj('SavedQueryService', ['searchByQuery']);
    savedQueryService.searchByQuery.and.returnValue(of([]));
    ticketExportService = jasmine.createSpyObj('TicketExportService', ['download']);
    ticketExportService.download.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [
        AdvancedSearchComponent,
        createTranslocoTestingModule(
          {
            advancedSearch: { hint: 'Ajuda', queryPlaceholder: 'Consulta', run: 'Executar', saveQuery: 'Salvar consulta' },
            ticketExport: {
              action: 'Exportar',
              errorLimit: 'A exportação excede o limite de 10.000 tickets.',
              errorGeneral: 'Não foi possível exportar os tickets.',
            },
          },
          {
            advancedSearch: { hint: 'Help', queryPlaceholder: 'Query', run: 'Run', saveQuery: 'Save query' },
            ticketExport: {
              action: 'Export',
              errorLimit: 'The export exceeds the 10,000 ticket limit.',
              errorGeneral: 'Could not export tickets.',
            },
          },
        ),
      ],
      providers: [
        provideRouter([{ path: 'search/advanced', component: AdvancedSearchComponent }]),
        { provide: SavedQueryService, useValue: savedQueryService },
        { provide: TicketExportService, useValue: ticketExportService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdvancedSearchComponent);
    router = TestBed.inject(Router);
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

  it('should offer export only after a successful valid advanced query execution', () => {
    expect(fixture.nativeElement.textContent).not.toContain('Exportar');

    savedQueryService.searchByQuery.and.returnValue(throwError(() => ({ error: { message: 'Syntax error' } })));
    fixture.componentInstance.queryText = 'bad query';
    fixture.componentInstance.runSearch();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Exportar');

    runSuccessfulQuery('status = "TODO"');
    openExportMenu();

    const formats = Array.from(document.querySelectorAll('[role="menuitem"]')).map(item => item.textContent?.trim());
    expect(formats).toEqual(['CSV', 'JSON']);
  });

  it('should export CSV and JSON from the last submitted query rather than unsent editor changes', () => {
    runSuccessfulQuery('status = "TODO" order by priority');
    fixture.componentInstance.queryText = 'status = "DONE"';
    fixture.detectChanges();

    chooseExportFormat('CSV');
    chooseExportFormat('JSON');

    expect(ticketExportService.download.calls.allArgs()).toEqual([
      [{ source: 'advanced', query: 'status = "TODO" order by priority' }, 'CSV'],
      [{ source: 'advanced', query: 'status = "TODO" order by priority' }, 'JSON'],
    ]);
  });

  it('should prevent duplicate advanced export while loading and restore the action after success', () => {
    const exportCompletion = new Subject<void>();
    ticketExportService.download.and.returnValue(exportCompletion);
    runSuccessfulQuery('priority = "HIGH"');

    const trigger = chooseExportFormat('CSV');
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

  [
    {
      status: 413,
      format: 'CSV' as const,
      message: 'A exportação excede o limite de 10.000 tickets.',
    },
    {
      status: 500,
      format: 'JSON' as const,
      message: 'Não foi possível exportar os tickets.',
    },
  ].forEach(({ status, format, message }) => {
    it(`should show export feedback and restore the action after ${status}`, () => {
      ticketExportService.download.and.returnValue(
        throwError(() => new HttpErrorResponse({ status, statusText: 'Export failed' })),
      );
      runSuccessfulQuery('assignee is not empty');

      const trigger = chooseExportFormat(format);
      const feedback = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;

      expect(feedback?.textContent).toContain(message);
      expect(trigger.disabled).toBeFalse();
    });
  });

  it('should rerender advanced export copy from Portuguese to English without changing route', async () => {
    await router.navigateByUrl('/search/advanced');
    runSuccessfulQuery('status = "TODO"');
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
