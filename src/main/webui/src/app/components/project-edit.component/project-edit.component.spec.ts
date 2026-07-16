import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomFieldService } from '../../services/custom-field.service';
import { ProjectsService } from '../../services/projects.service';
import { UsersService } from '../../services/users.service';
import { ProjectEditComponent } from './project-edit.component';

describe('ProjectEditComponent', () => {
  let component: ProjectEditComponent;
  let fixture: ComponentFixture<ProjectEditComponent>;
  let projectsService: jasmine.SpyObj<ProjectsService>;

  async function setup(routeData: object): Promise<void> {
    projectsService = jasmine.createSpyObj('ProjectsService', ['create', 'update']);
    await TestBed.configureTestingModule({
      imports: [ProjectEditComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of(routeData) },
        },
        { provide: ProjectsService, useValue: projectsService },
        { provide: AuthService, useValue: { hasRole: () => true, getAuthUserId: () => 1 } },
        { provide: UsersService, useValue: { search: () => of([]) } },
        {
          provide: CustomFieldService,
          useValue: {
            listInScope: jasmine.createSpy('listInScope').and.returnValue(of([])),
            listProjectFields: jasmine.createSpy('listProjectFields').and.returnValue(of([])),
          },
        },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProjectEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('edit mode', () => {
    beforeEach(async () => {
      await setup({
        project: {
          id: 1,
          name: 'Test Project',
          description: 'A test project',
          prefix: 'TP',
          securityLevel: 'INTERNAL', prefixLocked: false,
          workflow: { id: 1, name: 'Default Workflow' },
          owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
          ticketTemplate: {
            enabled: true,
            title: 'Template title',
            description: 'Template description here',
            categoryId: 2,
            priority: 'HIGH',
          },
          phaseTemplate: { deliverables: [] },
        },
        workflows: [{ id: 1, name: 'Default Workflow' }],
        categories: [{ id: 2, name: 'Bug', color: 'red' }],
      });
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should show template fields when template is enabled', () => {
      expect(fixture.debugElement.query(By.css('[formControlName=templateTitle]'))).toBeTruthy();
    });

    it('should keep prefix enabled when not locked', () => {
      expect(component.prefixLocked).toBeFalse();
      expect(component.projectForm.controls.prefix.disabled).toBeFalse();
    });

    it('should include ticket template in update payload', () => {
      projectsService.update.and.returnValue(of({
        id: 1,
        name: 'Test Project',
        description: 'A test project',
        prefix: 'TP',
        securityLevel: 'INTERNAL', prefixLocked: false,
        workflow: { id: 1, name: 'Default Workflow' },
        owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
        ticketTemplate: { enabled: true, title: 'Template title', description: 'Template description here', categoryId: 2, priority: 'HIGH' },
        phaseTemplate: { deliverables: [] },
      }));
      component.save();

      expect(projectsService.update).toHaveBeenCalledWith(1, jasmine.objectContaining({
        workflowId: 1,
        ticketTemplate: jasmine.objectContaining({
          enabled: true,
          title: 'Template title',
          categoryId: 2,
          priority: 'HIGH',
        }),
      }));
    });
  });

  describe('edit mode with locked prefix', () => {
    beforeEach(async () => {
      await setup({
        project: {
          id: 1,
          name: 'Locked Project',
          description: 'Has tickets',
          prefix: 'LK',
          securityLevel: 'INTERNAL', prefixLocked: true,
          workflow: { id: 1, name: 'Default Workflow' },
          owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
          ticketTemplate: { enabled: false },
          phaseTemplate: { deliverables: [] },
        },
        workflows: [{ id: 1, name: 'Default Workflow' }],
        categories: [],
      });
    });

    it('should disable prefix control when prefixLocked', () => {
      expect(component.prefixLocked).toBeTrue();
      expect(component.projectForm.controls.prefix.disabled).toBeTrue();
      expect(component.projectForm.controls.prefix.value).toBe('LK');
    });

    it('should still submit locked prefix via getRawValue on save', () => {
      projectsService.update.and.returnValue(of({
        id: 1,
        name: 'Locked Project',
        description: 'Has tickets',
        prefix: 'LK',
        securityLevel: 'INTERNAL', prefixLocked: true,
        workflow: { id: 1, name: 'Default Workflow' },
        owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
        ticketTemplate: { enabled: false },
        phaseTemplate: { deliverables: [] },
      }));
      component.projectForm.patchValue({ name: 'Locked Project Renamed' });
      component.save();

      expect(projectsService.update).toHaveBeenCalledWith(1, jasmine.objectContaining({
        name: 'Locked Project Renamed',
        prefix: 'LK',
      }));
    });
  });

  describe('create mode', () => {
    beforeEach(async () => {
      await setup({
        workflows: [{ id: 1, name: 'Default Workflow' }],
        categories: [{ id: 2, name: 'Bug', color: 'red' }],
      });
    });

    it('should require description before save', () => {
      component.projectForm.patchValue({
        name: 'New Project',
        prefix: 'NP',
        workflow: 1,
        description: '',
      });
      component.save();
      expect(projectsService.create).not.toHaveBeenCalled();
      expect(component.projectForm.controls.description.invalid).toBeTrue();
    });

    it('should require template fields when template is enabled', () => {
      component.projectForm.patchValue({
        name: 'New Project',
        description: 'Project description',
        prefix: 'NP',
        workflow: 1,
        templateEnabled: true,
        templateTitle: 'ab',
        templateDescription: 'cd',
        templateCategoryId: -1,
      });
      // Ensure validators apply after patch (valueChanges order can leave empty values passing minLength)
      component['updateTemplateValidators'](true);

      expect(component.projectForm.controls.templateTitle.invalid).toBeTrue();
      expect(component.projectForm.controls.templateDescription.invalid).toBeTrue();
      component.save();
      expect(projectsService.create).not.toHaveBeenCalled();
    });

    it('should include ticket template in create payload when valid', () => {
      projectsService.create.and.returnValue(of({
        id: 2,
        name: 'New Project',
        description: 'Project description',
        prefix: 'NP',
        securityLevel: 'INTERNAL', prefixLocked: false,
        workflow: { id: 1, name: 'Default Workflow' },
        owner: { id: 1, name: 'PM', email: 'pm@issues.vepo.dev' },
        ticketTemplate: {
          enabled: true,
          title: 'Default ticket title',
          description: 'Default ticket description',
          categoryId: 2,
          priority: 'MEDIUM',
        },
        phaseTemplate: { deliverables: [] },
      }));
      component.projectForm.patchValue({
        name: 'New Project',
        description: 'Project description',
        prefix: 'NP',
        workflow: 1,
        templateEnabled: true,
        templateTitle: 'Default ticket title',
        templateDescription: 'Default ticket description',
        templateCategoryId: 2,
        templatePriority: 'MEDIUM',
      });
      component.save();

      expect(projectsService.create).toHaveBeenCalledWith(jasmine.objectContaining({
        description: 'Project description',
        ticketTemplate: jasmine.objectContaining({
          enabled: true,
          title: 'Default ticket title',
          categoryId: 2,
        }),
      }));
    });
  });
});
