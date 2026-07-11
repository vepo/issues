package dev.vepo.issues.project.serviceaccount.revoketoken;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.project.serviceaccount.ServiceAccountPaths;
import dev.vepo.issues.project.serviceaccount.ServiceAccountService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path(ServiceAccountPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "ServiceAccount")
public class RevokeServiceAccountTokenEndpoint {

    private final ServiceAccountService serviceAccountService;

    @Inject
    public RevokeServiceAccountTokenEndpoint(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @DELETE
    @Path("/{serviceAccountId}/tokens/{tokenId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "revokeServiceAccountToken", summary = "Revoke a service account token")
    public void revoke(@PathParam("projectId") long projectId,
                       @PathParam("serviceAccountId") long serviceAccountId,
                       @PathParam("tokenId") long tokenId,
                       @Context SecurityContext securityContext) {
        serviceAccountService.revokeToken(projectId,
                                          serviceAccountId,
                                          tokenId,
                                          securityContext.getUserPrincipal().getName());
    }
}
