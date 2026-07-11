package dev.vepo.issues.project.serviceaccount.deactivate;

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
public class DeactivateServiceAccountEndpoint {

    private final ServiceAccountService serviceAccountService;

    @Inject
    public DeactivateServiceAccountEndpoint(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @DELETE
    @Path("/{serviceAccountId}")
    @ResponseStatus(204)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deactivateServiceAccount", summary = "Deactivate a project service account")
    public void deactivate(@PathParam("projectId") long projectId,
                           @PathParam("serviceAccountId") long serviceAccountId,
                           @Context SecurityContext securityContext) {
        serviceAccountService.deactivate(projectId, serviceAccountId, securityContext.getUserPrincipal().getName());
    }
}
