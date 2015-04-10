/*
 */
package io.hops.integration;

import io.hops.services.rest.JsonResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.RollbackException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *TransactionExceptionMapper maps all transaction related exceptions and sends the
 * cause of the exception to the client.
 * 
 * @author André & Ermias
 */
@Provider
public class TransactionExceptionMapper implements ExceptionMapper<RollbackException>{

    private final static Logger log = Logger.getLogger(TransactionExceptionMapper.class.getName());
    @Override
    public Response toResponse(RollbackException ex) {
        log.log(Level.INFO, "TransactionExceptionMapper: {0}", ex.getClass());
        JsonResponse json = new JsonResponse();
        json.setStatusCode(Response.Status.CONFLICT.getStatusCode());
        String cause = ex.getCause().getCause().getCause().getMessage();
        json.setErrorMsg(ex.getMessage() + " Caused by: " + cause);
        return Response.status(Response.Status.CONFLICT)
                .entity(json)
                .type(MediaType.APPLICATION_JSON).
                build();
    }

    
}
