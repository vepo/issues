package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
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
    private final ProjectAccessService accessService;
    private final ProjectMemberService memberService;
    private final UserRepository userRepository;

    @Inject
    public ProjectService(ProjectRepository repository,
                          WorkflowRepository workflowRepository,
                          CategoryRepository categoryRepository,
                          ProjectAccessService accessService,
                          ProjectMemberService memberService,
                          UserRepository userRepository) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
        this.categoryRepository = categoryRepository;
        this.accessService = accessService;
        this.memberService = memberService;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<ProjectResponse> listAll(String username) {
        var user = accessService.requireUser(username);
        return accessService.listProjectsForUser(user)
                            .stream()
                            .map(ProjectResponse::load)
                            .toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, String creatorUsername) {
        var creator = accessService.requireUser(creatorUsername);
        if (!creator.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new BadRequestException("Only project managers can create projects");
        }
        var owner = resolveOwner(request.ownerId(), creator);
        var project = new Project(request.prefix(),
                                  request.name(),
                                  request.description(),
                                  requireWorkflow(request.workflowId()),
                                  owner);
        applyTicketTemplate(project, request.ticketTemplate());
        applyPhaseTemplate(project, request.phaseTemplate());
        repository.save(project);
        memberService.ensureMember(project, owner);
        if (!owner.getId().equals(creator.getId())) {
            memberService.ensureMember(project, creator);
        }
        return ProjectResponse.load(project);
    }

    @Transactional
    public ProjectResponse update(long projectId, CreateProjectRequest request, String username) {
        accessService.requireManage(projectId, username);
        var user = accessService.requireUser(username);
        var project = accessService.requireProject(projectId);
        project.setName(request.name());
        project.setPrefix(request.prefix());
        project.setDescription(request.description());
        project.setWorkflow(requireWorkflow(request.workflowId()));
        applyTicketTemplate(project, request.ticketTemplate());
        applyPhaseTemplate(project, request.phaseTemplate());
        applyOwnerTransfer(project, request.ownerId(), user);
        return ProjectResponse.load(repository.save(project));
    }

    public ProjectResponse findById(long projectId, String username) {
        accessService.requireView(projectId, username);
        return ProjectResponse.load(accessService.requireProject(projectId));
    }

    public WorkflowResponse findWorkflow(long projectId, String username) {
        accessService.requireView(projectId, username);
        return WorkflowResponse.load(accessService.requireProject(projectId).getWorkflow());
    }

    public List<ProjectStatusResponse> listStatuses(long projectId, String username) {
        accessService.requireView(projectId, username);
        var project = accessService.requireProject(projectId);
        return project.getWorkflow()
                      .getStatuses()
                      .stream()
                      .map(status -> ProjectStatusResponse.load(status, project.getWorkflow()))
                      .toList();
    }

    public Project requireProject(long projectId) {
        return accessService.requireProject(projectId);
    }

    private User resolveOwner(Long ownerId, User creator) {
        if (ownerId == null) {
            return memberService.requireProjectManager(creator.getId());
        }
        var owner = memberService.requireProjectManager(ownerId);
        return owner;
    }

    private void applyOwnerTransfer(Project project, Long ownerId, User actingUser) {
        if (ownerId == null || ownerId.equals(project.getOwner().getId())) {
            return;
        }
        if (!accessService.isAdmin(actingUser) && !accessService.isProjectOwner(actingUser, project)) {
            throw new jakarta.ws.rs.ForbiddenException("Only the project owner or an admin may transfer ownership");
        }
        var newOwner = memberService.requireProjectManager(ownerId);
        project.setOwner(newOwner);
        memberService.ensureMember(project, newOwner);
    }

    private void applyPhaseTemplate(Project project, PhaseTemplateRequest template) {
        if (template == null) {
            project.setPhaseTemplateObjective(null);
            project.getPhaseDeliverableTemplates().clear();
            return;
        }
        project.setPhaseTemplateObjective(template.objective());
        replacePhaseDeliverableTemplates(project, template.deliverables());
    }

    private void replacePhaseDeliverableTemplates(Project project, java.util.List<String> deliverables) {
        project.getPhaseDeliverableTemplates().clear();
        if (deliverables == null) {
            return;
        }
        var order = 0;
        for (var text : deliverables) {
            if (text == null || text.isBlank()) {
                continue;
            }
            project.getPhaseDeliverableTemplates().add(new ProjectPhaseDeliverableTemplate(project, order++, text.trim()));
        }
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
}
