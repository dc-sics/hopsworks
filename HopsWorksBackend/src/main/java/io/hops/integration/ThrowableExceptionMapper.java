package io.hops.integration;

import io.hops.services.rest.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessLocalException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author André & Ermias
 */
@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

    private final static Logger log = Logger.getLogger(ThrowableExceptionMapper.class.getName());

    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(Throwable ex) {
        log.log(Level.INFO, "ThrowableExceptionMapper: {0}", ex.getClass());
        JsonResponse json = new JsonResponse();
        setHttpStatus(ex, json);
        json.setErrorMsg(ex.getMessage());//should be comented out in production environment
        return Response.status(json.getStatusCode())
                .entity(json)
                .build();
    }

    private void setHttpStatus(Throwable ex, JsonResponse json) {
        if (ex instanceof WebApplicationException) {
            json.setStatusCode(((WebApplicationException) ex).getResponse().getStatus());
        } else {
            json.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); //defaults to internal server error 500
        }
    }

}
