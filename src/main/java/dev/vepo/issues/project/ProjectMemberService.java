package dev.vepo.issues.project;

import java.util.List;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.TicketResponse;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService accessService;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Inject
    public ProjectMemberService(ProjectMemberRepository memberRepository,
                                ProjectAccessService accessService,
                                UserRepository userRepository,
                                TicketRepository ticketRepository) {
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<ProjectMemberResponse> listMembers(long projectId, String username) {
        accessService.requireView(projectId, username);
        return memberRepository.findMembersByProjectId(projectId)
                               .stream()
                               .map(ProjectMemberResponse::load)
                               .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(long projectId, AddProjectMemberRequest request, String username) {
        accessService.requireManage(projectId, username);
        var project = accessService.requireProject(projectId);
        var user = requireUserById(request.userId());
        if (memberRepository.isMember(projectId, user.getId())) {
            throw new BadRequestException("User is already a member of this project");
        }
        memberRepository.addMember(project, user);
        return ProjectMemberResponse.load(user);
    }

    @Transactional
    public void removeMember(long projectId, long userId, String username) {
        accessService.requireManage(projectId, username);
        var project = accessService.requireProject(projectId);
        if (project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Cannot remove the project owner from members");
        }
        var openAssigned = ticketRepository.countOpenAssignedTickets(projectId, userId);
        if (openAssigned > 0) {
            throw new BadRequestException(
                                          "Cannot remove member with %d open assigned ticket(s); reassign tickets first".formatted(openAssigned));
        }
        if (!memberRepository.removeMember(projectId, userId)) {
            throw new NotFoundException("Member not found in project");
        }
    }

    public List<TicketResponse> listOpenAssignedTickets(long projectId, long userId, String username) {
        accessService.requireManage(projectId, username);
        return ticketRepository.findOpenAssignedTickets(projectId, userId)
                               .map(TicketResponse::load)
                               .toList();
    }

    @Transactional
    public void ensureMember(Project project, User user) {
        if (!memberRepository.isMember(project.getId(), user.getId())) {
            memberRepository.addMember(project, user);
        }
    }

    private User requireUserById(long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new NotFoundException("User with ID %d does not exist".formatted(userId)));
    }

    public User requireProjectManager(long userId) {
        var user = requireUserById(userId);
        if (!user.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new BadRequestException("Project owner must have the project-manager role");
        }
        return user;
    }
}
