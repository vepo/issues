import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { of, Subject, throwError } from 'rxjs';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';
import { ProjectsService } from '../../services/projects.service';
import { StatusService } from '../../services/status.service';
import { TicketExportService } from '../../services/ticket-export.service';
import { TicketService } from '../../services/ticket.service';
import { SearchTicketsComponent } from './search-tickets.component';

describe('SearchTicketsComponent', () => {
  let component: SearchTicketsComponent;
  let fixture: ComponentFixture<SearchTicketsComponent>;
  let router: Router;
  let ticketService: jasmine.SpyObj<TicketService>;
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
    ticketService = jasmine.createSpyObj('TicketService', ['search']);
    ticketService.search.and.returnValue(of([]));
    ticketExportService = jasmine.createSpyObj('TicketExportService', ['download']);
    ticketExportService.download.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [
        createTranslocoTestingModule(
          {
            ticketExport: {
              action: 'Exportar',
              errorLimit: 'A exportação excede o limite de 10.000 tickets.',
              errorGeneral: 'Não foi possível exportar os tickets.',
            },
          },
          {
            ticketExport: {
              action: 'Export',
              errorLimit: 'The export exceeds the 10,000 ticket limit.',
              errorGeneral: 'Could not export tickets.',
            },
          },
        ),
        SearchTicketsComponent,
      ],
      providers: [
        provideRouter([{ path: 'search', component: SearchTicketsComponent }]),
        { provide: TicketService, useValue: ticketService },
        { provide: TicketExportService, useValue: ticketExportService },
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

  it('should render one compact Exportar menu with CSV and JSON for the current simple search', async () => {
    await router.navigate(['/search'], { queryParams: { q: 'login', status: 2 } });
    fixture.detectChanges();

    openExportMenu();

    const menuItems = Array.from(document.querySelectorAll('[role="menuitem"]')).map(item => item.textContent?.trim());
    expect(menuItems).toEqual(['CSV', 'JSON']);
  });

  it('should export all matches from exact current term and status criteria without using browser rows', async () => {
    await router.navigate(['/search'], { queryParams: { q: 'login', status: 2 } });
    fixture.detectChanges();
    expect(component.tickets).toEqual([]);

    chooseExportFormat('CSV');

    expect(ticketExportService.download).toHaveBeenCalledOnceWith(
      { source: 'simple', term: 'login', statusId: 2 },
      'CSV',
    );
  });

  it('should disable duplicate export while the current JSON export is loading', async () => {
    const exportCompletion = new Subject<void>();
    ticketExportService.download.and.returnValue(exportCompletion);
    await router.navigate(['/search'], { queryParams: { q: 'login', status: 2 } });
    fixture.detectChanges();

    const trigger = chooseExportFormat('JSON');
    trigger.click();
    fixture.detectChanges();

    expect(trigger.disabled).toBeTrue();
    expect(ticketExportService.download).toHaveBeenCalledTimes(1);
  });

  it('should clear export loading and error state after a successful export', async () => {
    const exportCompletion = new Subject<void>();
    ticketExportService.download.and.returnValue(exportCompletion);
    await router.navigate(['/search'], { queryParams: { q: 'login' } });
    fixture.detectChanges();

    const trigger = chooseExportFormat('CSV');
    expect(trigger.disabled).toBeTrue();

    exportCompletion.next();
    exportCompletion.complete();
    fixture.detectChanges();

    expect(trigger.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('should show limit-specific feedback when all matching tickets exceed the export cap', async () => {
    ticketExportService.download.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 413, statusText: 'Payload Too Large' })),
    );
    await router.navigate(['/search'], { queryParams: { q: 'login' } });
    fixture.detectChanges();

    chooseExportFormat('CSV');

    const feedback = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(feedback?.textContent).toContain('A exportação excede o limite de 10.000 tickets.');
  });

  it('should show general feedback and restore export after another export failure', async () => {
    ticketExportService.download.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' })),
    );
    await router.navigate(['/search'], { queryParams: { q: 'login' } });
    fixture.detectChanges();

    const trigger = chooseExportFormat('JSON');

    const feedback = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(feedback?.textContent).toContain('Não foi possível exportar os tickets.');
    expect(trigger.disabled).toBeFalse();
  });

  it('should rerender export menu copy immediately from Portuguese to English without changing route', async () => {
    await router.navigate(['/search'], { queryParams: { q: 'login', status: 2 } });
    fixture.detectChanges();
    const urlBeforeLocaleChange = router.url;
    openExportMenu();
    expect(document.body.textContent).toContain('Exportar');
    expect(document.body.textContent).toContain('CSV');
    expect(document.body.textContent).toContain('JSON');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Export');
    expect(fixture.nativeElement.textContent).not.toContain('Exportar');
    expect(router.url).toBe(urlBeforeLocaleChange);
  });
});
