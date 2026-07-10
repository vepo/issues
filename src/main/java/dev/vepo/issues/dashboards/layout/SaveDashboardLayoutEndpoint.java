package dev.vepo.issues.dashboards.layout;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.dashboards.DashboardLayoutResponse;
import dev.vepo.issues.dashboards.DashboardPaths;
import dev.vepo.issues.dashboards.DashboardService;
import dev.vepo.issues.dashboards.SaveDashboardLayoutRequest;
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

@Path(DashboardPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Dashboard")
public class SaveDashboardLayoutEndpoint {

    private final DashboardService dashboardService;

    @Inject
    public SaveDashboardLayoutEndpoint(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PUT
    @Path("layout")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "saveDashboardLayout", summary = "Save dashboard widget layout for the current user")
    public DashboardLayoutResponse saveLayout(@PathParam("projectId") long projectId,
                                              @Valid SaveDashboardLayoutRequest request,
                                              @Context SecurityContext securityContext) {
        return dashboardService.saveLayout(projectId, securityContext.getUserPrincipal().getName(), request);
    }
}
