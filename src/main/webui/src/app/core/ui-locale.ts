export type UiLocaleCode = 'pt' | 'en';

export function currentPathLocale(): UiLocaleCode {
  const segment = window.location.pathname.split('/').filter(Boolean)[0];
  return segment === 'en' ? 'en' : 'pt';
}

/** Path after the locale prefix, always starting with `/` (or `/` alone). */
export function pathWithoutLocale(): string {
  const parts = window.location.pathname.split('/').filter(Boolean);
  if (parts[0] === 'pt' || parts[0] === 'en') {
    parts.shift();
  }
  return '/' + parts.join('/');
}

export function hrefForLocale(locale: UiLocaleCode, path: string = pathWithoutLocale()): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  if (normalized === '/') {
    return `/${locale}/`;
  }
  return `/${locale}${normalized}`;
}

export function isAllowedLocale(value: string | null | undefined): value is UiLocaleCode {
  return value === 'pt' || value === 'en';
}
