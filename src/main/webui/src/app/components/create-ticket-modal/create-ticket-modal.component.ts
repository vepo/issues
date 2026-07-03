import { Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Category, CategoryService } from '../../services/category.service';
import { Project, ProjectsService } from '../../services/projects.service';
import { CreateTicketRequest, TicketService } from '../../services/ticket.service';

@Component({
  selector: 'app-create-ticket-modal',
  templateUrl: './create-ticket-modal.component.html',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatOptionModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions
  ]
})
export class CreateTicketModalComponent implements OnInit {
  private readonly dialogRef = inject<MatDialogRef<CreateTicketModalComponent>>(MatDialogRef);
  private readonly ticketService = inject(TicketService);
  private readonly categoryService = inject(CategoryService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  authorId!: number;
  categories: Category[] = [];
  projects: Project[] = [];

  ticketForm = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]),
    projectId: new FormControl(-1, [Validators.min(1)]),
    description: new FormControl('', [Validators.minLength(5), Validators.maxLength(1200)]),
    categoryId: new FormControl(-1, [Validators.min(1)]),
  });

  constructor() {
    const authService = this.authService;

    this.authorId = authService.getAuthUserId();
  }

  get title() {
    return this.ticketForm.get('title');
  }

  get description() {
    return this.ticketForm.get('description');
  }

  get categoryId() {
    return this.ticketForm.get('categoryId');
  }

  get projectId() {
    return this.ticketForm.get('projectId');
  }

  ngOnInit() {
    this.categoryService.findAll()
                        .subscribe(categories => this.categories = categories);
    this.projectsService.findAll()
                        .subscribe(projs => this.projects = projs);
  }

  close() {
    this.dialogRef.close();
  }

  createTicket() {
    if (this.ticketForm.invalid) return;
    
    const req: CreateTicketRequest = {
      title: this.ticketForm.value.title!,
      description: this.ticketForm.value.description!,
      categoryId: this.ticketForm.value.categoryId!,
      projectId: this.ticketForm.value.projectId!
    };
    this.ticketService.createTicket(req)
                      .subscribe(ticket => { 
                        this.dialogRef.close(ticket);
                        this.router.navigate(['/', 'ticket', ticket.identifier]);
                      });
  }
} 