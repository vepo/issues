package dev.vepo.issues.ticket.search.saved.list;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.search.saved.SavedQueryPaths;
import dev.vepo.issues.ticket.search.saved.SavedQueryResponse;
import dev.vepo.issues.ticket.search.saved.SavedQueryService;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path(SavedQueryPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "SavedQuery")
public class ListSavedQueriesEndpoint {

    private final SavedQueryService savedQueryService;

    @Inject
    public ListSavedQueriesEndpoint(SavedQueryService savedQueryService) {
        this.savedQueryService = savedQueryService;
    }

    @GET
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listSavedQueries", summary = "List saved queries for the current user")
    public List<SavedQueryResponse> list(@Context SecurityContext securityContext) {
        return savedQueryService.listForOwner(securityContext.getUserPrincipal().getName());
    }
}
