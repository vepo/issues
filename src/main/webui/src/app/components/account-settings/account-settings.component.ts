import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatError, MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { forkJoin } from 'rxjs';
import { AuthService, CurrentUser } from '../../services/auth.service';
import { ApiToken, ApiTokenService, CreatedApiToken } from '../../services/api-token.service';
import { ToastService } from '../../services/toast.service';
import { strongPasswordValidators } from '../../core/password-policy';
import { RuntimeDatePipe } from '../../core/runtime-locale.pipes';
import { isAllowedLocale, UiLocaleCode } from '../../core/ui-locale';
import { UiLocaleService } from '../../core/ui-locale.service';

const TOKEN_PLACEHOLDER = '<YOUR_API_TOKEN>';

@Component({
  selector: 'app-account-settings',
  imports: [
    RuntimeDatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatError,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    RouterLink,
    TranslocoPipe
  ],
  templateUrl: './account-settings.component.html'
})
export class AccountSettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly apiTokenService = inject(ApiTokenService);
  private readonly toastService = inject(ToastService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly uiLocaleService = inject(UiLocaleService);
  private readonly transloco = inject(TranslocoService);

  readonly localeOptions: { value: UiLocaleCode; label: string }[] = [
    { value: 'pt', label: 'Português' },
    { value: 'en', label: 'English' },
  ];

  user: CurrentUser | null = null;
  loading = true;
  error = '';
  profileMessage = '';
  profileError = '';
  passwordMessage = '';
  passwordError = '';
  isSavingProfile = false;
  isSavingPassword = false;
  changePasswordEnabled = true;

  apiTokens: ApiToken[] = [];
  tokensLoading = false;
  tokensError = '';
  isCreatingToken = false;
  isGeneratingAgentConfig = false;
  createdTokenSecret: string | null = null;
  agentPreset = 'cursor';
  agentSnippet: string | null = null;
  agentConfigError = '';
  agentConfigMessage = '';

  profileForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    locale: ['pt' as UiLocaleCode, Validators.required]
  });

  passwordForm: FormGroup = this.formBuilder.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', strongPasswordValidators],
    confirmPassword: ['', Validators.required]
  });

  tokenForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(2)]]
  });

  ngOnInit(): void {
    this.authService.getCapabilities().subscribe(capabilities => {
      this.changePasswordEnabled = capabilities.changePassword === true;
    });
    this.authService.me().subscribe({
      next: (user: CurrentUser) => {
        this.user = user;
        const locale = isAllowedLocale(user.locale) ? user.locale : 'pt';
        this.profileForm.patchValue({
          name: user.name,
          email: user.email,
          locale
        });
        this.loading = false;
      },
      error: () => {
        this.error = this.transloco.translate('account.messages.loadError');
        this.loading = false;
      }
    });
    this.loadApiTokens();
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }
    this.isSavingProfile = true;
    this.profileMessage = '';
    this.profileError = '';
    const { name, email, locale } = this.profileForm.value;
    const previousLocale = isAllowedLocale(this.user?.locale)
      ? this.user.locale
      : this.uiLocaleService.currentLocale;
    this.authService.updateProfile(name, email, locale).subscribe({
      next: (user) => {
        this.user = user;
        this.isSavingProfile = false;
        this.profileMessage = this.transloco.translate('account.messages.profileSaved');
        if (isAllowedLocale(locale) && locale !== previousLocale) {
          this.uiLocaleService.setActiveLocale(locale);
        }
      },
      error: () => {
        this.isSavingProfile = false;
        this.profileError = this.transloco.translate('account.messages.profileSaveError');
      }
    });
  }

  passwordsMismatch(): boolean {
    const newPassword = this.passwordForm.get('newPassword')?.value;
    const confirmPassword = this.passwordForm.get('confirmPassword')?.value;
    return !!newPassword && !!confirmPassword && newPassword !== confirmPassword;
  }

  changePassword(): void {
    if (this.passwordForm.invalid || this.passwordsMismatch()) {
      return;
    }
    this.isSavingPassword = true;
    this.passwordMessage = '';
    this.passwordError = '';
    const { currentPassword, newPassword } = this.passwordForm.value;
    this.authService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.isSavingPassword = false;
        this.passwordMessage = this.transloco.translate('account.messages.passwordChanged');
        this.passwordForm.reset();
      },
      error: () => {
        this.isSavingPassword = false;
        this.passwordError = this.transloco.translate('account.messages.passwordChangeError');
      }
    });
  }

  generateAgentConfig(): void {
    this.isGeneratingAgentConfig = true;
    this.agentConfigError = '';
    this.agentConfigMessage = '';
    const tokenName = this.transloco.translate('account.agent.generatedTokenName', {
      date: new Date().toISOString().slice(0, 16).replace('T', ' '),
    });
    forkJoin({
      created: this.apiTokenService.create(tokenName),
      setup: this.apiTokenService.getAgentSetupConfig(this.agentPreset)
    }).subscribe({
      next: ({ created, setup }) => {
        this.isGeneratingAgentConfig = false;
        this.createdTokenSecret = created.token;
        this.agentSnippet = this.injectTokenIntoSnippet(setup.snippet ?? '', created.token);
        this.agentConfigMessage = this.transloco.translate('account.messages.agentConfigGenerated');
        this.loadApiTokens();
        this.toastService.success(this.transloco.translate('account.messages.agentConfigGeneratedToast'));
      },
      error: () => {
        this.isGeneratingAgentConfig = false;
        this.agentConfigError = this.transloco.translate('account.messages.agentConfigError');
      }
    });
  }

  createApiToken(): void {
    if (this.tokenForm.invalid) {
      return;
    }
    this.isCreatingToken = true;
    this.tokensError = '';
    const name = this.tokenForm.value.name as string;
    this.apiTokenService.create(name).subscribe({
      next: (created: CreatedApiToken) => {
        this.isCreatingToken = false;
        this.createdTokenSecret = created.token;
        this.tokenForm.reset();
        this.loadApiTokens();
        this.toastService.success(this.transloco.translate('account.messages.tokenCreated'));
      },
      error: () => {
        this.isCreatingToken = false;
        this.tokensError = this.transloco.translate('account.messages.tokenCreateError');
      }
    });
  }

  revokeApiToken(token: ApiToken): void {
    if (!token.id || token.revokedAt) {
      return;
    }
    this.apiTokenService.revoke(token.id).subscribe({
      next: () => {
        this.loadApiTokens();
        this.toastService.success(this.transloco.translate('account.messages.tokenRevoked'));
      },
      error: () => {
        this.tokensError = this.transloco.translate('account.messages.tokenRevokeError');
      }
    });
  }

  copyText(text: string): void {
    navigator.clipboard.writeText(text).then(
      () => this.toastService.success(this.transloco.translate('account.messages.copied')),
      () => this.toastService.error(this.transloco.translate('account.messages.copyError'))
    );
  }

  dismissSecret(): void {
    this.createdTokenSecret = null;
  }

  activeTokens(): ApiToken[] {
    return this.apiTokens.filter(token => !token.revokedAt);
  }

  private loadApiTokens(): void {
    this.tokensLoading = true;
    this.apiTokenService.list().subscribe({
      next: tokens => {
        this.apiTokens = tokens;
        this.tokensLoading = false;
      },
      error: () => {
        this.tokensLoading = false;
        this.tokensError = this.transloco.translate('account.messages.tokensLoadError');
      }
    });
  }

  private injectTokenIntoSnippet(snippet: string, token: string): string {
    if (snippet.includes(TOKEN_PLACEHOLDER)) {
      return snippet.replaceAll(TOKEN_PLACEHOLDER, token);
    }
    return `${snippet}\n\n# Authorization: Bearer ${token}`;
  }
}
