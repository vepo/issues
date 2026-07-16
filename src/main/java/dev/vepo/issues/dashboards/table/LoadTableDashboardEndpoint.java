package dev.vepo.issues.dashboards.table;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.dashboards.DashboardPaths;
import dev.vepo.issues.dashboards.DashboardService;
import dev.vepo.issues.dashboards.DashboardType;
import dev.vepo.issues.dashboards.TableDataResponse;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
public class LoadTableDashboardEndpoint {

    private final DashboardService dashboardService;

    @Inject
    public LoadTableDashboardEndpoint(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GET
    @Path("table/{dashboardType}")
    @PermitAll
    @Operation(operationId = "loadTableDashboard", summary = "Load table dashboard data")
    public TableDataResponse loadTableData(@PathParam("projectId") Long projectId,
                                           @PathParam("dashboardType") DashboardType type,
                                           @Context SecurityContext securityContext) {
        return dashboardService.loadTableData(projectId, type, optionalUsername(securityContext).orElse(null));
    }

    private static java.util.Optional<String> optionalUsername(SecurityContext securityContext) {
        var principal = securityContext.getUserPrincipal();
        return principal == null ? java.util.Optional.empty() : java.util.Optional.of(principal.getName());
    }

}
