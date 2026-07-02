package dev.vepo.issues.dashboards.table;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.dashboards.DashboardPaths;
import dev.vepo.issues.dashboards.DashboardService;
import dev.vepo.issues.dashboards.DashboardType;
import dev.vepo.issues.dashboards.TableDataResponse;
import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(DashboardPaths.BASE)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Dashboard")
public class LoadTableDashboardEndpoint {

    private final DashboardService dashboardService;

    @Inject
    public LoadTableDashboardEndpoint(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GET
    @Path("table/{dashboardType}")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "loadTableDashboard", summary = "Load table dashboard data")
    public TableDataResponse loadTableData(@PathParam("projectId") Long projectId,
                                           @PathParam("dashboardType") DashboardType type) {
        return dashboardService.loadTableData(projectId, type);
    }
}
