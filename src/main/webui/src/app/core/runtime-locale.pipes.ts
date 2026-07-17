import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe, registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localePt from '@angular/common/locales/pt';
import { inject, Injectable, Pipe, PipeTransform } from '@angular/core';

import { UiLocaleService } from './ui-locale.service';

registerLocaleData(localePt);
registerLocaleData(localeEn);

type NumericValue = string | number | null | undefined;

@Injectable({ providedIn: 'root' })
export class RuntimeLocaleFormatter {
  private readonly localeService = inject(UiLocaleService);
  private readonly datePipes = new Map<string, DatePipe>();
  private readonly decimalPipes = new Map<string, DecimalPipe>();
  private readonly percentPipes = new Map<string, PercentPipe>();
  private readonly currencyPipes = new Map<string, CurrencyPipe>();

  formatDate(
    value: string | number | Date | null | undefined,
    format?: string,
    timezone?: string,
    locale?: string,
  ): string | null {
    return this.getFormattingPipe(this.datePipes, locale, DatePipe).transform(value, format, timezone);
  }

  formatNumber(value: NumericValue, digitsInfo?: string, locale?: string): string | null {
    return this.getFormattingPipe(this.decimalPipes, locale, DecimalPipe).transform(value, digitsInfo);
  }

  formatPercent(value: NumericValue, digitsInfo?: string, locale?: string): string | null {
    return this.getFormattingPipe(this.percentPipes, locale, PercentPipe).transform(value, digitsInfo);
  }

  formatCurrency(
    value: NumericValue,
    currencyCode = 'USD',
    display: string | boolean = 'symbol',
    digitsInfo?: string,
    locale?: string,
  ): string | null {
    return this.getFormattingPipe(this.currencyPipes, locale, CurrencyPipe).transform(
      value,
      currencyCode,
      display,
      digitsInfo,
    );
  }

  private getFormattingPipe<T>(
    pipes: Map<string, T>,
    locale: string | undefined,
    pipeType: new (locale: string) => T,
  ): T {
    const activeLocale = locale ?? this.localeService.currentLocale;
    let pipe = pipes.get(activeLocale);
    if (!pipe) {
      pipe = new pipeType(activeLocale);
      pipes.set(activeLocale, pipe);
    }
    return pipe;
  }
}

@Pipe({ name: 'runtimeDate', standalone: true, pure: false })
export class RuntimeDatePipe implements PipeTransform {
  private readonly formatter = inject(RuntimeLocaleFormatter);

  transform(
    value: string | number | Date | null | undefined,
    format?: string,
    timezone?: string,
    locale?: string,
  ): string | null {
    return this.formatter.formatDate(value, format, timezone, locale);
  }
}

@Pipe({ name: 'runtimeNumber', standalone: true, pure: false })
export class RuntimeNumberPipe implements PipeTransform {
  private readonly formatter = inject(RuntimeLocaleFormatter);

  transform(value: NumericValue, digitsInfo?: string, locale?: string): string | null {
    return this.formatter.formatNumber(value, digitsInfo, locale);
  }
}

@Pipe({ name: 'runtimePercent', standalone: true, pure: false })
export class RuntimePercentPipe implements PipeTransform {
  private readonly formatter = inject(RuntimeLocaleFormatter);

  transform(value: NumericValue, digitsInfo?: string, locale?: string): string | null {
    return this.formatter.formatPercent(value, digitsInfo, locale);
  }
}

@Pipe({ name: 'runtimeCurrency', standalone: true, pure: false })
export class RuntimeCurrencyPipe implements PipeTransform {
  private readonly formatter = inject(RuntimeLocaleFormatter);

  transform(
    value: NumericValue,
    currencyCode = 'USD',
    display: string | boolean = 'symbol',
    digitsInfo?: string,
    locale?: string,
  ): string | null {
    return this.formatter.formatCurrency(value, currencyCode, display, digitsInfo, locale);
  }
}
