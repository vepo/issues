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
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEndpoint.class);
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private MailerService mailerService;

    @Inject
    public AuthenticationEndpoint(PasswordEncoder passwordEncoder,
                                  UserRepository userRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  MailerService mailerService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailerService = mailerService;
    }

    @POST
    @Transactional
    @Path("/recovery")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        userRepository.findByEmailOrUsername(request.credential())
                      .ifPresentOrElse(user -> {
                          passwordResetTokenRepository.invalidateAllUserTokens(user.getId());
                          var resetToken = new PasswordResetToken(user);
                          passwordResetTokenRepository.save(resetToken);
                          mailerService.sendResetPassword(user, resetToken);
                      },
                                       () -> logger.warn("User not found!! credential={}", request));
        return Response.ok().build();
    }

    @POST
    @Path("/login")
    public LoginResponse login(@Valid LoginRequest request) {
        return this.userRepository.findByEmail(request.email())
                                  .filter(u -> passwordEncoder.matches(request.password(), u.getEncodedPassword()))
                                  .map(user -> {
                                      Instant now = Instant.now();
                                      return new LoginResponse(Jwt.issuer("https://issues.vepo.dev")
                                                                  .upn(user.getUsername())
                                                                  .claim("username", user.getUsername())
                                                                  .claim("id", user.getId())
                                                                  .claim("email", user.getEmail())
                                                                  .groups(user.getRoles().stream()
                                                                              .map(Role::role)
                                                                              .collect(Collectors.toSet()))
                                                                  .issuedAt(now)
                                                                  .expiresAt(now.plus(1, ChronoUnit.DAYS))
                                                                  .sign());
                                  })
                                  .orElseThrow(() -> new NotAuthorizedException("Invalid credentials!", request));
    }

    @GET
    @Path("/me")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public AuthResponse me(@Context SecurityContext ctx) {
        return userRepository.findByUsername(ctx.getUserPrincipal().getName())
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!"));
    }
}