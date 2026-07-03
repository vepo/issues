package dev.vepo.issues.ticket.search.saved.update;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.search.saved.SavedQueryPaths;
import dev.vepo.issues.ticket.search.saved.SavedQueryResponse;
import dev.vepo.issues.ticket.search.saved.SavedQueryService;
import dev.vepo.issues.ticket.search.saved.UpdateSavedQueryRequest;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
public class UpdateSavedQueryEndpoint {

    private final SavedQueryService savedQueryService;

    @Inject
    public UpdateSavedQueryEndpoint(SavedQueryService savedQueryService) {
        this.savedQueryService = savedQueryService;
    }

    @PUT
    @Path("{id}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "updateSavedQuery", summary = "Update a saved query")
    public SavedQueryResponse update(@PathParam("id") long id,
                                     @Valid UpdateSavedQueryRequest request,
                                     @Context SecurityContext securityContext) {
        return savedQueryService.update(id, request, securityContext.getUserPrincipal().getName());
    }
}
