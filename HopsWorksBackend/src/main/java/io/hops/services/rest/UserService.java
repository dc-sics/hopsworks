/*
 */
package io.hops.services.rest;

import io.hops.integration.AppException;
import io.hops.integration.UserFacade;
import io.hops.model.Users;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author André & Ermias
 */
@Path("/user")
@RolesAllowed({"ADMIN", "USER"})
@Stateless
public class UserService {

    @EJB
    private UserFacade userBean;

    @GET
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response getUserProfile(@Context SecurityContext sc) throws AppException {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), 
                                "Operation failed. User not found");
        }

        userBean.detach(user);
        user.setPassword("");

        return getNoCacheResponseBuilder(Response.Status.OK).entity(user).build();
    }


    @POST
    @Path("updateProfile")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response updateProfile(@FormParam("firstName") String firstName,
                                  @FormParam("lastName") String lastName, 
                                  @FormParam("telephoneNum") String telephoneNum, 
                                  @Context SecurityContext sc, 
                                  @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), 
                                    "Operation failed. User not found");
        }

        req.getServletContext().log("Updating..." + firstName + ", " + lastName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTelephoneNum(telephoneNum);
        userBean.update(user);
        json.setStatus("OK");
        //we don't want to send the hashed password out in the json response
        userBean.detach(user);
        user.setPassword("");
        //json.setData(user);

        return getNoCacheResponseBuilder(Response.Status.OK).entity(user).build();
    }

    @POST
    @Path("changeLoginCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response changeLoginCredentials(@FormParam("oldPassword") String oldPassword,
                                           @FormParam("newPassword") String newPassword,
                                           @FormParam("confirmedPassword") String confirmedPassword,
                                           @Context SecurityContext sc,
                                           @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), 
                    "Operation failed. User not found");
        }
        if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), 
                    "Operation failed. password not correct");
        }
        if (newPassword.length() == 0) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), 
                    "Operation failed. password can not be empty.");
        }
        if (!newPassword.equals(confirmedPassword)) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), 
                    "Operation failed. passwords do not match.");
        }

        user.setPassword(newPassword);
        userBean.update(user);

        json.setStatus("OK");

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }


    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
}
