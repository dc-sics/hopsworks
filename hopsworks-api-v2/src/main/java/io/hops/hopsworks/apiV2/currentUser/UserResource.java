package io.hops.hopsworks.apiV2.currentUser;

import io.hops.hopsworks.apiV2.Util;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.UserProjectDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.user.UsersController;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.apiV2.Util.except;

@Path("/user")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "User", description = "Current User Resources")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserResource {

  private final static Logger logger = Logger.getLogger(
      UserResource.class.
          getName());

  @EJB
  private UserFacade userBean;
  @EJB
  private UserManager userManager;
  @EJB
  private UsersController userController;
  @EJB
  private ProjectController projectController;
  @EJB
  private ActivityFacade activityFacade;
  @Inject
  private MessagesResource messages;
  

  @GET
  @Path("/profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProfile(@Context SecurityContext sc) throws AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

    if (user == null) {
      except(Response.Status.NOT_FOUND, ResponseMessages.USER_WAS_NOT_FOUND);
    }

    UserDTO userDTO = new UserDTO(user);

    return Util.ok(userDTO);
  }

  @POST
  @Path("/profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateProfile(@FormParam("firstName") String firstName,
          @FormParam("lastName") String lastName,
          @FormParam("telephoneNum") String telephoneNum,
          @FormParam("toursState") Integer toursState,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    UserDTO userDTO = userController.updateProfile(sc.getUserPrincipal().
            getName(), firstName, lastName, telephoneNum, toursState, req);


    return Response.ok(userDTO, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @POST
  @Path("credentials")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeLoginCredentials(
          @FormParam("oldPassword") String oldPassword,
          @FormParam("newPassword") String newPassword,
          @FormParam("confirmedPassword") String confirmedPassword,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    userController.changePassword(sc.getUserPrincipal().getName(), oldPassword,
            newPassword, confirmedPassword, req);
    
    return Response.noContent().build();
  }

  @POST
  @Path("securityQA")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeSecurityQA(@FormParam("oldPassword") String oldPassword,
          @FormParam("securityQuestion") String securityQuestion,
          @FormParam("securityAnswer") String securityAnswer,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    userController.changeSecQA(sc.getUserPrincipal().getName(), oldPassword,
            securityQuestion, securityAnswer, req);

    return Response.noContent().build();
  }
  
  
  @GET
  @Path("/projects/{id}/role")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRole(@PathParam("id") int projectId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String email = sc.getUserPrincipal().getName();

    UserProjectDTO userDTO = new UserProjectDTO();
    userDTO.setEmail(email);
    userDTO.setProject(projectId);

    List<ProjectTeam> list = projectController.findProjectTeamById(projectId);

    for (ProjectTeam pt : list) {
      logger.log(Level.INFO, "{0} ({1}) -  {2}", new Object[]{pt.
        getProjectTeamPK().getTeamMember(),
        pt.getProjectTeamPK().getProjectId(), pt.getTeamRole()});
      if (pt.getProjectTeamPK().getTeamMember().compareToIgnoreCase(email) == 0) {
        userDTO.setRole(pt.getTeamRole());
      }
    }

    return Util.ok(userDTO);
  }
  
  @Path("/messages")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public MessagesResource getMessages(){
    return messages;
  }
  
  @Path("/activity")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserActivity(@Context SecurityContext sc, @Context HttpServletRequest req){
    Users user = userManager.getUserByEmail(sc.getUserPrincipal().getName());
    List<Activity> activityDetails = activityFacade.getAllActivityByUser(user);
    GenericEntity<List<Activity>> projectActivities
        = new GenericEntity<List<Activity>>(activityDetails) {};
    return Util.ok(projectActivities);
  }
}
