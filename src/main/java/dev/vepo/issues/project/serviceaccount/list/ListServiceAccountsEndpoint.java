package dev.vepo.issues.project.serviceaccount.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.project.serviceaccount.ServiceAccountPaths;
import dev.vepo.issues.project.serviceaccount.ServiceAccountResponse;
import dev.vepo.issues.project.serviceaccount.ServiceAccountService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ServiceAccountPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "ServiceAccount")
public class ListServiceAccountsEndpoint {

    private final ServiceAccountService serviceAccountService;

    @Inject
    public ListServiceAccountsEndpoint(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @GET
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listServiceAccounts", summary = "List project service accounts")
    public List<ServiceAccountResponse> list(@PathParam("projectId") long projectId,
                                             @Context SecurityContext securityContext) {
        return serviceAccountService.list(projectId, securityContext.getUserPrincipal().getName());
    }
}
