package io.hops.services.rest;

import io.hops.integration.GroupFacade;
import io.hops.integration.UserDTO;
import io.hops.integration.UserFacade;
import io.hops.model.Groups;
import io.hops.model.Users;
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
 * @author André & Ermias
 */
@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class AuthService {

    @EJB
    private UserFacade userBean;

    @EJB
    private GroupFacade groupBean;

    @GET
    @Path("session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(@Context SecurityContext sc, @Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();
        req.getServletContext().log("SESSIONID: " + req.getSession().getId());
        try {
            //req.getServletContext().log("TEST: " + sc.getUserPrincipal().getName());
            json.setData(sc.getUserPrincipal().getName());
        } catch (Exception e) {
            json.setStatus("401");
            json.setErrorMsg("Authentication failed");
            return getNoCacheResponseBuilder(Response.Status.UNAUTHORIZED).entity(json).build();
        }
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("email") String email, @FormParam("password") String password,
            @Context HttpServletRequest req, @Context HttpHeaders httpHeaders) {

        req.getServletContext().log("email: " + email);
        //req.getServletContext().log("Password: " + password);
        req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());

        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        //only login if not already logged in...
        if (req.getUserPrincipal() == null) {
            if (user != null && user.getStatus() == Users.STATUS_REQUEST) {
                json.setStatus("FAILED");
                json.setErrorMsg("Your request has not yet been acknowlegded.");
                return Response.ok().entity(json).build();
            }
            try {
                req.login(email, password);
                req.getServletContext().log("Authentication: successfully logged in " + email);
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

        //we don't want to send the hashed password out in the json response
        userBean.detach(user);
        user.setPassword("");
        req.getServletContext().log("Authentication: successfully retrieved User Profile from DB for " + email);
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
        req.getServletContext().log("Logging out...");
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

        req.getServletContext().log("Registering This dude..." + newUser.getEmail() + ", " + newUser.getFirstName());

        JsonResponse json = new JsonResponse();
        json.setData(newUser); //just return the data we received

        //do some more validation 
        if (newUser.getChosenPassword().length() == 0) {
            json.setErrorMsg("Password can not be empty.");
            json.setStatus("FAILED");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!newUser.getChosenPassword().equals(newUser.getRepeatedPassword())) {

            json.setErrorMsg("Both passwords have to be the same - typo?");
            json.setStatus("FAILED");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }

        Users user = new Users(newUser);

        Groups group = groupBean.findByGroupName(Groups.USER);
        user.addGroup(group);
        //this could cause a runtime exception, i.e. in case the user already exists
        //such exceptions will be caught by our ExceptionMapper, i.e. javax.transaction.RollbackException
        userBean.persist(user); // this would use the clients transaction which is committed after save() has finished
        req.getServletContext().log("successfully registered new user: '" + newUser.getEmail() + "'");
        

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    //latter can be implemented with a mailing service to mail the user a new password
   //to their given email.
    @POST
    @Path("forgotPassword")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response forgotPassword(@FormParam("email") String email,
            @FormParam("securityQuestion") String securityQuestion,
            @FormParam("securityAnswer") String securityAnswer,
            @FormParam("newPassword") String newPassword,
            @FormParam("confirmedPassword") String confirmedPassword,
            @Context SecurityContext sc) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(email);

        if (user == null) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!user.getSecurityQuestion().equalsIgnoreCase(securityQuestion)
                || !user.getSecurityAnswer().equalsIgnoreCase(securityAnswer)) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. security question or answer do not match");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!newPassword.equals(confirmedPassword)) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. passwords do not match.");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }

        user.setPassword(newPassword);
        userBean.update(user);

        json.setStatus("OK");

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

}
