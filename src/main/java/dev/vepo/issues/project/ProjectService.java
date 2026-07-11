package dev.vepo.issues.project;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import dev.vepo.issues.categories.CategoryRepository;
import dev.vepo.issues.customfield.CustomFieldService;
import dev.vepo.issues.infra.PlainTextLength;
import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.workflow.WorkflowRepository;
import dev.vepo.issues.workflow.WorkflowResponse;

@ApplicationScoped
public class ProjectService {

    private final ProjectRepository repository;
    private final WorkflowRepository workflowRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectAccessService accessService;
    private final ProjectMemberService memberService;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CustomFieldService customFieldService;

    @Inject
    public ProjectService(ProjectRepository repository,
                          WorkflowRepository workflowRepository,
                          CategoryRepository categoryRepository,
                          ProjectAccessService accessService,
                          ProjectMemberService memberService,
                          UserRepository userRepository,
                          TicketRepository ticketRepository,
                          CustomFieldService customFieldService) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
        this.categoryRepository = categoryRepository;
        this.accessService = accessService;
        this.memberService = memberService;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.customFieldService = customFieldService;
    }

    @Transactional
    public List<ProjectResponse> listAll(String username) {
        var user = accessService.requireUser(username);
        return accessService.listProjectsForUser(user)
                            .stream()
                            .map(this::toResponse)
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
        applyCustomFieldTemplateDefaults(project, request.ticketTemplate());
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(long projectId, CreateProjectRequest request, String username) {
        accessService.requireManage(projectId, username);
        var user = accessService.requireUser(username);
        var project = accessService.requireProject(projectId);
        if (!project.getPrefix().equals(request.prefix()) && ticketRepository.countProjectTickets(projectId) > 0) {
            throw new BadRequestException("Project prefix cannot be changed while the project has tickets");
        }
        var previousWorkflowId = project.getWorkflow().getId();
        var newWorkflow = requireWorkflow(request.workflowId());
        if (!previousWorkflowId.equals(newWorkflow.getId())) {
            customFieldService.assertNoKeyCollisionOnWorkflowChange(projectId, newWorkflow.getId());
        }
        project.setName(request.name());
        project.setPrefix(request.prefix());
        project.setDescription(request.description());
        project.setWorkflow(newWorkflow);
        applyTicketTemplate(project, request.ticketTemplate());
        applyPhaseTemplate(project, request.phaseTemplate());
        applyOwnerTransfer(project, request.ownerId(), user);
        repository.save(project);
        if (!previousWorkflowId.equals(newWorkflow.getId())) {
            customFieldService.dropStaleTemplateDefaults(projectId, newWorkflow.getId());
        }
        applyCustomFieldTemplateDefaults(project, request.ticketTemplate());
        return toResponse(project);
    }

    public ProjectResponse findById(long projectId, String username) {
        accessService.requireView(projectId, username);
        return toResponse(accessService.requireProject(projectId));
    }

    private ProjectResponse toResponse(Project project) {
        var defaults = customFieldService.getTemplateDefaults(project.getId(), project.getWorkflow().getId());
        return ProjectResponse.load(project,
                                    ticketRepository.countProjectTickets(project.getId()) > 0,
                                    defaults);
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
        project.setTicketTemplateEnabled(true);
        project.setTicketTemplateTitle(normalizeTemplateText(template.title()));
        project.setTicketTemplateDescription(normalizeTemplateText(template.description()));
        project.setTicketTemplateCategoryId(template.categoryId());
        project.setTicketTemplatePriority(template.priority());
    }

    private void applyCustomFieldTemplateDefaults(Project project, TicketTemplateRequest template) {
        if (template == null || !template.enabled()) {
            customFieldService.setTemplateDefaults(project.getId(), project.getWorkflow().getId(), List.of());
            return;
        }
        customFieldService.setTemplateDefaults(project.getId(),
                                               project.getWorkflow().getId(),
                                               template.customFieldDefaults());
    }

    private String normalizeTemplateText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateTicketTemplate(TicketTemplateRequest template) {
        var hasTitle = template.title() != null && !template.title().isBlank();
        var hasDescription = template.description() != null && !template.description().isBlank();
        var hasCategory = template.categoryId() != null;
        var hasPriority = template.priority() != null;
        var hasCustomDefaults = template.customFieldDefaults() != null && !template.customFieldDefaults().isEmpty();

        if (!hasTitle && !hasDescription && !hasCategory && !hasPriority && !hasCustomDefaults) {
            throw new BadRequestException("Ticket template must configure at least one field");
        }
        if (hasTitle) {
            var title = template.title().trim();
            if (title.length() < 5 || title.length() > 255) {
                throw new BadRequestException("Ticket template title must be between 5 and 255 characters");
            }
        }
        if (hasDescription) {
            var description = template.description().trim();
            var plainLength = PlainTextLength.of(description);
            if (plainLength < 5 || plainLength > 1200) {
                throw new BadRequestException("Ticket template description must be between 5 and 1200 characters");
            }
        }
        if (hasCategory) {
            requireCategory(template.categoryId());
        }
        if (hasPriority && !List.of(TicketPriority.values()).contains(template.priority())) {
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
