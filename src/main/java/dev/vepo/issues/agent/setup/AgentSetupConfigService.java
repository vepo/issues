package dev.vepo.issues.agent.setup;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class AgentSetupConfigService {

    private final String issuesPublicBaseUrl;
    private final String mcpPublicBaseUrl;

    @Inject
    public AgentSetupConfigService(
                                   @ConfigProperty(name = "issues.public-base-url") String issuesPublicBaseUrl,
                                   @ConfigProperty(name = "issues.mcp-public-base-url") String mcpPublicBaseUrl) {
        this.issuesPublicBaseUrl = trimTrailingSlash(issuesPublicBaseUrl);
        this.mcpPublicBaseUrl = trimTrailingSlash(mcpPublicBaseUrl);
    }

    public AgentSetupConfigResponse setupConfig(String preset) {
        if (preset == null || preset.isBlank()) {
            throw new BadRequestException("preset is required");
        }
        if (!"cursor".equalsIgnoreCase(preset)) {
            throw new BadRequestException("Unsupported preset: %s".formatted(preset));
        }
        var mcpUrl = "%s/mcp".formatted(mcpPublicBaseUrl);
        var issuesApiBaseUrl = "%s/api".formatted(issuesPublicBaseUrl);
        var snippet = """
                      {
                        "mcpServers": {
                          "issues": {
                            "url": "%s",
                            "headers": {
                              "Authorization": "Bearer <YOUR_API_TOKEN>"
                            }
                          }
                        },
                        "issuesApiBaseUrl": "%s"
                      }
                      """.formatted(mcpUrl, issuesApiBaseUrl).strip();
        return new AgentSetupConfigResponse("cursor",
                                            issuesPublicBaseUrl,
                                            mcpPublicBaseUrl,
                                            mcpUrl,
                                            issuesApiBaseUrl,
                                            snippet);
    }

    private static String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
