import { Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { UsersService } from '../../services/users.service';

@Component({
  selector: 'app-users-edit.component',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule, TranslocoPipe],
  templateUrl: './users-edit.component.html',
  standalone: true
})
export class UsersEditComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly usersService = inject(UsersService);
  private readonly router = inject(Router);

  editMode: boolean = false;
  userId: number | null = null;
  userForm = new FormGroup({
    username: new FormControl('', [Validators.required, Validators.minLength(4), Validators.maxLength(15)]),
    name: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.email, Validators.required]),
    roles: new FormControl([] as string[], Validators.required)
  });

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ user }) => {
      console.debug("User data: ", user);
      this.editMode = user != null;
      this.userId = user.id;
      this.userForm.setValue({
        username: user.username,
        name: user.name,
        email: user.email,
        roles: user.roles
      });
    });
  }


  toggle(role: string): void {
    const currentRoles = this.userForm.get('roles')?.value || [];
    const roleIndex = currentRoles.indexOf(role);

    let newRoles: string[];
    if (roleIndex === -1) {
      newRoles = [...currentRoles, role];
    } else {
      newRoles = [...currentRoles];
      newRoles.splice(roleIndex, 1);
    }
    console.debug("Roles", newRoles);
    this.userForm.get('roles')?.setValue(newRoles);
  }

  cancel(): void {
    this.router.navigate(['/', 'users']);
  }

  isRoleSelected(role: string) {
    const currentRoles = this.userForm.get('roles')?.value || [];
    return currentRoles.indexOf(role) != -1;
  }


  save() {
    console.log("Save call!")
    if (this.userForm.invalid) return;
    const { name, username, email, roles } = this.userForm.value;
    if (!name || !username || !email || !roles) return;

    if (this.userId) {
      this.usersService.update(this.userId, { name, username, email, roles })
        .subscribe(user => this.router.navigate(['/', 'users']));
    } else {
      this.usersService.create({ name, username, email, roles })
        .subscribe(user => this.router.navigate(['/', 'users']));
    }
  }
}
