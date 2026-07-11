package dev.vepo.issues.project.serviceaccount.createtoken;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.project.serviceaccount.CreateServiceAccountTokenRequest;
import dev.vepo.issues.project.serviceaccount.CreatedServiceAccountTokenResponse;
import dev.vepo.issues.project.serviceaccount.ServiceAccountPaths;
import dev.vepo.issues.project.serviceaccount.ServiceAccountService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(ServiceAccountPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "ServiceAccount")
public class CreateServiceAccountTokenEndpoint {

    private final ServiceAccountService serviceAccountService;

    @Inject
    public CreateServiceAccountTokenEndpoint(ServiceAccountService serviceAccountService) {
        this.serviceAccountService = serviceAccountService;
    }

    @POST
    @Path("/{serviceAccountId}/tokens")
    @ResponseStatus(201)
    @RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createServiceAccountToken", summary = "Create a service account token")
    public CreatedServiceAccountTokenResponse create(@PathParam("projectId") long projectId,
                                                     @PathParam("serviceAccountId") long serviceAccountId,
                                                     @Valid CreateServiceAccountTokenRequest request,
                                                     @Context SecurityContext securityContext) {
        return serviceAccountService.createToken(projectId,
                                                 serviceAccountId,
                                                 request,
                                                 securityContext.getUserPrincipal().getName());
    }
}
