package dev.vepo.issues.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(0)
public class IssuesWebExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger logger = LoggerFactory.getLogger(IssuesWebExceptionMapper.class);

    @Override
    public Response toResponse(WebApplicationException exception) {
        logger.error("An error occurred!", exception);
        return Response.status(exception.getResponse().getStatus())
                       .type(MediaType.APPLICATION_JSON)
                       .entity(new ErrorResponse(exception.getResponse().getStatus(), exception.getMessage()))
                       .build();
    }

}
