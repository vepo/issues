package dev.vepo.issues.auth.local;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.AuthFailures;
import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.auth.CredentialAuthenticator;
import dev.vepo.issues.auth.PasswordEncoder;
import dev.vepo.issues.auth.VerifiedIdentity;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LocalCredentialAuthenticator implements CredentialAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(LocalCredentialAuthenticator.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public LocalCredentialAuthenticator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.LOCAL;
    }

    @Override
    public VerifiedIdentity authenticate(String email, String password) {
        var user = userRepository.findByEmail(email)
                                 .filter(u -> u.getEncodedPassword() != null)
                                 .filter(u -> passwordEncoder.matches(password, u.getEncodedPassword()))
                                 .orElseThrow(() -> {
                                     logger.debug("Local authentication failed for email={}", email);
                                     return AuthFailures.invalidCredentials();
                                 });
        if (passwordEncoder.needsRehash(user.getEncodedPassword())) {
            user.setEncodedPassword(passwordEncoder.hashPassword(password));
            logger.debug("Rehashed legacy password for user={}", user.getUsername());
        }
        return new VerifiedIdentity(user.getEmail(),
                                    user.getName(),
                                    user.getUsername(),
                                    new HashSet<>(user.getRoles()),
                                    AuthProvider.LOCAL,
                                    false);
    }
}
