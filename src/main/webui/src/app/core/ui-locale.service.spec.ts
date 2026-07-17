import { TestBed } from '@angular/core/testing';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslocoService } from '@jsverse/transloco';
import { BehaviorSubject } from 'rxjs';

import { RuntimePaginatorIntl } from './runtime-paginator-intl';
import { UiLocaleService } from './ui-locale.service';

describe('UiLocaleService', () => {
  let languageChanges: BehaviorSubject<string>;
  let translocoService: jasmine.SpyObj<TranslocoService>;

  function createService(browserLanguage: string): UiLocaleService {
    spyOnProperty(window.navigator, 'language', 'get').and.returnValue(browserLanguage);
    languageChanges = new BehaviorSubject<string>('pt');
    translocoService = jasmine.createSpyObj<TranslocoService>(
      'TranslocoService',
      ['setActiveLang'],
      { langChanges$: languageChanges.asObservable() }
    );

    TestBed.configureTestingModule({
      providers: [
        UiLocaleService,
        { provide: MatPaginatorIntl, useClass: RuntimePaginatorIntl },
        { provide: TranslocoService, useValue: translocoService }
      ]
    });

    return TestBed.inject(UiLocaleService);
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should default to Portuguese when browser language is Portuguese', () => {
    const service = createService('pt-BR');

    expect(service.currentLocale).toBe('pt');
    expect(translocoService.setActiveLang).toHaveBeenCalledOnceWith('pt');
  });

  it('should default to English when browser language is English', () => {
    const service = createService('en-US');

    expect(service.currentLocale).toBe('en');
    expect(translocoService.setActiveLang).toHaveBeenCalledOnceWith('en');
  });

  it('should fall back to Portuguese when browser language is outside the allow-list', () => {
    const service = createService('es-AR');

    expect(service.currentLocale).toBe('pt');
    expect(translocoService.setActiveLang).toHaveBeenCalledOnceWith('pt');
  });

  it('should activate selected locale immediately without navigation or reload', () => {
    const service = createService('pt-BR');
    translocoService.setActiveLang.calls.reset();
    const currentUrl = window.location.href;

    service.setActiveLocale('en');

    expect(translocoService.setActiveLang).toHaveBeenCalledOnceWith('en');
    expect(service.currentLocale).toBe('en');
    expect(window.location.href).toBe(currentUrl);
  });

  it('should expose current locale changes reactively', () => {
    const service = createService('pt-BR');
    const observedLocales: string[] = [];
    const subscription = service.currentLocale$.subscribe((locale: string) => observedLocales.push(locale));

    service.setActiveLocale('en');
    languageChanges.next('en');

    expect(observedLocales).toEqual(['pt', 'en']);
    subscription.unsubscribe();
  });

  it('should update Angular Material paginator labels immediately when locale changes from Portuguese to English', () => {
    const service = createService('pt-BR');
    const paginator = TestBed.inject(MatPaginatorIntl);
    const labelsChanged = jasmine.createSpy('labelsChanged');
    const subscription = paginator.changes.subscribe(labelsChanged);

    service.setActiveLocale('pt');

    expect(paginator.itemsPerPageLabel).toBe('Itens por página:');
    expect(paginator.nextPageLabel).toBe('Próxima página');
    expect(paginator.getRangeLabel(0, 10, 25)).toBe('1 – 10 de 25');

    service.setActiveLocale('en');

    expect(paginator.itemsPerPageLabel).toBe('Items per page:');
    expect(paginator.nextPageLabel).toBe('Next page');
    expect(paginator.getRangeLabel(0, 10, 25)).toBe('1 – 10 of 25');
    expect(labelsChanged).toHaveBeenCalled();
    subscription.unsubscribe();
  });
});
