package io.hops.services.rest;

import io.hops.integration.GroupFacade;
import io.hops.integration.UserDTO;
import io.hops.integration.UserFacade;
import io.hops.model.Groups;
import io.hops.model.Users;
import io.hops.model.UsersInterface;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class SecurityService {

    @EJB
    private UserFacade userBean;
    
    @EJB
    private GroupFacade groupBean;

    @GET
    @Path("ping")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping(@Context SecurityContext sc, @Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();

        req.getServletContext().log("Principal of callee: " + sc.getUserPrincipal().getName());
        req.getServletContext().log("SESSIONID@ping: " + req.getSession().getId());

        json.setData(sc.getUserPrincipal().getName());

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @GET
    @Path("session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(@Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();
        req.getServletContext().log("SESSIONID@ping: " + req.getSession().getId());
        json.setData(req.getSession().getId());
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("email") String email, @FormParam("password") String password,
            @Context HttpServletRequest req, @Context HttpHeaders httpHeaders) {

        req.getServletContext().log("Username: " + email);
        //req.getServletContext().log("Password: " + password);
        req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());

        JsonResponse json = new JsonResponse();
        UsersInterface user = (UsersInterface) userBean.findByEmail(email);

        //only login if not already logged in...
        if (req.getUserPrincipal() == null) {
            if (user != null && user.getStatus() == UsersInterface.STATUS_REQUEST) {
                json.setStatus("FAILED");
                json.setErrorMsg("Your request has not yet been acknowlegded.");
                return Response.ok().entity(json).build();
            }
            try {
                req.login(email, password);
                req.getServletContext().log("Authentication Demo: successfully logged in " + email);
            } catch (ServletException e) {
                json.setStatus("FAILED");
                json.setErrorMsg("Authentication failed");
                return Response.ok().entity(json).build();
            }
        } else {
            req.getServletContext().log("Skip logged because already logged in: " + email);
        }

        //read the user data from db and return to caller
        json.setStatus("SUCCESS");

        req.getServletContext().log("Authentication Demo: successfully retrieved User Profile from DB for " + email);
        json.setData(user);
        json.setSessionID(req.getSession().getId());

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }

    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();

        try {
            req.logout();
            json.setStatus("SUCCESS");
            req.getSession().invalidate();
        } catch (ServletException e) {
            json.setStatus("FAILED");
            json.setErrorMsg("Logout failed on backend");
        }
        return Response.ok().entity(json).build();
    }

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response register(UserDTO newUser, @Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();
        json.setData(newUser); //just return the date we received

        //do some validation (in reality you would do some more validation...)
        //by the way: i did not choose to use bean validation (JSR 303)
        if (newUser.getPassword1().length() == 0 || !newUser.getPassword1().equals(newUser.getPassword2())) {
            json.setErrorMsg("Both passwords have to be the same - typo?");
            json.setStatus("FAILED");
            return Response.ok().entity(json).build();
        }

        Users user = new Users(newUser);
        Groups group = groupBean.findByGroupName(Groups.USER);
        user.addGroup(group);

        //this could cause a runtime exception, i.e. in case the user already exists
        //such exceptions will be caught by our ExceptionMapper, i.e. javax.transaction.RollbackException
        userBean.persist(user); // this would use the clients transaction which is committed after save() has finished
        req.getServletContext().log("successfully registered new user: '" + newUser.getEmail() + "':'" + newUser.getPassword1() + "'");

        return Response.ok().entity(json).build();
    }

}
