import { Routes } from '@angular/router';
import { CreateTicketComponent } from './components/create-ticket/create-ticket.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { KanbanComponent } from './components/kanban/kanban.component';
import { LoginComponent } from './components/login/login.component';
import { PasswordResetRequestComponent } from './components/password-reset-request/password-reset-request.component';
import { PasswordResetComponent } from './components/password-reset/password-reset.component';
import { ProjectEditComponent } from './components/project-edit.component/project-edit.component';
import { ProjectsViewComponent } from './components/projects-view.component/projects-view.component';
import { SearchTicketsComponent } from './components/search-tickets/search-tickets.component';
import { TicketViewComponent } from './components/ticket-view/ticket-view.component';
import { UsersEditComponent } from './components/users-edit.component/users-edit.component';
import { UsersViewComponent } from './components/users-view.component/users-view.component';
import { WorkflowsViewComponent } from './components/workflows/workflows-view.component';
import { WorkflowCreateComponent } from './components/workflows/workflow-create.component';
import { WorkflowEditComponent } from './components/workflows/workflow-edit.component';
import { AccountSettingsComponent } from './components/account-settings/account-settings.component';
import { CategoriesViewComponent } from './components/categories/categories-view.component';
import { projectResolver, projectsResolver } from './resolvers/project-resolver';
import { statusResolver } from './resolvers/status-resolver';
import { ticketResolver } from './resolvers/ticket.resolver';
import { ticketsResolver } from './resolvers/tickets-resolver';
import { userResolver, usersResolver } from './resolvers/users.resolver';
import { categoriesResolver } from './resolvers/categories-resolver';
import { workflowsResolver } from './resolvers/workflow-resolver';
import { authGuard } from './services/auth.guard';
import { roleGuard } from './services/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'login/reset-password', component: PasswordResetRequestComponent },
  { path: 'login/reset-password/:token', component: PasswordResetComponent },
  {
    path: '',
    component: HomeComponent,
    canActivate: [authGuard]
  },
  {
    path: 'project/:projectId/kanban',
    component: KanbanComponent,
    resolve: {
      project: projectResolver,
      statuses: statusResolver,
      tickets: ticketsResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'project/:projectId/dashboard',
    component: DashboardComponent,
    resolve: {
      project: projectResolver,
      statuses: statusResolver,
      tickets: ticketsResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'search',
    component: SearchTicketsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'tickets/new',
    component: CreateTicketComponent,
    resolve: {
      projects: projectsResolver,
      categories: categoriesResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'project/:projectId/tickets/new',
    component: CreateTicketComponent,
    resolve: {
      project: projectResolver,
      projects: projectsResolver,
      categories: categoriesResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'ticket/:ticketIdentifier',
    component: TicketViewComponent,
    resolve: {
      ticket: ticketResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'users',
    component: UsersViewComponent,
    resolve: {
      users: usersResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'users/new',
    component: UsersEditComponent,
    canActivate: [authGuard],
  },
  {
    path: 'users/:userId',
    component: UsersEditComponent,
    resolve: {
      user: userResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'projects',
    component: ProjectsViewComponent,
    resolve: {
      projects: projectsResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'projects/new',
    component: ProjectEditComponent,
    canActivate: [authGuard],
    resolve: {
      workflows: workflowsResolver,
      categories: categoriesResolver
    }
  },
  {
    path: 'projects/:projectId',
    component: ProjectEditComponent,
    resolve: {
      project: projectResolver,
      workflows: workflowsResolver,
      categories: categoriesResolver
    },
    canActivate: [authGuard]
  },
  {
    path: 'workflows',
    component: WorkflowsViewComponent,
    resolve: {
      workflows: workflowsResolver
    },
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'workflows/new',
    component: WorkflowCreateComponent,
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'workflows/:workflowId',
    component: WorkflowEditComponent,
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'account/settings',
    component: AccountSettingsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'categories',
    component: CategoriesViewComponent,
    resolve: {
      categories: categoriesResolver
    },
    canActivate: [authGuard, roleGuard(['admin'])],
  },
  {
    path: '',
    redirectTo: '/',
    pathMatch: 'full'
  },

  { path: '**', redirectTo: '/' }
];
