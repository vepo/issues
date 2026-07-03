import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectHubComponent } from './project-hub.component';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { PhaseService } from '../../services/phase.service';
import { VersionService } from '../../services/version.service';

describe('ProjectHubComponent', () => {
  let fixture: ComponentFixture<ProjectHubComponent>;
  let phaseService: jasmine.SpyObj<PhaseService>;
  let versionService: jasmine.SpyObj<VersionService>;

  beforeEach(async () => {
    phaseService = jasmine.createSpyObj('PhaseService', ['list']);
    versionService = jasmine.createSpyObj('VersionService', ['list']);
    phaseService.list.and.returnValue(of([
      { id: 1, projectId: 1, name: 'MVP', status: 'ACTIVE', deliverables: [], createdAt: '2026-01-01' }
    ]));
    versionService.list.and.returnValue(of([
      { id: 1, projectId: 1, label: '1.0.0', description: 'Initial' }
    ]));

    await TestBed.configureTestingModule({
      imports: [ProjectHubComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              project: {
                id: 1,
                name: 'Issues',
                description: 'MVP',
                owner: { id: 2 }
              }
            })
          }
        },
        {
          provide: AuthService,
          useValue: {
            hasRole: () => false,
            getAuthUserId: () => 2
          }
        },
        { provide: PhaseService, useValue: phaseService },
        { provide: VersionService, useValue: versionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectHubComponent);
    fixture.detectChanges();
  });

  it('shows management actions for project owner', () => {
    expect(fixture.componentInstance.canManage()).toBeTrue();
  });

  it('loads phases and versions for the project', () => {
    expect(phaseService.list).toHaveBeenCalledWith(1);
    expect(versionService.list).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.phases.length).toBe(1);
    expect(fixture.componentInstance.versions.length).toBe(1);
  });
});
