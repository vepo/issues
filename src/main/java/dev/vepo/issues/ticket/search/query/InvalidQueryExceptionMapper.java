package dev.vepo.issues.ticket.search.query;

import dev.vepo.issues.infra.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1)
public class InvalidQueryExceptionMapper implements ExceptionMapper<InvalidQueryException> {

    @Override
    public Response toResponse(InvalidQueryException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), exception.getMessage()))
                       .build();
    }
}
