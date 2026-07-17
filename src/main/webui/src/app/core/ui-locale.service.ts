import { DestroyRef, inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoService } from '@jsverse/transloco';
import { BehaviorSubject, distinctUntilChanged, Observable, skip } from 'rxjs';

export type UiLocale = 'pt' | 'en';

@Injectable({ providedIn: 'root' })
export class UiLocaleService {
  private readonly destroyRef = inject(DestroyRef);
  private readonly locale: BehaviorSubject<UiLocale>;
  readonly currentLocale$: Observable<UiLocale>;

  constructor(private readonly translocoService: TranslocoService) {
    const browserLocale = this.resolveBrowserLocale(window.navigator.language);
    this.locale = new BehaviorSubject<UiLocale>(browserLocale);
    this.currentLocale$ = this.locale.asObservable().pipe(distinctUntilChanged());

    this.translocoService.langChanges$
      .pipe(skip(1), takeUntilDestroyed(this.destroyRef))
      .subscribe((locale) => {
        if (this.isUiLocale(locale) && locale !== this.locale.value) {
          this.locale.next(locale);
        }
      });
    this.translocoService.setActiveLang(browserLocale);
  }

  get currentLocale(): UiLocale {
    return this.locale.value;
  }

  setActiveLocale(locale: UiLocale): void {
    this.locale.next(locale);
    this.translocoService.setActiveLang(locale);
  }

  private resolveBrowserLocale(browserLanguage: string): UiLocale {
    const language = browserLanguage.toLowerCase().split('-')[0];
    return this.isUiLocale(language) ? language : 'pt';
  }

  private isUiLocale(locale: string): locale is UiLocale {
    return locale === 'pt' || locale === 'en';
  }
}
