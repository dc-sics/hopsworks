package io.hops.hopsworks.apiV2.users;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Path("/users")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Users", description = "Users Resource")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersResource {
  @EJB
  private UserFacade userBean;
  
  @ApiOperation("Get a list of users in the cluster")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response findAll(@Context SecurityContext sc) {

    List<Users> users = userBean.findAllUsers();
    List<UserView> userViews = new ArrayList<>();
    
    for (Users user : users) {
      UserView userView = new UserView(user);
      userViews.add(userView);
    }
    GenericEntity<List<UserView>> result = new GenericEntity<List<UserView>>(userViews) {};
    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
