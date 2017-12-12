package io.hops.hopsworks.ldap.rest;

import io.swagger.annotations.Api;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth")
@Api(value = "LDAP-Auth",
    description = "LDAP authentication service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email, @FormParam("password") String password,
      @Context HttpServletRequest req) {
    if (email == null || email.isEmpty()) {
      throw new IllegalArgumentException("Email can not be empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Password can not be empty.");
    }
    if (req.getRemoteUser() != null && !req.getRemoteUser().equals(email)) {
      logout(req);
    }
    try {
      req.login(email, password);
    } catch (ServletException e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    return Response.ok().build();
  }
  
  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) {
    try {
      req.getSession().invalidate();
      req.logout();
    } catch (ServletException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    
    return Response.ok().build();
  }
}
