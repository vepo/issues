import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { currentPathLocale, hrefForLocale, isAllowedLocale } from '../../core/ui-locale';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, RouterLink]
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  error = '';
  passwordRecovery = true;
  hide = signal(true);

  ngOnInit(): void {
    this.auth.getCapabilities().subscribe(capabilities => {
      this.passwordRecovery = capabilities.passwordRecovery === true;
    });
  }

  login() {
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.auth.me().subscribe({
          next: async (user) => {
            const preferred = user.locale;
            if (isAllowedLocale(preferred) && preferred !== currentPathLocale()) {
              window.location.assign(hrefForLocale(preferred, '/'));
              return;
            }
            await this.router.navigate(['/']);
          },
          error: async () => await this.router.navigate(['/']),
        });
      },
      error: () => this.error = $localize`:@@login.invalidCredentials:E-mail ou senha inválidos`
    });
  }

  clickEvent(event: MouseEvent) {
    this.hide.set(!this.hide());
    event.stopPropagation();
  }
}
