import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { emptyFilter, User, UserSearchFilter, UsersService } from '../../services/users.service';

@Component({
  selector: 'app-users-view.component',
  imports: [MatIcon, MatButton, MatFormFieldModule, MatInputModule, FormsModule, RouterLink],
  templateUrl: './users-view.component.html'
})
export class UsersViewComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly usersService = inject(UsersService);


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
}
