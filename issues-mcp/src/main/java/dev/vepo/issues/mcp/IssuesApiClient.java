package dev.vepo.issues.mcp;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "issues-api")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface IssuesApiClient {

    @GET
    @Path("/tickets/search")
    String searchTickets(@HeaderParam("Authorization") String authorization,
                         @QueryParam("term") String term,
                         @QueryParam("statusId") Long statusId);

    @GET
    @Path("/tickets/{id}/context")
    String getTicketContext(@HeaderParam("Authorization") String authorization,
                            @PathParam("id") long id);

    @POST
    @Path("/tickets/{id}")
    String updateTicket(@HeaderParam("Authorization") String authorization,
                        @PathParam("id") long id,
                        String body);

    @POST
    @Path("/tickets/{id}/move")
    String moveTicket(@HeaderParam("Authorization") String authorization,
                      @PathParam("id") long id,
                      String body);

    @POST
    @Path("/tickets/{id}/comments")
    String addComment(@HeaderParam("Authorization") String authorization,
                      @PathParam("id") long id,
                      String body);

    @GET
    @Path("/projects")
    String listProjects(@HeaderParam("Authorization") String authorization);
}
