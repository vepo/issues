package dev.vepo.issues.user;

import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings({ "java:S1192", "java:S1700" })
public enum Role {
    USER("user"),
    ADMIN("admin"),
    PROJECT_MANAGER("project-manager");

    public static final String PROJECT_MANAGER_ROLE = "project-manager";
    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";

    private final String role;

    Role(String role) {
        this.role = role;
    }

    public String role() {
        return role;
    }

    public static Optional<Role> from(String role) {
        return Stream.of(Role.values())
                     .filter(r -> r.role.equalsIgnoreCase(role))
                     .findFirst();
    }
}
