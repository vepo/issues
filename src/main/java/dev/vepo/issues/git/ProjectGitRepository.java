package dev.vepo.issues.git;

import java.time.Instant;

import dev.vepo.issues.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_project_git_repositories")
public class ProjectGitRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "remote_url", nullable = false, length = 512)
    private String remoteUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private GitProvider provider;

    @Column(name = "default_branch", length = 128)
    private String defaultBranch;

    /**
     * Shared secret for forge HMAC verification. Stored recoverable so webhook
     * signatures can be checked (pre-production; encrypt-at-rest later).
     */
    @Column(name = "webhook_secret", length = 128)
    private String webhookSecret;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectGitRepository() {}

    public ProjectGitRepository(Project project, String remoteUrl, GitProvider provider, String defaultBranch) {
        this.project = project;
        this.remoteUrl = remoteUrl;
        this.provider = provider == null ? GitProvider.OTHER : provider;
        this.defaultBranch = defaultBranch;
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        touch();
    }

    public GitProvider getProvider() {
        return provider;
    }

    public void setProvider(GitProvider provider) {
        this.provider = provider == null ? GitProvider.OTHER : provider;
        touch();
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
        touch();
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
        touch();
    }

    public boolean hasSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
