package dev.vepo.issues.agent.setup;

public record AgentSetupConfigResponse(String preset,
                                       String issuesPublicBaseUrl,
                                       String mcpPublicBaseUrl,
                                       String mcpUrl,
                                       String issuesApiBaseUrl,
                                       String snippet) {}
