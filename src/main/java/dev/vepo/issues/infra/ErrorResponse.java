package dev.vepo.issues.infra;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Standardized error payload for API failures.")
public record ErrorResponse(@Schema(description = "HTTP-compatible error code (e.g., 400, 401).") int code,
                            @Schema(description = "Human-readable error explanation.") String message) {

}
