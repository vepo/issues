import { Routes } from '@angular/router';
import { CreateTicketComponent } from './components/create-ticket/create-ticket.component';
import { TicketImportWizardComponent } from './components/ticket-import/ticket-import-wizard.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { BacklogComponent } from './components/backlog/backlog.component';
import { KanbanComponent } from './components/kanban/kanban.component';
import { BurndownComponent } from './components/burndown/burndown.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { PasswordResetRequestComponent } from './components/password-reset-request/password-reset-request.component';
import { PasswordResetComponent } from './components/password-reset/password-reset.component';
import { ProjectEditComponent } from './components/project-edit.component/project-edit.component';
import { ProjectAllocationComponent } from './components/projects/project-allocation.component';
import { ProjectHubComponent } from './components/projects/project-hub.component';
import { ServiceAccountsComponent } from './components/projects/service-accounts.component';
import { ProjectsViewComponent } from './components/projects-view.component/projects-view.component';
import { SearchTicketsComponent } from './components/search-tickets/search-tickets.component';
import { AdvancedSearchComponent } from './components/advanced-search/advanced-search.component';
import { SavedQueryListComponent } from './components/saved-query-list/saved-query-list.component';
import { SavedQueryEditComponent } from './components/saved-query-edit/saved-query-edit.component';
import { SavedQueryViewComponent } from './components/saved-query-view/saved-query-view.component';
import { TicketViewComponent } from './components/ticket-view/ticket-view.component';
import { UsersEditComponent } from './components/users-edit.component/users-edit.component';
import { UsersViewComponent } from './components/users-view.component/users-view.component';
import { WorkflowsViewComponent } from './components/workflows/workflows-view.component';
import { WorkflowCreateComponent } from './components/workflows/workflow-create.component';
import { WorkflowEditComponent } from './components/workflows/workflow-edit.component';
import { AccountSettingsComponent } from './components/account-settings/account-settings.component';
import { CategoriesViewComponent } from './components/categories/categories-view.component';
import { VersionsViewComponent } from './components/versions/versions-view.component';
import { VersionDetailComponent } from './components/versions/version-detail.component';
import { PhasesViewComponent } from './components/phases/phases-view.component';
import { PhaseDetailComponent } from './components/phases/phase-detail.component';
import { projectResolver, projectsResolver } from './resolvers/project-resolver';
import { statusResolver } from './resolvers/status-resolver';
import { ticketResolver } from './resolvers/ticket.resolver';
import { ticketsResolver } from './resolvers/tickets-resolver';
import { userResolver, usersResolver } from './resolvers/users.resolver';
import { categoriesResolver } from './resolvers/categories-resolver';
import { versionResolver, versionsResolver } from './resolvers/versions-resolver';
import { phaseResolver, phasesResolver } from './resolvers/phases-resolver';
import { workflowsResolver } from './resolvers/workflow-resolver';
import { authGuard } from './services/auth.guard';
import { projectManageGuard } from './services/project-manage.guard';
import { publicReadGuard } from './services/public-read.guard';
import { roleGuard } from './services/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'login/register', component: RegisterComponent },
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
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/burndown',
    component: BurndownComponent,
    resolve: {
      project: projectResolver,
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/backlog',
    component: BacklogComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/dashboard',
    component: DashboardComponent,
    resolve: {
      project: projectResolver,
      statuses: statusResolver,
      tickets: ticketsResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/versions',
    component: VersionsViewComponent,
    resolve: {
      project: projectResolver,
      versions: versionsResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/versions/new',
    component: VersionDetailComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'project/:projectId/versions/:versionId',
    component: VersionDetailComponent,
    resolve: {
      project: projectResolver,
      version: versionResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/phases',
    component: PhasesViewComponent,
    resolve: {
      project: projectResolver,
      phases: phasesResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'project/:projectId/phases/new',
    component: PhaseDetailComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'project/:projectId/phases/:phaseId',
    component: PhaseDetailComponent,
    resolve: {
      project: projectResolver,
      phase: phaseResolver
    },
    canActivate: [publicReadGuard],
  },
  {
    path: 'search',
    component: SearchTicketsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'search/advanced',
    component: AdvancedSearchComponent,
    canActivate: [authGuard],
  },
  {
    path: 'search/queries',
    component: SavedQueryListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'search/queries/new',
    component: SavedQueryEditComponent,
    canActivate: [authGuard],
  },
  {
    path: 'search/queries/:id/edit',
    component: SavedQueryEditComponent,
    canActivate: [authGuard],
  },
  {
    path: 'search/q/:slug',
    component: SavedQueryViewComponent,
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
    path: 'tickets/import',
    component: TicketImportWizardComponent,
    resolve: {
      projects: projectsResolver,
      categories: categoriesResolver
    },
    canActivate: [authGuard],
  },
  {
    path: 'project/:projectId/tickets/import',
    component: TicketImportWizardComponent,
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
    canActivate: [publicReadGuard],
  },
  {
    path: 'users',
    component: UsersViewComponent,
    resolve: {
      users: usersResolver
    },
    canActivate: [authGuard, roleGuard(['admin'])],
  },
  {
    path: 'users/new',
    component: UsersEditComponent,
    canActivate: [authGuard, roleGuard(['admin'])],
  },
  {
    path: 'users/:userId',
    component: UsersEditComponent,
    resolve: {
      user: userResolver
    },
    canActivate: [authGuard, roleGuard(['admin'])],
  },
  {
    path: 'projects',
    component: ProjectsViewComponent,
    resolve: {
      projects: projectsResolver
    },
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])],
  },
  {
    path: 'projects/new',
    component: ProjectEditComponent,
    canActivate: [authGuard, roleGuard(['project-manager'])],
    resolve: {
      workflows: workflowsResolver,
      categories: categoriesResolver
    }
  },
  {
    path: 'projects/:projectId',
    component: ProjectHubComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [publicReadGuard]
  },
  {
    path: 'projects/:projectId/edit',
    component: ProjectEditComponent,
    resolve: {
      project: projectResolver,
      workflows: workflowsResolver,
      categories: categoriesResolver
    },
    canActivate: [authGuard, projectManageGuard]
  },
  {
    path: 'projects/:projectId/allocation',
    component: ProjectAllocationComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [authGuard, projectManageGuard]
  },
  {
    path: 'projects/:projectId/service-accounts',
    component: ServiceAccountsComponent,
    resolve: {
      project: projectResolver
    },
    canActivate: [authGuard, roleGuard(['admin', 'project-manager'])]
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
