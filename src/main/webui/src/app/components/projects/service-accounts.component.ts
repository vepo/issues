import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Project } from '../../services/projects.service';
import { ServiceAccount, ServiceAccountService } from '../../services/service-account.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-service-accounts',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './service-accounts.component.html'
})
export class ServiceAccountsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly serviceAccountService = inject(ServiceAccountService);
  private readonly toastService = inject(ToastService);
  private readonly formBuilder = inject(FormBuilder);

  project?: Project;
  accounts: ServiceAccount[] = [];
  loading = false;
  error = '';
  isCreating = false;
  generatingTokenId: number | null = null;
  deactivatingId: number | null = null;
  createdTokenSecret: string | null = null;
  createdTokenAccountName: string | null = null;

  createForm: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(2)]]
  });

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
      if (project?.id) {
        this.loadAccounts(project.id);
      }
    });
  }

  createAccount(): void {
    if (!this.project?.id || this.createForm.invalid) {
      return;
    }
    this.isCreating = true;
    this.error = '';
    const name = this.createForm.value.name as string;
    this.serviceAccountService.create(this.project.id, name).subscribe({
      next: account => {
        this.accounts = [...this.accounts, account];
        this.createForm.reset();
        this.isCreating = false;
        this.toastService.success('Conta de serviço criada.');
      },
      error: () => {
        this.isCreating = false;
        this.error = 'Não foi possível criar a conta de serviço.';
      }
    });
  }

  generateToken(account: ServiceAccount): void {
    if (!this.project?.id || !account.id || !account.active) {
      return;
    }
    this.generatingTokenId = account.id;
    this.error = '';
    const tokenName = `token-${new Date().toISOString().slice(0, 10)}`;
    this.serviceAccountService.createToken(this.project.id, account.id, tokenName).subscribe({
      next: created => {
        this.generatingTokenId = null;
        this.createdTokenSecret = created.token;
        this.createdTokenAccountName = account.name;
        this.toastService.success('Token gerado. Copie o segredo agora.');
      },
      error: () => {
        this.generatingTokenId = null;
        this.error = 'Não foi possível gerar o token.';
      }
    });
  }

  deactivate(account: ServiceAccount): void {
    if (!this.project?.id || !account.id || !account.active) {
      return;
    }
    this.deactivatingId = account.id;
    this.error = '';
    this.serviceAccountService.deactivate(this.project.id, account.id).subscribe({
      next: () => {
        this.deactivatingId = null;
        this.accounts = this.accounts.map(item =>
          item.id === account.id ? { ...item, active: false } : item
        );
        this.toastService.success('Conta de serviço desativada.');
      },
      error: () => {
        this.deactivatingId = null;
        this.error = 'Não foi possível desativar a conta de serviço.';
      }
    });
  }

  copySecret(): void {
    if (!this.createdTokenSecret) {
      return;
    }
    navigator.clipboard.writeText(this.createdTokenSecret).then(
      () => this.toastService.success('Copiado para a área de transferência.'),
      () => this.toastService.error('Não foi possível copiar.')
    );
  }

  dismissSecret(): void {
    this.createdTokenSecret = null;
    this.createdTokenAccountName = null;
  }

  activeAccounts(): ServiceAccount[] {
    return this.accounts.filter(account => account.active);
  }

  private loadAccounts(projectId: number): void {
    this.loading = true;
    this.serviceAccountService.list(projectId).subscribe({
      next: accounts => {
        this.accounts = accounts;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Não foi possível carregar as contas de serviço.';
      }
    });
  }
}
