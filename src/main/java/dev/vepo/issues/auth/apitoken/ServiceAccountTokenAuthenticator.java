package dev.vepo.issues.auth.apitoken;

import java.time.Instant;
import java.util.Optional;

import dev.vepo.issues.project.serviceaccount.ServiceAccountRepository;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ServiceAccountTokenAuthenticator {

    private final ApiTokenHasher apiTokenHasher;
    private final ServiceAccountRepository serviceAccountRepository;

    @Inject
    public ServiceAccountTokenAuthenticator(ApiTokenHasher apiTokenHasher,
                                            ServiceAccountRepository serviceAccountRepository) {
        this.apiTokenHasher = apiTokenHasher;
        this.serviceAccountRepository = serviceAccountRepository;
    }

    @Transactional
    public Optional<User> authenticate(String rawToken) {
        if (!apiTokenHasher.isServiceAccountToken(rawToken)) {
            return Optional.empty();
        }
        return serviceAccountRepository.findActiveTokenByHash(apiTokenHasher.hash(rawToken))
                                       .map(token -> {
                                           token.setLastUsedAt(Instant.now());
                                           return token.getServiceAccount().getUser();
                                       });
    }
}
