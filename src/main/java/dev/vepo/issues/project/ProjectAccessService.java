package dev.vepo.issues.project;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Inject
    public ProjectAccessService(ProjectRepository projectRepository,
                                ProjectMemberRepository memberRepository,
                                UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN);
    }

    public boolean isProjectOwner(User user, Project project) {
        return project.getOwner() != null && project.getOwner().getId().equals(user.getId());
    }

    /**
     * Membership (or admin) — used for writes and member-scoped operations.
     */
    public boolean canViewProject(User user, Project project) {
        return isAdmin(user) || memberRepository.isMember(project.getId(), user.getId());
    }

    /**
     * Read access by project security level (anonymous allowed only for PUBLIC).
     */
    public boolean canRead(Optional<User> user, Project project) {
        var level = project.getSecurityLevel() == null ? SecurityLevel.INTERNAL : project.getSecurityLevel();
        return switch (level) {
            case PUBLIC -> true;
            case INTERNAL -> user.isPresent();
            case PRIVATE -> user.map(u -> isAdmin(u) || isProjectOwner(u, project)
                    || memberRepository.isMember(project.getId(), u.getId()))
                                .orElse(false);
        };
    }

    public boolean canManageProject(User user, Project project) {
        return isAdmin(user) || isProjectOwner(user, project);
    }

    public void requireView(long projectId, String username) {
        var user = requireUser(username);
        var project = requireProject(projectId);
        if (!canViewProject(user, project)) {
            throw new ForbiddenException("Access denied to project %d".formatted(projectId));
        }
    }

    public void requireRead(long projectId, String username) {
        requireRead(projectId, Optional.of(username));
    }

    public void requireRead(long projectId, Optional<String> username) {
        var project = requireProject(projectId);
        var user = username.map(this::requireUser);
        if (!canRead(user, project)) {
            throw new ForbiddenException("Access denied to project %d".formatted(projectId));
        }
    }

    public void requireManage(long projectId, String username) {
        var user = requireUser(username);
        var project = requireProject(projectId);
        if (!canManageProject(user, project)) {
            throw new ForbiddenException("Only the project owner or an admin may manage project %d".formatted(projectId));
        }
    }

    public Set<Long> projectScopeIds(User user) {
        if (isAdmin(user)) {
            return new HashSet<>(projectRepository.findAllIds());
        }
        var ids = new HashSet<Long>();
        memberRepository.findProjectIdsForMember(user.getId()).forEach(ids::add);
        projectRepository.findOwnedProjectIds(user.getId()).forEach(ids::add);
        return ids;
    }

    /**
     * Project ids the user may read under security levels (for search filtering).
     */
    public Set<Long> readableProjectIds(User user) {
        if (isAdmin(user)) {
            return new HashSet<>(projectRepository.findAllIds());
        }
        var ids = new HashSet<Long>();
        memberRepository.findProjectIdsForMember(user.getId()).forEach(ids::add);
        projectRepository.findOwnedProjectIds(user.getId()).forEach(ids::add);
        projectRepository.findIdsBySecurityLevels(List.of(SecurityLevel.INTERNAL, SecurityLevel.PUBLIC))
                         .forEach(ids::add);
        return ids;
    }

    public List<Project> listProjectsForUser(User user) {
        return listReadableProjects(user);
    }

    public List<Project> listReadableProjects(User user) {
        if (isAdmin(user)) {
            return projectRepository.findAll().toList();
        }
        var byId = new LinkedHashMap<Long, Project>();
        projectRepository.findByMemberUserId(user.getId())
                         .forEach(project -> byId.put(project.getId(), project));
        projectRepository.findOwnedByUserId(user.getId())
                         .forEach(project -> byId.put(project.getId(), project));
        projectRepository.findBySecurityLevels(List.of(SecurityLevel.INTERNAL, SecurityLevel.PUBLIC))
                         .forEach(project -> byId.put(project.getId(), project));
        return byId.values()
                   .stream()
                   .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                   .toList();
    }

    public List<Project> listPublicProjects() {
        return projectRepository.findBySecurityLevels(List.of(SecurityLevel.PUBLIC))
                                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                                .toList();
    }

    public Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new NotFoundException("Project with ID %d does not exist".formatted(projectId)));
    }

    public void requireProjectMember(long projectId, long userId) {
        if (!memberRepository.isMember(projectId, userId)) {
            throw new BadRequestException("User is not a member of this project");
        }
    }
}
