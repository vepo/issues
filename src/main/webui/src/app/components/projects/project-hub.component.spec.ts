import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectHubComponent } from './project-hub.component';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';

describe('ProjectHubComponent', () => {
  let fixture: ComponentFixture<ProjectHubComponent>;

  beforeEach(async () => {
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
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectHubComponent);
    fixture.detectChanges();
  });

  it('shows management actions for project owner', () => {
    expect(fixture.componentInstance.canManage()).toBeTrue();
  });
});
