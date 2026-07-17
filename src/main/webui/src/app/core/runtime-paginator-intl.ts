import { DestroyRef, inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { UiLocale, UiLocaleService } from './ui-locale.service';

interface PaginatorLabels {
  itemsPerPage: string;
  nextPage: string;
  previousPage: string;
  firstPage: string;
  lastPage: string;
  rangeSeparator: string;
}

const PAGINATOR_LABELS: Readonly<Record<UiLocale, PaginatorLabels>> = {
  pt: {
    itemsPerPage: 'Itens por página:',
    nextPage: 'Próxima página',
    previousPage: 'Página anterior',
    firstPage: 'Primeira página',
    lastPage: 'Última página',
    rangeSeparator: 'de',
  },
  en: {
    itemsPerPage: 'Items per page:',
    nextPage: 'Next page',
    previousPage: 'Previous page',
    firstPage: 'First page',
    lastPage: 'Last page',
    rangeSeparator: 'of',
  },
};

@Injectable()
export class RuntimePaginatorIntl extends MatPaginatorIntl {
  private readonly destroyRef = inject(DestroyRef);
  private readonly localeService = inject(UiLocaleService);

  constructor() {
    super();
    this.localeService.currentLocale$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((locale) => this.applyLocale(locale));
  }

  private applyLocale(locale: UiLocale): void {
    const labels = PAGINATOR_LABELS[locale];

    this.itemsPerPageLabel = labels.itemsPerPage;
    this.nextPageLabel = labels.nextPage;
    this.previousPageLabel = labels.previousPage;
    this.firstPageLabel = labels.firstPage;
    this.lastPageLabel = labels.lastPage;
    this.getRangeLabel = (page: number, pageSize: number, length: number): string => {
      if (length === 0 || pageSize === 0) {
        return `0 ${labels.rangeSeparator} ${length}`;
      }
      const startIndex = page * pageSize;
      const endIndex = Math.min(startIndex + pageSize, length);
      return `${startIndex + 1} – ${endIndex} ${labels.rangeSeparator} ${length}`;
    };
    this.changes.next();
  }
}
