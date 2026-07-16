package dev.vepo.issues.git;

public record ProjectGitRepositoryResponse(long projectId,
                                           String remoteUrl,
                                           GitProvider provider,
                                           String defaultBranch,
                                           String webhookUrl,
                                           boolean hasSecret,
                                           String webhookSecret) {

    public static ProjectGitRepositoryResponse load(ProjectGitRepository repository, String webhookUrl) {
        return load(repository, webhookUrl, null);
    }

    public static ProjectGitRepositoryResponse load(ProjectGitRepository repository, String webhookUrl, String plaintextSecretOnce) {
        return new ProjectGitRepositoryResponse(repository.getProject().getId(),
                                                repository.getRemoteUrl(),
                                                repository.getProvider(),
                                                repository.getDefaultBranch(),
                                                webhookUrl,
                                                repository.hasSecret(),
                                                plaintextSecretOnce);
    }
}
