import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, RouterLink, TranslocoPipe]
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly transloco = inject(TranslocoService);

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
          next: async () => await this.router.navigate(['/']),
          error: async () => await this.router.navigate(['/']),
        });
      },
      error: () => this.error = this.transloco.translate('auth.login.invalidCredentials')
    });
  }

  clickEvent(event: MouseEvent) {
    this.hide.set(!this.hide());
    event.stopPropagation();
  }
}
