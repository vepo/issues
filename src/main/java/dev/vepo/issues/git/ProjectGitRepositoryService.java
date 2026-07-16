package dev.vepo.issues.git;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.issues.project.ProjectAccessService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProjectGitRepositoryService {

    private static final int SECRET_BYTES = 32;

    private final ProjectGitRepositoryRepository repository;
    private final ProjectAccessService accessService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String publicBaseUrl;

    @Inject
    public ProjectGitRepositoryService(ProjectGitRepositoryRepository repository,
                                       ProjectAccessService accessService,
                                       @ConfigProperty(name = "issues.public-base-url", defaultValue = "http://localhost:8080") String publicBaseUrl) {
        this.repository = repository;
        this.accessService = accessService;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    public Optional<ProjectGitRepositoryResponse> find(long projectId, String username) {
        accessService.requireManage(projectId, username);
        return repository.findByProjectId(projectId)
                         .map(repo -> ProjectGitRepositoryResponse.load(repo, webhookUrl(projectId)));
    }

    @Transactional
    public ProjectGitRepositoryResponse upsert(long projectId, ProjectGitRepositoryRequest request, String username) {
        accessService.requireManage(projectId, username);
        var project = accessService.requireProject(projectId);
        var existing = repository.findByProjectId(projectId);
        String plaintextSecretOnce = null;
        ProjectGitRepository entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setRemoteUrl(request.remoteUrl().trim());
            entity.setProvider(request.provider());
            entity.setDefaultBranch(blankToNull(request.defaultBranch()));
            if (!entity.hasSecret()) {
                plaintextSecretOnce = generateSecret();
                entity.setWebhookSecret(plaintextSecretOnce);
            }
        } else {
            entity = new ProjectGitRepository(project,
                                              request.remoteUrl().trim(),
                                              request.provider(),
                                              blankToNull(request.defaultBranch()));
            plaintextSecretOnce = generateSecret();
            entity.setWebhookSecret(plaintextSecretOnce);
        }
        repository.save(entity);
        return ProjectGitRepositoryResponse.load(entity, webhookUrl(projectId), plaintextSecretOnce);
    }

    @Transactional
    public ProjectGitRepositoryResponse regenerateSecret(long projectId, String username) {
        accessService.requireManage(projectId, username);
        var entity = repository.findByProjectId(projectId)
                               .orElseThrow(() -> new NotFoundException("Git repository is not configured for project %d".formatted(projectId)));
        var plaintext = generateSecret();
        entity.setWebhookSecret(plaintext);
        repository.save(entity);
        return ProjectGitRepositoryResponse.load(entity, webhookUrl(projectId), plaintext);
    }

    public ProjectGitRepository requireAssociation(long projectId) {
        return repository.findByProjectId(projectId)
                         .orElseThrow(() -> new NotFoundException("Git repository is not configured for project %d".formatted(projectId)));
    }

    public String webhookUrl(long projectId) {
        return "%s/api/projects/%d/git/webhook".formatted(publicBaseUrl, projectId);
    }

    private String generateSecret() {
        var bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
