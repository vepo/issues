import {
  ApplicationConfig,
  inject,
  isDevMode,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { JWT_OPTIONS, JwtHelperService } from '@auth0/angular-jwt';
import { provideTransloco } from '@jsverse/transloco';
import { RuntimePaginatorIntl } from './core/runtime-paginator-intl';
import { TranslocoHttpLoader } from './core/transloco-loader';
import { UiLocaleService } from './core/ui-locale.service';
import { BASE_PATH } from './generated/variables';
import { routes } from './app.routes';
import { authInterceptor } from './services/auth.interceptor';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    provideTransloco({
      config: {
        availableLangs: ['pt', 'en'],
        defaultLang: 'pt',
        fallbackLang: 'pt',
        reRenderOnLangChange: true,
        prodMode: !isDevMode()
      },
      loader: TranslocoHttpLoader
    }),
    provideAppInitializer(() => {
      inject(UiLocaleService);
    }),
    { provide: MatPaginatorIntl, useClass: RuntimePaginatorIntl },
    { provide: BASE_PATH, useValue: '' },
    { provide: LOCALE_ID, useValue: 'pt' },
    { provide: JWT_OPTIONS, useValue: JWT_OPTIONS },
    JwtHelperService,
    provideCharts(withDefaultRegisterables())
  ]
};
