package dev.vepo.issues.project;

import dev.vepo.issues.user.User;

public record ProjectMemberResponse(long id, String username, String name, String email) {

    public static ProjectMemberResponse load(User user) {
        return new ProjectMemberResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail());
    }
}
