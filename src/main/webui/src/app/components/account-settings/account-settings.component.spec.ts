import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { UiLocaleService } from '../../core/ui-locale.service';
import { AuthService } from '../../services/auth.service';
import { ApiTokenService } from '../../services/api-token.service';
import { ToastService } from '../../services/toast.service';
import { AccountSettingsComponent } from './account-settings.component';

describe('AccountSettingsComponent', () => {
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let apiTokenService: jasmine.SpyObj<ApiTokenService>;
  let uiLocaleService: jasmine.SpyObj<UiLocaleService>;

  async function setup(changePassword: boolean): Promise<void> {
    authService = jasmine.createSpyObj('AuthService', ['me', 'getCapabilities', 'changePassword', 'updateProfile']);
    uiLocaleService = jasmine.createSpyObj(
      'UiLocaleService',
      ['setActiveLocale'],
      { currentLocale: 'pt' },
    );
    apiTokenService = jasmine.createSpyObj('ApiTokenService', [
      'list',
      'create',
      'revoke',
      'getAgentSetupConfig'
    ]);
    authService.getCapabilities.and.returnValue(of({
      provider: changePassword ? 'local' : 'endpoint',
      passwordRecovery: changePassword,
      changePassword,
    }));
    authService.me.and.returnValue(of({
      id: 1,
      username: 'user',
      name: 'User',
      email: 'user@issues.vepo.dev',
      roles: new Set(['user']),
      locale: 'pt',
    } as any));
    authService.updateProfile.and.returnValue(of({
      id: 1,
      username: 'user',
      name: 'User',
      email: 'user@issues.vepo.dev',
      roles: new Set(['user']),
      locale: 'en',
    } as any));
    apiTokenService.list.and.returnValue(of([
      {
        id: 10,
        name: 'CI',
        tokenPrefix: 'iss_pat_',
        createdAt: '2026-07-11T12:00:00Z',
        lastUsedAt: null as any,
        revokedAt: null as any,
      }
    ]));
    apiTokenService.create.and.returnValue(of({
      id: 11,
      name: 'Novo',
      prefix: 'iss_pat_',
      createdAt: '2026-07-11T12:00:00Z',
      token: 'iss_pat_secret',
    }));
    apiTokenService.getAgentSetupConfig.and.returnValue(of({
      preset: 'cursor',
      issuesPublicBaseUrl: 'http://localhost:8080',
      mcpPublicBaseUrl: 'http://localhost:8081',
      mcpUrl: 'http://localhost:8081/mcp',
      issuesApiBaseUrl: 'http://localhost:8080/api',
      snippet: '{"headers":{"Authorization":"Bearer <YOUR_API_TOKEN>"}}',
    }));

    await TestBed.configureTestingModule({
      imports: [
        AccountSettingsComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            pt: {
              common: { actions: 'Ações' },
              auth: { email: 'E-mail', reset: { requestTitle: 'Recuperar senha' } },
              account: {
                title: 'Conta',
                language: 'Idioma',
                subtitle: 'Seu perfil e permissões no Issues',
                profile: 'Perfil',
                name: 'Nome',
                username: 'Usuário',
                roles: 'Perfis',
                saveProfile: 'Salvar perfil',
                password: {
                  title: 'Alterar senha',
                  current: 'Senha atual',
                  new: 'Nova senha',
                  confirm: 'Confirmar nova senha',
                  forgot: 'Esqueceu a senha?',
                },
                agent: {
                  title: 'Conectar agente',
                  subtitle: 'Gere um token e uma configuração pronta para colar no Cursor MCP.',
                  preset: 'Preset',
                  generate: 'Gerar token e configuração',
                },
                tokens: {
                  title: 'Tokens de API',
                  subtitle: 'Tokens pessoais para agentes e integrações.',
                  name: 'Nome do token',
                  create: 'Criar token',
                  prefix: 'Prefixo',
                  createdAt: 'Criado em',
                  lastUsed: 'Último uso',
                  revoke: 'Revogar',
                },
              },
            },
            en: {
              common: { actions: 'Actions' },
              auth: { email: 'Email', reset: { requestTitle: 'Recover password' } },
              account: {
                title: 'Account',
                language: 'Language',
                subtitle: 'Your profile and permissions in Issues',
                profile: 'Profile',
                name: 'Name',
                username: 'Username',
                roles: 'Roles',
                saveProfile: 'Save profile',
                password: {
                  title: 'Change password',
                  current: 'Current password',
                  new: 'New password',
                  confirm: 'Confirm new password',
                  forgot: 'Forgot your password?',
                },
                agent: {
                  title: 'Connect agent',
                  subtitle: 'Generate a token and ready-to-paste Cursor MCP configuration.',
                  preset: 'Preset',
                  generate: 'Generate token and configuration',
                },
                tokens: {
                  title: 'API tokens',
                  subtitle: 'Personal tokens for agents and integrations.',
                  name: 'Token name',
                  create: 'Create token',
                  prefix: 'Prefix',
                  createdAt: 'Created at',
                  lastUsed: 'Last used',
                  revoke: 'Revoke',
                },
              },
            },
          },
          translocoConfig: {
            availableLangs: ['pt', 'en'],
            defaultLang: 'pt',
            reRenderOnLangChange: true,
          },
          preloadLangs: true,
        }),
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ApiTokenService, useValue: apiTokenService },
        { provide: UiLocaleService, useValue: uiLocaleService },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSettingsComponent);
    fixture.detectChanges();
  }

  it('should show password section when changePassword is true', async () => {
    await setup(true);
    expect(fixture.nativeElement.textContent).toContain('Alterar senha');
    expect(fixture.debugElement.query(By.css('[formControlName=currentPassword]'))).toBeTruthy();
  });

  it('should hide password section when changePassword is false', async () => {
    await setup(false);
    expect(fixture.debugElement.query(By.css('[formControlName=currentPassword]'))).toBeNull();
  });

  it('should show Conectar agente and Tokens de API sections', async () => {
    await setup(true);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Conectar agente');
    expect(text).toContain('Gerar token e configuração');
    expect(text).toContain('Tokens de API');
    expect(text).toContain('CI');
    expect(text).toContain('Revogar');
  });

  it('should inject token into MCP snippet when generating agent config', async () => {
    await setup(true);
    const button = fixture.debugElement.queryAll(By.css('button'))
      .find(el => (el.nativeElement as HTMLElement).textContent?.includes('Gerar token e configuração'));
    expect(button).toBeTruthy();
    button!.nativeElement.click();
    fixture.detectChanges();

    expect(apiTokenService.create).toHaveBeenCalled();
    expect(apiTokenService.getAgentSetupConfig).toHaveBeenCalledWith('cursor');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('iss_pat_secret');
    expect(text).not.toContain('<YOUR_API_TOKEN>');
  });

  it('should apply changed profile locale immediately without reloading or changing path', async () => {
    await setup(true);
    expect(fixture.nativeElement.textContent).toContain('Idioma');
    const localeControl = fixture.debugElement.query(By.css('[formControlName=locale]'));
    expect(localeControl).toBeTruthy();
    const currentPath = window.location.pathname;
    fixture.componentInstance.profileForm.patchValue({ locale: 'en' });

    fixture.componentInstance.saveProfile();

    expect(authService.updateProfile).toHaveBeenCalledWith('User', 'user@issues.vepo.dev', 'en');
    expect(uiLocaleService.setActiveLocale).toHaveBeenCalledOnceWith('en');
    expect(window.location.pathname).toBe(currentPath);
  });

  it('should rerender account content from Portuguese to English in place', async () => {
    await setup(true);
    const transloco = TestBed.inject(TranslocoService);
    const currentPath = window.location.pathname;
    expect(fixture.nativeElement.textContent).toContain('Conta');
    expect(fixture.nativeElement.textContent).toContain('Idioma');

    transloco.setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Account');
    expect(fixture.nativeElement.textContent).toContain('Language');
    expect(fixture.nativeElement.textContent).not.toContain('Idioma');
    expect(window.location.pathname).toBe(currentPath);
  });
});
