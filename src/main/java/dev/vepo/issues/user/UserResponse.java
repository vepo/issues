package dev.vepo.issues.user;

import java.util.List;

public record UserResponse(long id, String username, String name, String email, List<String> roles) {

    public static UserResponse load(User user) {
        return new UserResponse(user.getId(),
                                user.getUsername(),
                                user.getName(),
                                user.getEmail(),
                                user.getRoles()
                                    .stream()
                                    .map(Role::role)
                                    .toList());
    }
}
