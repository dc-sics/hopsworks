/*
 */
package io.hops.services.rest;

import io.hops.integration.UserFacade;
import io.hops.model.Users;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author André & Ermias
 */
@Path("/admin")
@RolesAllowed("ADMIN")
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class AdminServices {

    @EJB
    private UserFacade userBean;

    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping(@Context SecurityContext sc, @Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();

        req.getServletContext().log("Principal of callee: " + sc.getUserPrincipal().getName());
        req.getServletContext().log("SESSIONID @ping: " + req.getSession().getId());

        json.setData(sc.getUserPrincipal().getName());

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("activate")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response activateUser(@FormParam("email") String email) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        if (user != null) {
            user.setStatus(Users.STATUS_ALLOW);
            userBean.update(user);
        } else {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        json.setStatus("OK");
        //json.setData(user);

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    /**
     * Accept the requests of a list of users.
     * @param emails of users to be accepted.
     * @return json response  
     */
    @POST
    @Path("activate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response activateUser(List<String> emails) {
        JsonResponse json = new JsonResponse();
        if (emails == null || emails.isEmpty()) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. No users in list.");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        Users user;
        ArrayList<String> usersNotFound = new ArrayList<>();
        for (String email : emails) {
            user = userBean.findByEmail(email);
            if (user != null) {
                user.setStatus(Users.STATUS_ALLOW);
                userBean.update(user);
            } else {
                usersNotFound.add(email);
            }
        }
        if (usersNotFound.size() == emails.size()) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. Requests were not processed.");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!usersNotFound.isEmpty()) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation partially failed. Requests for " + StringUtils.join(usersNotFound.iterator(), ", ") + " were not processed.");
            //json.setData(usersNotFound);
        } else {
            json.setStatus("OK");
        }

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response deactivateUser(@FormParam("email") String email) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        if (user != null) {
            user.setStatus(Users.STATUS_REQUEST);
            userBean.update(user);
        } else {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        json.setStatus("OK");
        //json.setData(user);

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("remove")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response removeUser(@FormParam("email") String email) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        if (user == null) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }

        userBean.remove(user);
        json.setStatus("OK");

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        JsonResponse json = new JsonResponse();
        List<Users> users = userBean.findAll();
        if (users != null && !users.isEmpty()) {
            json.setData(users);
        } else {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(json).build();
        }
        json.setStatus("OK");
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response getUser(@FormParam("email") String email) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        if (user != null) {
            json.setStatus("OK");
            //we don't want to send the hashed password out in the json response
            userBean.detach(user);
            user.setPassword("");
            json.setData(user);
        } else {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(json).build();
        }

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
