import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectAllocationComponent } from './project-allocation.component';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { ProjectMembersService } from '../../services/project-members.service';
import { UsersService } from '../../services/users.service';
import { ToastService } from '../../services/toast.service';
import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';

describe('ProjectAllocationComponent', () => {
  let fixture: ComponentFixture<ProjectAllocationComponent>;
  let membersService: jasmine.SpyObj<ProjectMembersService>;

  beforeEach(async () => {
    membersService = jasmine.createSpyObj('ProjectMembersService', ['listMembers', 'addMember', 'removeMember', 'listOpenAssignedTickets']);
    membersService.listMembers.and.returnValue(of([{ id: 1, username: 'user', name: 'User', email: 'user@issues.vepo.dev' }]));

    await TestBed.configureTestingModule({
      imports: [createTranslocoTestingModule(), ProjectAllocationComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ project: { id: 1, name: 'Issues' } }) }
        },
        { provide: ProjectMembersService, useValue: membersService },
        { provide: UsersService, useValue: { search: () => of([]) } },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectAllocationComponent);
    fixture.detectChanges();
  });

  it('loads project members on init', () => {
    expect(membersService.listMembers).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.members.length).toBe(1);
  });
});
