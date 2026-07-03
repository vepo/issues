import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { ProjectsService } from '../../services/projects.service';
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
          workflow: { id: 1, name: 'Default Workflow' },
          ticketTemplate: {
            enabled: true,
            title: 'Template title',
            description: 'Template description here',
            categoryId: 2,
            priority: 'HIGH',
          },
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

    it('should include ticket template in update payload', () => {
      projectsService.update.and.returnValue(of({
        id: 1,
        name: 'Test Project',
        description: 'A test project',
        prefix: 'TP',
        workflow: { id: 1, name: 'Default Workflow' },
        ticketTemplate: { enabled: true, title: 'Template title', description: 'Template description here', categoryId: 2, priority: 'HIGH' },
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
        templateTitle: '',
        templateDescription: '',
        templateCategoryId: -1,
      });
      component.save();
      expect(projectsService.create).not.toHaveBeenCalled();
      expect(component.projectForm.controls.templateTitle.invalid).toBeTrue();
      expect(component.projectForm.controls.templateDescription.invalid).toBeTrue();
      expect(component.projectForm.controls.templateCategoryId.invalid).toBeTrue();
    });

    it('should include ticket template in create payload when valid', () => {
      projectsService.create.and.returnValue(of({
        id: 2,
        name: 'New Project',
        description: 'Project description',
        prefix: 'NP',
        workflow: { id: 1, name: 'Default Workflow' },
        ticketTemplate: {
          enabled: true,
          title: 'Default ticket title',
          description: 'Default ticket description',
          categoryId: 2,
          priority: 'MEDIUM',
        },
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
