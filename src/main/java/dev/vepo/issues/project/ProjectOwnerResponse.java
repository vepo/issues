package dev.vepo.issues.project;

import dev.vepo.issues.user.User;

public record ProjectOwnerResponse(long id, String name, String email) {

    public static ProjectOwnerResponse load(User user) {
        return new ProjectOwnerResponse(user.getId(), user.getName(), user.getEmail());
    }
}
