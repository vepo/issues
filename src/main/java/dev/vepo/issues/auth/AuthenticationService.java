package dev.vepo.issues.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.mailer.MailerService;
import dev.vepo.issues.user.PasswordResetToken;
import dev.vepo.issues.user.PasswordResetTokenRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailerService mailerService;

    @Inject
    public AuthenticationService(PasswordEncoder passwordEncoder,
                                 UserRepository userRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 MailerService mailerService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailerService = mailerService;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        userRepository.findByEmailOrUsername(request.credential())
                      .ifPresentOrElse(user -> {
                          passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
                          var resetToken = new PasswordResetToken(user);
                          passwordResetTokenRepository.save(resetToken);
                          mailerService.sendResetPassword(user, resetToken);
                      },
                                       () -> logger.warn("User not found!! credential={}", request));
    }

    public LoginResponse login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                             .filter(u -> passwordEncoder.matches(request.password(), u.getEncodedPassword()))
                             .map(user -> {
                                 var now = Instant.now();
                                 return new LoginResponse(Jwt.issuer("https://issues.vepo.dev")
                                                             .upn(user.getUsername())
                                                             .claim("username", user.getUsername())
                                                             .claim("id", user.getId())
                                                             .claim("email", user.getEmail())
                                                             .groups(user.getRoles()
                                                                         .stream()
                                                                         .map(Role::role)
                                                                         .collect(Collectors.toSet()))
                                                             .issuedAt(now)
                                                             .expiresAt(now.plus(1, ChronoUnit.DAYS))
                                                             .sign());
                             })
                             .orElseThrow(() -> new NotAuthorizedException("Invalid credentials!", request));
    }

    public AuthResponse me(String username) {
        return userRepository.findByUsername(username)
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!"));
    }
}
