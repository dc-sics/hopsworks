package io.hops.hopsworks.ldap.rest;

import io.swagger.annotations.Api;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "LDAP-user", description = "LDAP user service")
public class UserService {
  private final static Logger LOGGER = Logger.getLogger(UserService.class.getName());
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUser() {
    LOGGER.log(Level.INFO, "Userinfo");
    return Response.ok("Userinfo").build();
  }
}
