package dev.vepo.issues.user;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final String passwordDefault;

    @Inject
    public UserService(UserRepository userRepository,
                       @ConfigProperty(name = "password.default") String passwordDefault) {
        this.userRepository = userRepository;
        this.passwordDefault = passwordDefault;
    }

    public UserResponse findById(long userId) {
        return userRepository.findById(userId)
                             .map(UserResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!!! userId=%s".formatted(userId)));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        return UserResponse.load(userRepository.save(new User(request.username(),
                                                              request.name(),
                                                              request.email(),
                                                              passwordDefault,
                                                              parseRoles(request.roles()))));
    }

    @Transactional
    public UserResponse update(long userId, CreateUserRequest request) {
        return UserResponse.load(userRepository.findById(userId)
                                               .map(user -> {
                                                   user.setEmail(request.email());
                                                   user.setName(request.name());
                                                   user.setRoles(parseRoles(request.roles()));
                                                   userRepository.save(user);
                                                   return user;
                                               })
                                               .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId))));
    }

    public List<UserResponse> search(String name, String email, List<String> roles) {
        return userRepository.search(name,
                                     email,
                                     roles.stream()
                                          .map(role -> Role.from(role)
                                                           .orElseThrow(() -> new BadRequestException("Role does not exist! role=%s".formatted(role))))
                                          .toList())
                             .map(UserResponse::load)
                             .toList();
    }

    @Transactional
    public void delete(long userId) {
        var user = userRepository.findById(userId)
                                 .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId)));
        if (userRepository.countBlockingAssignedTickets(userId) > 0) {
            throw new BadRequestException("User cannot be deleted while assigned to open tickets");
        }
        userRepository.softDelete(user);
    }

    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User does not found! username=%s".formatted(username)));
    }

    public User requireById(long userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new NotFoundException("User does not found! userId=%d".formatted(userId)));
    }

    private java.util.Set<Role> parseRoles(List<String> roles) {
        return roles.stream()
                    .map(role -> Role.from(role)
                                     .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                    .collect(Collectors.toSet());
    }
}
