import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatInputHarness } from '@angular/material/input/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { UsersService } from '../../services/users.service';
import { UsersEditComponent } from './users-edit.component';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

// Helper function to find checkbox by label text
async function findCheckboxByLabel(checkboxes: MatCheckboxHarness[], labelText: string): Promise<MatCheckboxHarness> {
  for (const checkbox of checkboxes) {
    const text = await checkbox.getLabelText();
    if (text.includes(labelText)) {
      return checkbox;
    }
  }
  throw new Error(`Checkbox with label containing "${labelText}" not found`);
}

describe('UsersEditComponent', () => {
  let component: UsersEditComponent;
  let fixture: ComponentFixture<UsersEditComponent>;
  let loader: HarnessLoader;
  let mockUsersService: jasmine.SpyObj<UsersService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: any;

  beforeEach(async () => {
    mockUsersService = jasmine.createSpyObj('UsersService', ['create', 'update']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockUsersService.create.and.returnValue(of({
      id: 1,
      name: 'name',
      username: 'username',
      email: 'name@issues.vepo.dev',
      roles:['USER']
    }));
    mockUsersService.update.and.returnValue(of({
      id: 1,
      name: 'name',
      username: 'username',
      email: 'name@issues.vepo.dev',
      roles:['USER']
    }));

    mockActivatedRoute = {
      data: EMPTY
    };

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatCheckboxModule,
        MatButtonModule,
        RouterLink,
        UsersEditComponent,
        createTranslocoTestingModule(
          { common: { save: 'Salvar' }, user: { roles: { projectManager: 'Gerente de projetos' } } },
          { common: { save: 'Save' }, user: { roles: { projectManager: 'Project manager' } } },
        ),
      ],
      providers: [
        { provide: UsersService, useValue: mockUsersService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UsersEditComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize in create mode when no user provided', () => {
      expect(component.editMode).toBeFalse();
      expect(fixture.debugElement.query(By.css('h1')).nativeElement.textContent).toContain('Novo usuário');
    });

    it('should initialize in edit mode when user provided', fakeAsync(() => {
      const testUser = {
        id: 1,
        name: 'Test User',
        username: 'test',
        email: 'test@example.com',
        roles: ['admin']
      };
      
      mockActivatedRoute.data = of({ user: testUser });
      fixture = TestBed.createComponent(UsersEditComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.editMode).toBeTrue();
      expect(component.userId).toBe(1);
      expect(component.userForm.value).toEqual({
        name: 'Test User',
        username: 'test',
        email: 'test@example.com',
        roles: ['admin']
      });
      expect(fixture.debugElement.query(By.css('h1')).nativeElement.textContent).toContain('Editar usuário');
    }));
  });

  describe('Form Validation', () => {
    it('should mark form as invalid when empty', () => {
      expect(component.userForm.invalid).toBeTrue();
    });

    it('should validate name field', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('Test User');
      expect(component.userForm.controls.name.valid).toBeTrue();
      
      await nameInput.setValue('');
      expect(component.userForm.controls.name.invalid).toBeTrue();
    });

    it('should validate email field', async () => {
      const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
      
      await emailInput.setValue('invalid-email');
      expect(component.userForm.controls.email.invalid).toBeTrue();
      
      await emailInput.setValue('valid@example.com');
      expect(component.userForm.controls.email.valid).toBeTrue();
    });

    it('should validate roles field', async () => {
      expect(component.userForm.controls.roles.invalid).toBeTrue();
      
      // Find the admin checkbox by its label text
      const adminCheckbox = await findCheckboxByLabel(await loader.getAllHarnesses(MatCheckboxHarness), 'Administrador');
      await adminCheckbox.check();
      
      expect(component.userForm.controls.roles.valid).toBeTrue();
    });
  });

  describe('Toggle Roles', () => {
    it('should add role when toggled on', async () => {
      const adminCheckbox = await findCheckboxByLabel(await loader.getAllHarnesses(MatCheckboxHarness), 'Administrador');
      
      await adminCheckbox.check();
      expect(component.userForm.value.roles).toContain('admin');
      
      await adminCheckbox.uncheck();
      expect(component.userForm.value.roles).not.toContain('admin');
    });

    it('should remove role when toggled off', async () => {
      // First add a role
      component.userForm.patchValue({ roles: ['admin'] });
      
      const adminCheckbox = await findCheckboxByLabel(await loader.getAllHarnesses(MatCheckboxHarness), 'Administrador');
      await adminCheckbox.uncheck();

      expect(component.userForm.value.roles).not.toContain('admin');
    });
  });

  describe('Save Functionality', () => {
    beforeEach(async () => {
      // Set valid form values
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
      const usernameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
      const adminCheckbox = await findCheckboxByLabel(await loader.getAllHarnesses(MatCheckboxHarness), 'Administrador');
      
      await nameInput.setValue('Test User');
      await usernameInput.setValue('test');
      await emailInput.setValue('test@example.com');
      await adminCheckbox.check();
    });

    it('should call create when in create mode', async () => {
      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Salvar' }));
      await saveButton.click();
      
      expect(mockUsersService.create).toHaveBeenCalledWith({
        name: 'Test User',
        username: 'test',
        email: 'test@example.com',
        roles: ['admin']
      });
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/', 'users']);
    });

    it('should call update when in edit mode', fakeAsync(() => {
      component.editMode = true;
      component.userId = 1;
      fixture.detectChanges();
      tick();

      const saveButton = fixture.debugElement
        .queryAll(By.css('button.btn[matButton="filled"]'))
        .map((el) => el.nativeElement as HTMLButtonElement)
        .find((btn) => btn.textContent?.trim() === 'Salvar');
      expect(saveButton).withContext('Salvar button').toBeTruthy();
      saveButton!.click();

      expect(mockUsersService.update).toHaveBeenCalledWith(1, {
        name: 'Test User',
        username: 'test',
        email: 'test@example.com',
        roles: ['admin']
      });
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/', 'users']);
    }));

    it('should not call service when form is invalid', async () => {
      component.userForm.reset();
      fixture.detectChanges();
      
      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Salvar' }));
      await saveButton.click();
      
      expect(mockUsersService.create).not.toHaveBeenCalled();
      expect(mockRouter.navigate).not.toHaveBeenCalled();
    });
  });

  describe('UI Interactions', () => {
    it('should disable save button when form is invalid', async () => {
      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Salvar' }));
      expect(await saveButton.isDisabled()).toBeTrue();
      
      // Make form valid
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
      const usernameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
      const adminCheckbox = await findCheckboxByLabel(await loader.getAllHarnesses(MatCheckboxHarness), 'Administrador');
      
      await nameInput.setValue('Test User');
      await usernameInput.setValue('test');
      await emailInput.setValue('test@example.com');
      await adminCheckbox.check();
      
      expect(await saveButton.isDisabled()).toBeFalse();
    });

    it('should navigate to users list on cancel', async () => {
      const cancelButton = await loader.getHarness(MatButtonHarness.with({ text: 'Cancelar' }));
      await cancelButton.click();
      
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/', 'users']);
    });
  });
});