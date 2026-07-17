import { TranslocoTestingModule, type Translation } from '@jsverse/transloco';
import enCatalog from '../../../../public/i18n/en.json';
import ptCatalog from '../../../../public/i18n/pt.json';

function mergeCatalog(base: Translation, override: Translation): Translation {
  const merged: Translation = { ...base };
  for (const [key, value] of Object.entries(override)) {
    merged[key] = value && typeof value === 'object' && !Array.isArray(value)
      ? mergeCatalog((base[key] as Translation) ?? {}, value as Translation)
      : value;
  }
  return merged;
}

export function createTranslocoTestingModule(
  pt: Translation = {},
  en: Translation = {},
) {
  return TranslocoTestingModule.forRoot({
    langs: {
      pt: mergeCatalog(ptCatalog, pt),
      en: mergeCatalog(enCatalog, en),
    },
    translocoConfig: {
      availableLangs: ['pt', 'en'],
      defaultLang: 'pt',
      reRenderOnLangChange: true,
    },
    preloadLangs: true,
  });
}
