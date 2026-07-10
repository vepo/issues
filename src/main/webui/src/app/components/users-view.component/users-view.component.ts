import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButton } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { emptyFilter, User, UserSearchFilter, UsersService } from '../../services/users.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-users-view.component',
  imports: [MatIcon, MatButton, MatFormFieldModule, MatInputModule, FormsModule, RouterLink, MatDialogModule],
  templateUrl: './users-view.component.html'
})
export class UsersViewComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly usersService = inject(UsersService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);

  users: User[] = [];
  filter: UserSearchFilter = emptyFilter();
  lastSearch: UserSearchFilter = emptyFilter();

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ users }) => this.users = users);
  }

  toggleRole(role: string) {
    const roleIndex = this.filter.roles.indexOf(role);
    if (roleIndex === -1) {
      this.filter.roles.push(role);
    } else {
      this.filter.roles.splice(roleIndex, 1);
    }
    this.updateSearch();
    this.lastSearch = { ...this.filter, roles: [...this.filter.roles] };
  }

  updateSearch() {
    this.usersService.search(this.filter)
      .subscribe(resp => this.users = resp);
  }

  filterChanged(value: string) {
    if (value === 'name' || value === 'email') {
      this.updateSearch();
    }
    this.lastSearch = { ...this.filter, roles: [...this.filter.roles] };
  }

  confirmDelete(user: User): void {
    const confirmed = this.dialog.open(UserDeleteDialogComponent);
    confirmed.afterClosed().subscribe(result => {
      if (!result) {
        return;
      }
      this.usersService.delete(user.id).subscribe({
        next: () => this.updateSearch(),
        error: (error: HttpErrorResponse) => {
          const message = error.error?.message ?? 'Não foi possível excluir o usuário.';
          this.toast.error(message);
        }
      });
    });
  }
}

@Component({
  selector: 'app-user-delete-dialog',
  imports: [MatDialogModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton],
  template: `
    <h2 mat-dialog-title i18n>Excluir usuário?</h2>
    <mat-dialog-content i18n>Esta ação não pode ser desfeita.</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button class="btn btn-secondary" matButton="outlined" mat-dialog-close i18n>Cancelar</button>
      <button class="btn btn-cancel" matButton="filled" [mat-dialog-close]="true" i18n>Excluir</button>
    </mat-dialog-actions>
  `
})
export class UserDeleteDialogComponent {}
