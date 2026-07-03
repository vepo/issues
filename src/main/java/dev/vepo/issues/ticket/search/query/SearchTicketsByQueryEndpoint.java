package dev.vepo.issues.ticket.search.query;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.ticket.TicketPaths;
import dev.vepo.issues.ticket.TicketResponse;
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

@Path(TicketPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Ticket")
public class SearchTicketsByQueryEndpoint {

    private final TicketQueryLanguageService queryLanguageService;

    @Inject
    public SearchTicketsByQueryEndpoint(TicketQueryLanguageService queryLanguageService) {
        this.queryLanguageService = queryLanguageService;
    }

    @POST
    @Path("search/query")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "searchTicketsByQuery", summary = "Search tickets using the query language")
    public List<TicketResponse> search(@Valid SearchTicketsByQueryRequest request, @Context SecurityContext securityContext) {
        return queryLanguageService.execute(request.query(), securityContext.getUserPrincipal().getName())
                                   .stream()
                                   .map(TicketResponse::load)
                                   .toList();
    }
}
