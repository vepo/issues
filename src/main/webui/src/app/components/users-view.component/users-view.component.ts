import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { emptyFilter, User, UserSearchFilter, UsersService } from '../../services/users.service';

@Component({
  selector: 'app-users-view.component',
  imports: [MatIcon, MatButton, MatCheckboxModule, FormsModule, RouterLink],
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
    console.debug("Toggle filter for role:", role)
    let roleIndex = this.filter.roles.indexOf(role);
    if (roleIndex == -1) {
      this.filter.roles.push(role);
      this.users = this.users.filter(u => u.roles.indexOf(role) != -1);
    } else {
      console.debug("Removing role...", roleIndex, this.filter.roles);
      this.filter.roles.splice(roleIndex, 1);
      console.debug("Role removed!", this.filter.roles);
      this.updateSearch();
    }
    this.lastSearch = this.filter;
  }

  updateSearch() {
    this.usersService.search(this.filter)
      .subscribe(resp => this.users = resp);
  }

  filterChanged(value: string) {
    switch (value) {
      case 'name':
        console.debug("Is new name substring of old name?", this.lastSearch.name, this.filter.name);
        if (this.filter.name != '' && (this.lastSearch.name == '' || this.lastSearch.name.indexOf(this.filter.name))) {
          console.debug('filtering', this.filter);
          this.users = this.users.filter(u => u.name.indexOf(this.filter.name) != -1);
        } else {
          console.debug('Searching again...', this.filter)
          this.updateSearch();
        }
        break;
      case 'email':
        if (this.filter.email != '' && (this.lastSearch.email == '' || this.lastSearch.email.indexOf(this.filter.email))) {
          this.users = this.users.filter(u => u.email.indexOf(this.filter.email) != -1);
        } else {
          this.updateSearch();
        }
        break;
    }
    this.lastSearch = this.filter;
  }
}
