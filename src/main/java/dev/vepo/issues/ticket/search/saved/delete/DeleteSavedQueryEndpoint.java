package dev.vepo.issues.ticket.search.saved.delete;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.search.saved.SavedQueryPaths;
import dev.vepo.issues.ticket.search.saved.SavedQueryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path(SavedQueryPaths.BASE)
@ApplicationScoped
@DenyAll
@Tag(name = "SavedQuery")
public class DeleteSavedQueryEndpoint {

    private final SavedQueryService savedQueryService;

    @Inject
    public DeleteSavedQueryEndpoint(SavedQueryService savedQueryService) {
        this.savedQueryService = savedQueryService;
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "deleteSavedQuery", summary = "Delete a saved query")
    public Response delete(@PathParam("id") long id, @Context SecurityContext securityContext) {
        savedQueryService.delete(id, securityContext.getUserPrincipal().getName());
        return Response.noContent()
                       .build();
    }
}
