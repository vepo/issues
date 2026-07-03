package dev.vepo.issues.phase;

import java.util.regex.Pattern;

import jakarta.ws.rs.BadRequestException;

public final class SemVerValidator {

    private static final Pattern SEMVER = Pattern.compile(
                                                          "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private SemVerValidator() {}

    public static void requireValid(String label) {
        if (label == null || label.isBlank() || !SEMVER.matcher(label.trim()).matches()) {
            throw new BadRequestException("Version label must be valid SemVer (e.g. 1.0.0)");
        }
    }
}
