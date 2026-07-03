package dev.vepo.issues.home.savedqueries;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.home.HomePaths;
import dev.vepo.issues.home.HomeService;
import dev.vepo.issues.ticket.search.saved.HomeSavedQuerySectionResponse;
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

@Path(HomePaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Home")
public class ListHomeSavedQuerySectionsEndpoint {

    private final HomeService homeService;

    @Inject
    public ListHomeSavedQuerySectionsEndpoint(HomeService homeService) {
        this.homeService = homeService;
    }

    @GET
    @Path("saved-queries")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "listHomeSavedQuerySections", summary = "List saved query sections for the home screen")
    public List<HomeSavedQuerySectionResponse> list(@Context SecurityContext securityContext) {
        return homeService.listSavedQuerySections(securityContext.getUserPrincipal().getName());
    }
}
