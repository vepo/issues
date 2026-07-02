package dev.vepo.issues.ticket;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/projects/{projectId}/tickets")
public class ProjectTicketEndpoint {

    private final TicketRepository repository;

    @Inject
    public ProjectTicketEndpoint(TicketRepository repository) {
        this.repository = repository;
    }

    @GET
    public List<TicketResponse> findByProjectId(@PathParam("projectId") long projectId) {
        return repository.findByProjectId(projectId)
                         .map(TicketResponse::load)
                         .toList();
    }
}
