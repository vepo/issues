package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProjectService {

    private final ProjectRepository repository;
    private final WorkflowRepository workflowRepository;
    private final CategoryRepository categoryRepository;

    @Inject
    public ProjectService(ProjectRepository repository,
                          WorkflowRepository workflowRepository,
                          CategoryRepository categoryRepository) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public List<ProjectResponse> listAll() {
        return repository.findAll()
                         .map(ProjectResponse::load)
                         .toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        var project = new Project(request.prefix(),
                                  request.name(),
                                  request.description(),
                                  requireWorkflow(request.workflowId()));
        applyTicketTemplate(project, request.ticketTemplate());
        return ProjectResponse.load(repository.save(project));
    }

    @Transactional
    public ProjectResponse update(long projectId, CreateProjectRequest request) {
        return ProjectResponse.load(repository.save(repository.findById(projectId)
                                                              .map(project -> {
                                                                  project.setName(request.name());
                                                                  project.setPrefix(request.prefix());
                                                                  project.setDescription(request.description());
                                                                  project.setWorkflow(requireWorkflow(request.workflowId()));
                                                                  applyTicketTemplate(project, request.ticketTemplate());
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

    private void applyTicketTemplate(Project project, TicketTemplateRequest template) {
        if (template == null || !template.enabled()) {
            project.clearTicketTemplate();
            return;
        }
        validateTicketTemplate(template);
        requireCategory(template.categoryId());
        project.setTicketTemplateEnabled(true);
        project.setTicketTemplateTitle(template.title().trim());
        project.setTicketTemplateDescription(template.description().trim());
        project.setTicketTemplateCategoryId(template.categoryId());
        project.setTicketTemplatePriority(template.priority());
    }

    private void validateTicketTemplate(TicketTemplateRequest template) {
        if (template.title() == null || template.title().isBlank()) {
            throw new BadRequestException("Ticket template title cannot be empty");
        }
        if (template.title().length() < 5 || template.title().length() > 255) {
            throw new BadRequestException("Ticket template title must be between 5 and 255 characters");
        }
        if (template.description() == null || template.description().isBlank()) {
            throw new BadRequestException("Ticket template description cannot be empty");
        }
        if (template.description().length() < 5 || template.description().length() > 1200) {
            throw new BadRequestException("Ticket template description must be between 5 and 1200 characters");
        }
        if (template.categoryId() == null) {
            throw new BadRequestException("Ticket template category ID must be provided");
        }
        if (template.priority() == null) {
            throw new BadRequestException("Ticket template priority must be provided");
        }
        if (!List.of(TicketPriority.values()).contains(template.priority())) {
            throw new BadRequestException("Ticket template priority is invalid");
        }
    }

    private void requireCategory(long categoryId) {
        categoryRepository.findById(categoryId)
                          .orElseThrow(() -> new NotFoundException("Category with ID %d does not exist".formatted(categoryId)));
    }

    private dev.vepo.issues.workflow.Workflow requireWorkflow(long workflowId) {
        return workflowRepository.findById(workflowId)
                                 .orElseThrow(() -> new NotFoundException("Workflow with ID %d does not exist".formatted(workflowId)));
    }

    private NotFoundException projectNotFound(long projectId) {
        return new NotFoundException("Project with ID %d does not exist".formatted(projectId));
    }
}
