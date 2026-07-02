package dev.vepo.issues.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(100)
public class IssuesServerExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger logger = LoggerFactory.getLogger(IssuesServerExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.error("An unexpected error occurred: {}", exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error"))
                       .build();
    }

}
