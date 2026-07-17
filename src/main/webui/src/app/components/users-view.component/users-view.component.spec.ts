import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { UsersService } from '../../services/users.service';
import { ToastService } from '../../services/toast.service';
import { UsersViewComponent } from './users-view.component';
import { TranslocoService } from '@jsverse/transloco';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

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
      imports: [
        UsersViewComponent,
        createTranslocoTestingModule(
          { user: { title: 'Usuários', create: 'Novo usuário' } },
          { user: { title: 'Users', create: 'New user' } },
        ),
      ],
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

  it('should rerender user actions immediately when locale changes from Portuguese to English', async () => {
    expect(fixture.nativeElement.textContent).toContain('Usuários');
    expect(fixture.nativeElement.textContent).toContain('Novo usuário');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Users');
    expect(fixture.nativeElement.textContent).toContain('New user');
    expect(fixture.nativeElement.textContent).not.toContain('Novo usuário');
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
