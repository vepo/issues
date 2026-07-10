package dev.vepo.issues.auth;

import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.mailer.MailerService;
import dev.vepo.issues.user.PasswordResetToken;
import dev.vepo.issues.user.PasswordResetTokenRepository;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import dev.vepo.issues.user.UserRepository;
import dev.vepo.issues.user.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final MailerService mailerService;
    private final int refreshTokenDays;

    @Inject
    public AuthenticationService(PasswordEncoder passwordEncoder,
                                 UserRepository userRepository,
                                 PasswordResetTokenRepository passwordResetTokenRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 JwtTokenIssuer jwtTokenIssuer,
                                 MailerService mailerService,
                                 @ConfigProperty(name = "auth.refresh-token-days", defaultValue = "30") int refreshTokenDays) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenIssuer = jwtTokenIssuer;
        this.mailerService = mailerService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
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
                            Set.of(Role.USER));
        return UserResponse.load(userRepository.save(user));
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
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
        userRepository.findByEmailOrUsername(request.credential())
                      .ifPresentOrElse(user -> {
                          passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
                          var resetToken = new PasswordResetToken(user);
                          passwordResetTokenRepository.save(resetToken);
                          mailerService.sendResetPassword(user, resetToken);
                      },
                                       () -> logger.warn("User not found!! credential={}", request.credential()));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                             .filter(u -> passwordEncoder.matches(request.password(), u.getEncodedPassword()))
                             .map(this::issueTokens)
                             .orElseThrow(() -> new NotAuthorizedException("Invalid credentials!", request));
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        var refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                                                 .orElseThrow(() -> new NotAuthorizedException("Invalid refresh token"));
        if (!refreshToken.isValid()) {
            throw new NotAuthorizedException("Invalid refresh token");
        }
        refreshTokenRepository.revokeToken(refreshToken.getToken());
        return issueTokens(refreshToken.getUser());
    }

    public AuthResponse me(String username) {
        return userRepository.findByUsername(username)
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!"));
    }

    private LoginResponse issueTokens(User user) {
        var refreshToken = refreshTokenRepository.save(new RefreshToken(user, refreshTokenDays));
        return new LoginResponse(jwtTokenIssuer.issueAccessToken(user),
                                 refreshToken.getToken(),
                                 jwtTokenIssuer.accessTokenExpiresInSeconds());
    }
}
