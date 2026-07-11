import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ServiceAccountService } from '../../services/service-account.service';
import { ToastService } from '../../services/toast.service';
import { ServiceAccountsComponent } from './service-accounts.component';

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
      imports: [ServiceAccountsComponent],
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
