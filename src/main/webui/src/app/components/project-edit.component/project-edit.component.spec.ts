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

  beforeEach(async () => {
    projectsService = jasmine.createSpyObj('ProjectsService', ['create', 'update']);
    await TestBed.configureTestingModule({
      imports: [ProjectEditComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
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
            }),
          },
        },
        { provide: ProjectsService, useValue: projectsService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
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
