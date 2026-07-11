package dev.vepo.issues.auth.apitoken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ApiTokenService {

    private final ApiTokenRepository apiTokenRepository;
    private final ApiTokenHasher apiTokenHasher;
    private final UserRepository userRepository;

    @Inject
    public ApiTokenService(ApiTokenRepository apiTokenRepository,
                           ApiTokenHasher apiTokenHasher,
                           UserRepository userRepository) {
        this.apiTokenRepository = apiTokenRepository;
        this.apiTokenHasher = apiTokenHasher;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreatedApiTokenResponse create(String username, CreateApiTokenRequest request) {
        var user = requireUser(username);
        var secret = apiTokenHasher.generatePersonalApiTokenSecret();
        var token = new ApiToken(user, request.name().trim(), apiTokenHasher.hash(secret), apiTokenHasher.displayPrefix(secret));
        apiTokenRepository.persist(token);
        return CreatedApiTokenResponse.load(token, secret);
    }

    public List<ApiTokenResponse> list(String username) {
        var user = requireUser(username);
        return apiTokenRepository.listByUserId(user.getId())
                                 .stream()
                                 .map(ApiTokenResponse::load)
                                 .toList();
    }

    @Transactional
    public void revoke(String username, long tokenId) {
        var user = requireUser(username);
        var token = apiTokenRepository.findById(tokenId)
                                      .filter(t -> t.getUser().getId().equals(user.getId()))
                                      .orElseThrow(() -> new NotFoundException("API token not found"));
        if (!token.isRevoked()) {
            token.revoke();
        }
    }

    @Transactional
    public Optional<User> authenticatePersonalApiToken(String rawToken) {
        if (!apiTokenHasher.isPersonalApiToken(rawToken)) {
            return Optional.empty();
        }
        return apiTokenRepository.findActiveByTokenHash(apiTokenHasher.hash(rawToken))
                                 .map(token -> {
                                     token.setLastUsedAt(Instant.now());
                                     return token.getUser();
                                 });
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
