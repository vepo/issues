import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { ProjectMembersService, ProjectMember } from '../../services/project-members.service';
import { Project } from '../../services/projects.service';
import { Ticket } from '../../services/ticket.service';
import { User, UsersService } from '../../services/users.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-project-allocation',
  imports: [
    TranslocoPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './project-allocation.component.html'
})
export class ProjectAllocationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly membersService = inject(ProjectMembersService);
  private readonly usersService = inject(UsersService);
  private readonly toastService = inject(ToastService);

  project?: Project;
  members: ProjectMember[] = [];
  candidateUsers: User[] = [];
  selectedUserId: number | null = null;
  isSaving = false;
  removalBlockMessage: string | null = null;
  blockedOpenTickets: Ticket[] = [];

  ngOnInit(): void {
    this.route.data.subscribe(({ project }) => {
      this.project = project;
      if (project?.id) {
        this.loadMembers(project.id);
        this.loadCandidates(project.id);
      }
    });
  }

  addMember(): void {
    if (!this.project?.id || this.selectedUserId == null) {
      return;
    }
    this.isSaving = true;
    this.membersService.addMember(this.project.id, this.selectedUserId).subscribe({
      next: member => {
        this.members = [...this.members, member];
        this.selectedUserId = null;
        this.isSaving = false;
        this.loadCandidates(this.project!.id);
        this.toastService.success('Membro adicionado.');
      },
      error: () => {
        this.isSaving = false;
        this.toastService.error('Não foi possível adicionar o membro.');
      }
    });
  }

  removeMember(member: ProjectMember): void {
    if (!this.project?.id) {
      return;
    }
    this.removalBlockMessage = null;
    this.blockedOpenTickets = [];
    this.membersService.removeMember(this.project.id, member.id).subscribe({
      next: () => {
        this.members = this.members.filter(m => m.id !== member.id);
        this.loadCandidates(this.project!.id);
        this.toastService.success('Membro removido.');
      },
      error: (error: HttpErrorResponse) => {
        const message = error.error?.message ?? 'Não foi possível remover o membro.';
        this.removalBlockMessage = message;
        this.membersService.listOpenAssignedTickets(this.project!.id, member.id).subscribe({
          next: tickets => this.blockedOpenTickets = tickets
        });
      }
    });
  }

  private loadMembers(projectId: number): void {
    this.membersService.listMembers(projectId).subscribe(members => {
      this.members = members;
    });
  }

  private loadCandidates(projectId: number): void {
    this.usersService.search().subscribe(users => {
      const memberIds = new Set(this.members.map(m => m.id));
      this.candidateUsers = users.filter(user => !memberIds.has(user.id));
    });
  }
}
