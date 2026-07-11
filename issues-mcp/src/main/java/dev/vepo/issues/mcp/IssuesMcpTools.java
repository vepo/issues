package dev.vepo.issues.mcp;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IssuesMcpTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssuesMcpTools.class);

    private final IssuesApiClient issuesApiClient;
    private final IssuesAuthSupport authSupport;

    @Inject
    public IssuesMcpTools(@RestClient IssuesApiClient issuesApiClient, IssuesAuthSupport authSupport) {
        this.issuesApiClient = issuesApiClient;
        this.authSupport = authSupport;
    }

    @Tool(name = "search_tickets", description = "Search Issues tickets by term and optional status id")
    public ToolResponse searchTickets(
                                      @ToolArg(description = "Search term (title/identifier text)") String term,
                                      @ToolArg(description = "Optional workflow status id; omit or -1 for any", required = false, defaultValue = "-1") Long statusId) {
        return invoke(() -> issuesApiClient.searchTickets(authSupport.requireAuthorization(),
                                                          term,
                                                          statusId == null ? -1L : statusId));
    }

    @Tool(name = "get_ticket_context", description = "Get composite ticket context (detail, transitions, in-scope custom fields)")
    public ToolResponse getTicketContext(
                                         @ToolArg(description = "Ticket numeric id") long ticketId) {
        return invoke(() -> issuesApiClient.getTicketContext(authSupport.requireAuthorization(), ticketId));
    }

    @Tool(name = "update_ticket", description = "Update a ticket. Body must be Issues UpdateTicketRequest JSON.")
    public ToolResponse updateTicket(
                                     @ToolArg(description = "Ticket numeric id") long ticketId,
                                     @ToolArg(description = "JSON body: title, description, categoryId, priority, optional ticketType/dueDate/planningFields/storyPoints/customFields") String bodyJson) {
        return invoke(() -> issuesApiClient.updateTicket(authSupport.requireAuthorization(), ticketId, bodyJson));
    }

    @Tool(name = "move_ticket", description = "Move a ticket to another workflow status")
    public ToolResponse moveTicket(
                                   @ToolArg(description = "Ticket numeric id") long ticketId,
                                   @ToolArg(description = "Target workflow status id") long toStatusId) {
        var body = "{\"to\":%d}".formatted(toStatusId);
        return invoke(() -> issuesApiClient.moveTicket(authSupport.requireAuthorization(), ticketId, body));
    }

    @Tool(name = "add_comment", description = "Add a comment to a ticket")
    public ToolResponse addComment(
                                   @ToolArg(description = "Ticket numeric id") long ticketId,
                                   @ToolArg(description = "Comment content (plain or rich text as accepted by Issues)") String content) {
        var escaped = content == null ? "" : content.replace("\\", "\\\\").replace("\"", "\\\"");
        var body = "{\"content\":\"%s\"}".formatted(escaped);
        return invoke(() -> issuesApiClient.addComment(authSupport.requireAuthorization(), ticketId, body));
    }

    @Tool(name = "list_projects", description = "List projects visible to the authenticated principal")
    public ToolResponse listProjects() {
        return invoke(() -> issuesApiClient.listProjects(authSupport.requireAuthorization()));
    }

    private ToolResponse invoke(ApiCall call) {
        try {
            return ToolResponse.success(call.execute());
        } catch (IssuesAuthSupport.MissingAuthorizationException e) {
            return IssuesAuthSupport.missingAuthResponse();
        } catch (ClientWebApplicationException e) {
            LOGGER.warn("Issues API call failed: HTTP {}", e.getResponse() == null ? "?" : e.getResponse().getStatus());
            var status = e.getResponse() == null ? 0 : e.getResponse().getStatus();
            var entity = safeEntity(e);
            return ToolResponse.error("Issues API error HTTP %d: %s".formatted(status, entity));
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error calling Issues API", e);
            return ToolResponse.error("Unexpected error: %s".formatted(e.getMessage()));
        }
    }

    private static String safeEntity(ClientWebApplicationException e) {
        try {
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                return e.getResponse().readEntity(String.class);
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return e.getMessage() == null ? "" : e.getMessage();
    }

    @FunctionalInterface
    private interface ApiCall {
        String execute();
    }
}
