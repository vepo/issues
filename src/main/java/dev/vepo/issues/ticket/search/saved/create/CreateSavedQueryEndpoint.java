package dev.vepo.issues.ticket.search.saved.create;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.issues.ticket.search.saved.CreateSavedQueryRequest;
import dev.vepo.issues.ticket.search.saved.SavedQueryPaths;
import dev.vepo.issues.ticket.search.saved.SavedQueryResponse;
import dev.vepo.issues.ticket.search.saved.SavedQueryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(SavedQueryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "SavedQuery")
public class CreateSavedQueryEndpoint {

    private final SavedQueryService savedQueryService;

    @Inject
    public CreateSavedQueryEndpoint(SavedQueryService savedQueryService) {
        this.savedQueryService = savedQueryService;
    }

    @POST
    @ResponseStatus(201)
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "createSavedQuery", summary = "Create a saved query")
    public SavedQueryResponse create(@Valid CreateSavedQueryRequest request, @Context SecurityContext securityContext) {
        return savedQueryService.create(request, securityContext.getUserPrincipal().getName());
    }
}
