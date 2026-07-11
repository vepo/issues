package dev.vepo.issues.project.serviceaccount;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.auth.apitoken.ApiTokenHasher;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.project.ProjectMemberRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final ProjectAccessService projectAccessService;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ApiTokenHasher apiTokenHasher;

    @Inject
    public ServiceAccountService(ServiceAccountRepository serviceAccountRepository,
                                 ProjectAccessService projectAccessService,
                                 ProjectMemberRepository projectMemberRepository,
                                 UserRepository userRepository,
                                 ApiTokenHasher apiTokenHasher) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.projectAccessService = projectAccessService;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.apiTokenHasher = apiTokenHasher;
    }

    @Transactional
    public ServiceAccountResponse create(long projectId, CreateServiceAccountRequest request, String username) {
        projectAccessService.requireManage(projectId, username);
        var project = projectAccessService.requireProject(projectId);
        var displayName = request.name().trim();
        if (displayName.isBlank()) {
            throw new BadRequestException("Service account name is required");
        }
        var linkedUser = createLinkedUser(displayName);
        projectMemberRepository.addMember(project, linkedUser);
        var serviceAccount = new ServiceAccount(project, linkedUser, displayName);
        serviceAccountRepository.persist(serviceAccount);
        return ServiceAccountResponse.load(serviceAccount);
    }

    public List<ServiceAccountResponse> list(long projectId, String username) {
        projectAccessService.requireManage(projectId, username);
        return serviceAccountRepository.listByProjectId(projectId)
                                       .stream()
                                       .map(ServiceAccountResponse::load)
                                       .toList();
    }

    @Transactional
    public void deactivate(long projectId, long serviceAccountId, String username) {
        projectAccessService.requireManage(projectId, username);
        var serviceAccount = requireServiceAccount(projectId, serviceAccountId);
        serviceAccount.deactivate();
    }

    @Transactional
    public CreatedServiceAccountTokenResponse createToken(long projectId,
                                                          long serviceAccountId,
                                                          CreateServiceAccountTokenRequest request,
                                                          String username) {
        projectAccessService.requireManage(projectId, username);
        var serviceAccount = requireServiceAccount(projectId, serviceAccountId);
        if (!serviceAccount.isActive()) {
            throw new BadRequestException("Service account is inactive");
        }
        var secret = apiTokenHasher.generateServiceAccountTokenSecret();
        var token = new ServiceAccountToken(serviceAccount,
                                            request.name().trim(),
                                            apiTokenHasher.hash(secret),
                                            apiTokenHasher.displayPrefix(secret));
        serviceAccountRepository.persistToken(token);
        return CreatedServiceAccountTokenResponse.load(token, secret);
    }

    @Transactional
    public void revokeToken(long projectId, long serviceAccountId, long tokenId, String username) {
        projectAccessService.requireManage(projectId, username);
        requireServiceAccount(projectId, serviceAccountId);
        var token = serviceAccountRepository.findTokenById(tokenId)
                                            .filter(t -> t.getServiceAccount().getId().equals(serviceAccountId))
                                            .orElseThrow(() -> new NotFoundException("Service account token not found"));
        if (!token.isRevoked()) {
            token.revoke();
        }
    }

    private ServiceAccount requireServiceAccount(long projectId, long serviceAccountId) {
        return serviceAccountRepository.findById(serviceAccountId)
                                       .filter(sa -> sa.getProject().getId().equals(projectId))
                                       .orElseThrow(() -> new NotFoundException("Service account not found"));
    }

    private User createLinkedUser(String displayName) {
        var suffix = UUID.randomUUID().toString().replace("-", "");
        var username = "sa%s".formatted(suffix.substring(0, 13));
        var email = "sa-%s@service-accounts.local".formatted(suffix);
        var user = new User(username, displayName, email, null, Set.of(Role.USER), AuthProvider.LOCAL);
        return userRepository.save(user);
    }
}
