package dev.vepo.issues.project;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
public class ProjectEndpoint {

    private static Supplier<NotFoundException> projectNotFound(long projectId) {
        return () -> new NotFoundException("Project with ID %d does not exist".formatted(projectId));
    }

    private static Supplier<NotFoundException> workflowNotFound(long workflowId) {
        return () -> new NotFoundException("Workflow with ID %d does not exist".formatted(workflowId));
    }

    private final ProjectRepository repository;
    private final WorkflowRepository workflowRepository;

    @Inject
    public ProjectEndpoint(ProjectRepository repository,
                           WorkflowRepository workflowRepository) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
    }

    @GET
    @Transactional
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    public List<ProjectResponse> listAllProjects() {
        return repository.findAll()
                         .map(ProjectResponse::load)
                         .toList();
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    @RolesAllowed(Role.PROJECT_MANAGER_ROLE)
    public ProjectResponse create(@Valid CreateProjectRequest request) {
        return ProjectResponse.load(repository.save(new Project(request.prefix(),
                                                                request.name(),
                                                                request.description(),
                                                                workflowRepository.findById(request.workflowId())
                                                                                  .orElseThrow(workflowNotFound(request.workflowId())))));
    }

    @POST
    @Path("{projectId}")
    @Transactional
    @ResponseStatus(201)
    @RolesAllowed(Role.PROJECT_MANAGER_ROLE)
    public ProjectResponse update(@PathParam("projectId") Long projectId, @Valid CreateProjectRequest request) {
        return ProjectResponse.load(repository.save(repository.findById(projectId)
                                                              .map(project -> {
                                                                  project.setName(request.name());
                                                                  project.setPrefix(request.prefix());
                                                                  project.setDescription(request.description());
                                                                  return project;
                                                              }).orElseThrow(projectNotFound(projectId))));
    }

    @GET
    @Path("{projectId}")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    public ProjectResponse getProjectById(@PathParam("projectId") long projectId) {
        return ProjectResponse.load(repository.findById(projectId)
                                              .orElseThrow(projectNotFound(projectId)));
    }

    @GET
    @Path("{projectId}/workflow")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    public WorkflowResponse getProjectWorkflow(@PathParam("projectId") long projectId) {
        return WorkflowResponse.load(repository.findById(projectId)
                                               .orElseThrow(projectNotFound(projectId))
                                               .getWorkflow());
    }

    @GET
    @Path("{projectId}/status")
    @RolesAllowed({ Role.PROJECT_MANAGER_ROLE, Role.ADMIN_ROLE, Role.USER_ROLE })
    public List<ProjectStatusResponse> getAllTicketStatusByProject(@PathParam("projectId") long projectId) {
        return repository.findById(projectId)
                         .map(project -> project.getWorkflow()
                                                .getStatuses()
                                                .stream()
                                                .map(status -> ProjectStatusResponse.load(status, project.getWorkflow()))
                                                .toList())
                         .orElseThrow(projectNotFound(projectId));
    }
}