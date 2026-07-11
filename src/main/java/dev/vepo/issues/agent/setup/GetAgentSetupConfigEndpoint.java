package dev.vepo.issues.agent.setup;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.vepo.issues.user.Role;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/agent")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@DenyAll
@Tag(name = "Agent")
public class GetAgentSetupConfigEndpoint {

    private final AgentSetupConfigService agentSetupConfigService;

    @Inject
    public GetAgentSetupConfigEndpoint(AgentSetupConfigService agentSetupConfigService) {
        this.agentSetupConfigService = agentSetupConfigService;
    }

    @GET
    @Path("/setup-config")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    @Operation(operationId = "getAgentSetupConfig", summary = "Get agent setup configuration snippet")
    public AgentSetupConfigResponse getSetupConfig(@QueryParam("preset") String preset) {
        return agentSetupConfigService.setupConfig(preset);
    }
}
