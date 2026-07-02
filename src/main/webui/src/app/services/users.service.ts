import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { UserApi } from '../generated/api/user.service';
import { CreateUserRequest } from '../generated/model/createUserRequest';
import { UserResponse } from '../generated/model/userResponse';

import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type User = Loaded<UserResponse>;
export type UpdateOrCreateUserRequest = CreateUserRequest;

export interface UserSearchFilter {
  name: string;
  email: string;
  roles: string[];
}

export function emptyFilter(): UserSearchFilter {
  return {
    name: '',
    email: '',
    roles: []
  };
}

@Injectable({
  providedIn: 'root'
})
export class UsersService {
  private readonly api = inject(UserApi);

  findById(userId: number): Observable<User> {
    return this.api.findUserById(userId).pipe(map(asLoaded));
  }

  create(user: UpdateOrCreateUserRequest): Observable<User> {
    return this.api.createUser(user).pipe(map(asLoaded));
  }

  update(userId: number, user: UpdateOrCreateUserRequest): Observable<User> {
    return this.api.updateUser(userId, user).pipe(map(asLoaded));
  }

  search(filter?: UserSearchFilter): Observable<User[]> {
    return this.api.searchUsers(filter?.email, filter?.name, filter?.roles).pipe(map(asLoadedArray));
  }
}
