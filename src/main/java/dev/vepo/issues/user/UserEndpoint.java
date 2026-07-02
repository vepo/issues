package dev.vepo.issues.user;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ResponseStatus;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("users")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
public class UserEndpoint {
    private final UserRepository userRepository;
    private final String passwordDefault;

    @Inject
    public UserEndpoint(UserRepository userRepository,
                        @ConfigProperty(name = "password.default") String passwordDefault) {
        this.userRepository = userRepository;
        this.passwordDefault = passwordDefault;
    }

    @GET
    @Path("{userId}")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
    public UserResponse findUserById(@PathParam("userId") long userId) {
        return userRepository.findById(userId)
                             .map(UserResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!!! userId=%s".formatted(userId)));
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    @RolesAllowed(Role.ADMIN_ROLE)
    public UserResponse create(@Valid CreateUserRequest request) {
        return UserResponse.load(this.userRepository.save(new User(request.username(),
                                                                   request.name(),
                                                                   request.email(),
                                                                   passwordDefault,
                                                                   request.roles()
                                                                          .stream()
                                                                          .map(role -> Role.from(role)
                                                                                           .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                                                                          .collect(Collectors.toSet()))));
    }

    @POST
    @Path("{userId}")
    @Transactional
    @RolesAllowed(Role.ADMIN_ROLE)
    public UserResponse update(@PathParam("userId") long userId, @Valid CreateUserRequest request) {
        return UserResponse.load(this.userRepository.findById(userId)
                                                    .map(user -> {
                                                        user.setEmail(request.email());
                                                        user.setName(request.name());
                                                        user.setRoles(request.roles()
                                                                             .stream()
                                                                             .map(role -> Role.from(role)
                                                                                              .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                                                                             .collect(Collectors.toSet()));
                                                        this.userRepository.save(user);
                                                        return user;
                                                    })
                                                    .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId))));
    }

    @GET
    @Path("search")
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
    public List<UserResponse> search(@QueryParam("name") String name,
                                     @QueryParam("email") String email,
                                     @QueryParam("roles") List<String> roles) {
        return userRepository.search(name, email, roles.stream().map(role -> Role.from(role)
                                                                                 .orElseThrow(() -> new BadRequestException("Role does not exist! role=%s".formatted(role))))
                                                       .toList())
                             .map(UserResponse::load)
                             .toList();
    }

}
