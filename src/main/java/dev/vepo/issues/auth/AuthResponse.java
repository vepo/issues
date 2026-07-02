package dev.vepo.issues.auth;

import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;

public record AuthResponse(long id, String username, String name, String email, Set<String> roles) {

    public static AuthResponse load(User user) {
        return new AuthResponse(user.getId(),
                                user.getUsername(),
                                user.getName(),
                                user.getEmail(),
                                user.getRoles().stream()
                                    .map(Role::role)
                                    .collect(Collectors.toSet()));
    }
}