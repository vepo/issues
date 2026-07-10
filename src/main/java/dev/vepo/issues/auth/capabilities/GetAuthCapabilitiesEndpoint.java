package dev.vepo.issues.auth.capabilities;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.auth.AuthCapabilitiesResponse;
import dev.vepo.issues.auth.AuthPaths;
import dev.vepo.issues.auth.AuthenticationService;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(AuthPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Auth")
public class GetAuthCapabilitiesEndpoint {

    private final AuthenticationService authenticationService;

    @Inject
    public GetAuthCapabilitiesEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GET
    @Path("/capabilities")
    @PermitAll
    @Operation(operationId = "getAuthCapabilities", summary = "Auth UI capabilities for the active provider")
    public AuthCapabilitiesResponse getAuthCapabilities() {
        return authenticationService.capabilities();
    }
}
