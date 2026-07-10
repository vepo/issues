import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { UsersService } from '../../services/users.service';
import { ToastService } from '../../services/toast.service';
import { UsersViewComponent } from './users-view.component';

describe('UsersViewComponent', () => {
  let component: UsersViewComponent;
  let fixture: ComponentFixture<UsersViewComponent>;
  let usersService: jasmine.SpyObj<UsersService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let toast: jasmine.SpyObj<ToastService>;

  const users = [
    { id: 1, username: 'alice', name: 'Alice', email: 'alice@example.com', roles: ['user'] },
  ];

  beforeEach(async () => {
    usersService = jasmine.createSpyObj('UsersService', ['search', 'delete']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    toast = jasmine.createSpyObj('ToastService', ['error']);
    usersService.search.and.returnValue(of(users));

    await TestBed.configureTestingModule({
      imports: [UsersViewComponent],
      providers: [
        { provide: UsersService, useValue: usersService },
        { provide: ActivatedRoute, useValue: { data: of({ users }) } },
        { provide: ToastService, useValue: toast },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    fixture = TestBed.createComponent(UsersViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should delete user and refresh list when confirmed', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    usersService.delete.and.returnValue(of(void 0));
    usersService.search.and.returnValue(of([]));

    component.confirmDelete(users[0]);

    expect(usersService.delete).toHaveBeenCalledWith(1);
    expect(component.users).toEqual([]);
  });

  it('should toast error when delete is rejected', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    usersService.delete.and.returnValue(
      throwError(() => ({ error: { message: 'User cannot be deleted while assigned to open tickets' } }))
    );

    component.confirmDelete(users[0]);

    expect(toast.error).toHaveBeenCalledWith('User cannot be deleted while assigned to open tickets');
  });
});
