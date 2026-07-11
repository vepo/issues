package dev.vepo.issues.auth;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.auth.apitoken.ApiTokenHasher;
import dev.vepo.issues.mailer.MailerService;
import dev.vepo.issues.user.PasswordResetToken;
import dev.vepo.issues.user.PasswordResetTokenRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.user.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String LOCAL_PASSWORD_OPS_ONLY =
            "Password operations are only available with local authentication";
    private static final int USERNAME_MAX_LENGTH = 15;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApiTokenHasher tokenHasher;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final MailerService mailerService;
    private final Instance<CredentialAuthenticator> authenticators;
    private final AuthProvider activeProvider;
    private final int refreshTokenDays;

    @Inject
    public AuthenticationService(PasswordEncoder passwordEncoder,
                                 UserRepository userRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 ApiTokenHasher tokenHasher,
                                 JwtTokenIssuer jwtTokenIssuer,
                                 MailerService mailerService,
                                 Instance<CredentialAuthenticator> authenticators,
                                 @ConfigProperty(name = "auth.provider", defaultValue = "local") String authProviderConfig,
                                 @ConfigProperty(name = "auth.refresh-token-days", defaultValue = "30") int refreshTokenDays) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.jwtTokenIssuer = jwtTokenIssuer;
        this.mailerService = mailerService;
        this.authenticators = authenticators;
        this.activeProvider = AuthProvider.fromConfig(authProviderConfig);
        this.refreshTokenDays = refreshTokenDays;
    }

    public AuthCapabilitiesResponse capabilities() {
        var local = activeProvider.isLocal();
        return new AuthCapabilitiesResponse(activeProvider.configValue(), local, local);
    }

    public AuthProvider activeProvider() {
        return activeProvider;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        requireLocalPasswordOperations();
        userRepository.findByUsername(request.username())
                      .ifPresent(existing -> {
                          throw new BadRequestException("Username already in use");
                      });
        userRepository.findByEmail(request.email())
                      .ifPresent(existing -> {
                          throw new BadRequestException("Email already in use");
                      });
        var user = new User(request.username(),
                            request.name(),
                            request.email(),
                            passwordEncoder.hashPassword(request.password()),
                            Set.of(Role.USER),
                            AuthProvider.LOCAL);
        return UserResponse.load(userRepository.save(user));
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
        requireLocalPasswordOperations();
        var resetToken = passwordResetTokenRepository.findByToken(request.token())
                                                     .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (!resetToken.isValid()) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        var user = resetToken.getUser();
        user.setEncodedPassword(passwordEncoder.hashPassword(request.newPassword()));
        resetToken.setUsed(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        requireLocalPasswordOperations();
        var user = userRepository.findByUsername(username)
                                 .orElseThrow(() -> new NotFoundException("User not found!"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getEncodedPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setEncodedPassword(passwordEncoder.hashPassword(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Transactional
    public AuthResponse updateProfile(String username, UpdateProfileRequest request) {
        var user = userRepository.findByUsername(username)
                                 .orElseThrow(() -> new NotFoundException("User not found!"));
        userRepository.findByEmail(request.email())
                      .filter(other -> !other.getId().equals(user.getId()))
                      .ifPresent(other -> {
                          throw new BadRequestException("Email already in use");
                      });
        user.setName(request.name());
        user.setEmail(request.email());
        return AuthResponse.load(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        requireLocalPasswordOperations();
        userRepository.findByEmailOrUsername(request.credential())
                      .ifPresentOrElse(user -> {
                          passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
                          var rawToken = PasswordResetToken.generateRawToken();
                          var resetToken = new PasswordResetToken(user, tokenHasher.hash(rawToken), rawToken);
                          passwordResetTokenRepository.save(resetToken);
                          mailerService.sendResetPassword(user, resetToken);
                      },
                                       () -> logger.warn("User not found!! credential={}", request.credential()));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var identity = resolveAuthenticator().authenticate(request.email(), request.password());
        var user = resolveOrProvisionUser(identity);
        return issueTokens(user);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        var refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                                                 .orElseThrow(AuthFailures::invalidRefreshToken);
        if (!refreshToken.isValid()) {
            throw AuthFailures.invalidRefreshToken();
        }
        refreshTokenRepository.revokeTokenHash(refreshToken.getToken());
        return issueTokens(refreshToken.getUser());
    }

    public AuthResponse me(String username) {
        return userRepository.findByUsername(username)
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!"));
    }

    private CredentialAuthenticator resolveAuthenticator() {
        return authenticators.stream()
                             .filter(authenticator -> authenticator.provider() == activeProvider)
                             .findFirst()
                             .orElseThrow(() -> new IllegalStateException(
                                                                          "No CredentialAuthenticator registered for provider %s".formatted(activeProvider)));
    }

    private User resolveOrProvisionUser(VerifiedIdentity identity) {
        return userRepository.findByEmail(identity.email())
                             .map(existing -> updateExistingUser(existing, identity))
                             .orElseGet(() -> createExternalUser(identity));
    }

    private User updateExistingUser(User user, VerifiedIdentity identity) {
        if (identity.name() != null && !identity.name().isBlank()) {
            user.setName(identity.name());
        }
        user.setAuthProvider(identity.provider());
        if (identity.syncRoles()) {
            user.setRoles(new HashSet<>(identity.roles()));
        }
        return user;
    }

    private User createExternalUser(VerifiedIdentity identity) {
        if (identity.provider() == AuthProvider.LOCAL) {
            throw AuthFailures.invalidCredentials();
        }
        var preferredUsername = identity.username() != null && !identity.username().isBlank()
                                                                                              ? identity.username()
                                                                                              : emailLocalPart(identity.email());
        var username = allocateUniqueUsername(preferredUsername);
        var name = identity.name() != null && !identity.name().isBlank()
                                                                         ? identity.name()
                                                                         : username;
        var roles = identity.roles() == null || identity.roles().isEmpty()
                                                                           ? Set.of(Role.USER)
                                                                           : new HashSet<>(identity.roles());
        var user = new User(username, name, identity.email(), null, roles, identity.provider());
        return userRepository.save(user);
    }

    String allocateUniqueUsername(String preferred) {
        var base = sanitizeUsername(preferred);
        if (base.isEmpty()) {
            base = "user";
        }
        if (userRepository.findByUsername(base).isEmpty()) {
            return base;
        }
        var suffix = 1;
        while (suffix < 10_000) {
            var suffixText = Integer.toString(suffix);
            var maxBaseLength = USERNAME_MAX_LENGTH - suffixText.length();
            var truncated = base.length() <= maxBaseLength ? base : base.substring(0, maxBaseLength);
            var candidate = truncated + suffixText;
            if (userRepository.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
            suffix++;
        }
        throw new IllegalStateException("Unable to allocate unique username for %s".formatted(preferred));
    }

    static String sanitizeUsername(String raw) {
        if (raw == null) {
            return "";
        }
        var sanitized = raw.replaceAll("[^a-zA-Z0-9._-]", "");
        if (sanitized.length() > USERNAME_MAX_LENGTH) {
            return sanitized.substring(0, USERNAME_MAX_LENGTH);
        }
        return sanitized;
    }

    static String emailLocalPart(String email) {
        if (email == null || email.isBlank()) {
            return "user";
        }
        var at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private void requireLocalPasswordOperations() {
        if (!activeProvider.isLocal()) {
            throw new BadRequestException(LOCAL_PASSWORD_OPS_ONLY);
        }
    }

    private LoginResponse issueTokens(User user) {
        var rawRefresh = RefreshToken.generateRawToken();
        refreshTokenRepository.save(new RefreshToken(user,
                                                     refreshTokenDays,
                                                     tokenHasher.hash(rawRefresh),
                                                     rawRefresh));
        return new LoginResponse(jwtTokenIssuer.issueAccessToken(user),
                                 rawRefresh,
                                 jwtTokenIssuer.accessTokenExpiresInSeconds());
    }
}
