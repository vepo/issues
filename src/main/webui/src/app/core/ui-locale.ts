export type UiLocaleCode = 'pt' | 'en';

export function isAllowedLocale(value: string | null | undefined): value is UiLocaleCode {
  return value === 'pt' || value === 'en';
}
