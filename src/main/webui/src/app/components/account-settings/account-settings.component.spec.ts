import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ApiTokenService } from '../../services/api-token.service';
import { ToastService } from '../../services/toast.service';
import { AccountSettingsComponent } from './account-settings.component';

describe('AccountSettingsComponent', () => {
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let apiTokenService: jasmine.SpyObj<ApiTokenService>;

  async function setup(changePassword: boolean): Promise<void> {
    authService = jasmine.createSpyObj('AuthService', ['me', 'getCapabilities', 'changePassword', 'updateProfile']);
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
      imports: [AccountSettingsComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ApiTokenService, useValue: apiTokenService },
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
});
