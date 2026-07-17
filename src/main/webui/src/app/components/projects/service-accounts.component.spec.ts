import '@angular/localize/init';
import { registerLocaleData } from '@angular/common';
import localeEn from '@angular/common/locales/en';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ServiceAccountService } from '../../services/service-account.service';
import { ToastService } from '../../services/toast.service';
import { ServiceAccountsComponent } from './service-accounts.component';
import { TranslocoService } from '@jsverse/transloco';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';
import { UiLocaleService } from '../../core/ui-locale.service';

registerLocaleData(localePt);
registerLocaleData(localeEn);

describe('ServiceAccountsComponent', () => {
  let fixture: ComponentFixture<ServiceAccountsComponent>;
  let serviceAccountService: jasmine.SpyObj<ServiceAccountService>;

  beforeEach(async () => {
    serviceAccountService = jasmine.createSpyObj('ServiceAccountService', [
      'list',
      'create',
      'deactivate',
      'createToken',
      'revokeToken'
    ]);
    serviceAccountService.list.and.returnValue(of([
      {
        id: 1,
        name: 'bot-ci',
        createdAt: '2026-07-11T12:00:00Z',
        active: true,
      }
    ]));
    serviceAccountService.create.and.returnValue(of({
      id: 2,
      name: 'novo-bot',
      createdAt: '2026-07-11T13:00:00Z',
      active: true,
    }));
    serviceAccountService.createToken.and.returnValue(of({
      id: 9,
      name: 'token-2026-07-11',
      prefix: 'iss_sat_',
      createdAt: '2026-07-11T13:00:00Z',
      token: 'iss_sat_secret',
    }));
    serviceAccountService.deactivate.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [
        ServiceAccountsComponent,
        createTranslocoTestingModule(
          {
            common: { generating: 'Gerando...' },
            serviceAccount: {
              title: 'Contas de serviço',
              generateToken: 'Gerar token',
              deactivating: 'Desativando...',
              deactivate: 'Desativar',
            },
          },
          {
            common: { generating: 'Generating...' },
            serviceAccount: {
              title: 'Service accounts',
              generateToken: 'Generate token',
              deactivating: 'Deactivating...',
              deactivate: 'Deactivate',
            },
          },
        ),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ project: { id: 42, name: 'Demo' } })
          }
        },
        { provide: ServiceAccountService, useValue: serviceAccountService },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) },
        { provide: LOCALE_ID, useValue: 'pt' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceAccountsComponent);
    fixture.detectChanges();
  });

  it('should list service accounts with actions', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Contas de serviço');
    expect(text).toContain('bot-ci');
    expect(text).toContain('Gerar token');
    expect(text).toContain('Desativar');
  });

  it('should rerender service-account actions immediately when locale changes from Portuguese to English', async () => {
    expect(fixture.nativeElement.textContent).toContain('Contas de serviço');
    expect(fixture.nativeElement.textContent).toContain('Gerar token');
    expect(fixture.nativeElement.textContent).toContain('Desativar');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Service accounts');
    expect(fixture.nativeElement.textContent).toContain('Generate token');
    expect(fixture.nativeElement.textContent).toContain('Deactivate');
    expect(fixture.nativeElement.textContent).not.toContain('Gerar token');
  });

  it('should reformat visible creation dates immediately when locale changes from Portuguese to English', async () => {
    const localeService = TestBed.inject(UiLocaleService);
    localeService.setActiveLocale('pt');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('11 de jul. de 2026');

    localeService.setActiveLocale('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jul 11, 2026');
    expect(fixture.nativeElement.textContent).not.toContain('11 de jul. de 2026');
  });

  it('should create a service account', () => {
    const component = fixture.componentInstance;
    component.createForm.setValue({ name: 'novo-bot' });
    component.createAccount();
    fixture.detectChanges();

    expect(serviceAccountService.create).toHaveBeenCalledWith(42, 'novo-bot');
    expect(fixture.nativeElement.textContent).toContain('novo-bot');
  });

  it('should show secret once after generating token', () => {
    const generateButton = fixture.debugElement.queryAll(By.css('button'))
      .find(el => (el.nativeElement as HTMLElement).textContent?.includes('Gerar token'));
    generateButton!.nativeElement.click();
    fixture.detectChanges();

    expect(serviceAccountService.createToken).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('iss_sat_secret');
  });
});
