package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProjectService {

    private final ProjectRepository repository;
    private final WorkflowRepository workflowRepository;

    @Inject
    public ProjectService(ProjectRepository repository, WorkflowRepository workflowRepository) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public List<ProjectResponse> listAll() {
        return repository.findAll()
                         .map(ProjectResponse::load)
                         .toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        return ProjectResponse.load(repository.save(new Project(request.prefix(),
                                                                request.name(),
                                                                request.description(),
                                                                requireWorkflow(request.workflowId()))));
    }

    @Transactional
    public ProjectResponse update(long projectId, CreateProjectRequest request) {
        return ProjectResponse.load(repository.save(repository.findById(projectId)
                                                              .map(project -> {
                                                                  project.setName(request.name());
                                                                  project.setPrefix(request.prefix());
                                                                  project.setDescription(request.description());
                                                                  return project;
                                                              })
                                                              .orElseThrow(() -> projectNotFound(projectId))));
    }

    public ProjectResponse findById(long projectId) {
        return ProjectResponse.load(requireProject(projectId));
    }

    public WorkflowResponse findWorkflow(long projectId) {
        return WorkflowResponse.load(requireProject(projectId).getWorkflow());
    }

    public List<ProjectStatusResponse> listStatuses(long projectId) {
        var project = requireProject(projectId);
        return project.getWorkflow()
                      .getStatuses()
                      .stream()
                      .map(status -> ProjectStatusResponse.load(status, project.getWorkflow()))
                      .toList();
    }

    public Project requireProject(long projectId) {
        return repository.findById(projectId)
                         .orElseThrow(() -> projectNotFound(projectId));
    }

    private dev.vepo.issues.workflow.Workflow requireWorkflow(long workflowId) {
        return workflowRepository.findById(workflowId)
                                 .orElseThrow(() -> new NotFoundException("Workflow with ID %d does not exist".formatted(workflowId)));
    }

    private NotFoundException projectNotFound(long projectId) {
        return new NotFoundException("Project with ID %d does not exist".formatted(projectId));
    }
}
