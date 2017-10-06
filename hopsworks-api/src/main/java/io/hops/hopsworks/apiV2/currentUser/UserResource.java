package io.hops.hopsworks.apiV2.currentUser;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.JsonResponse;
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
import io.hops.hopsworks.common.dao.user.sshkey.SshKeyDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.user.UsersController;
import io.swagger.annotations.Api;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

@Path("/v2/user")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "V2 User", description = "Current User Resources")
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

    return Util.jsonOk(userDTO);
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
    JsonResponse json = new JsonResponse();

    UserDTO userDTO = userController.updateProfile(sc.getUserPrincipal().
            getName(), firstName, lastName, telephoneNum, toursState, req);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PROFILE_UPDATED);
    json.setData(userDTO);

    return Util.jsonOk(userDTO);
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
    JsonResponse json = new JsonResponse();

    userController.changePassword(sc.getUserPrincipal().getName(), oldPassword,
            newPassword, confirmedPassword, req);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.PASSWORD_CHANGED);

    return Util.jsonOk(json);
  }

  @POST
  @Path("securityQA")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeSecurityQA(@FormParam("oldPassword") String oldPassword,
          @FormParam("securityQuestion") String securityQuestion,
          @FormParam("securityAnswer") String securityAnswer,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    userController.changeSecQA(sc.getUserPrincipal().getName(), oldPassword,
            securityQuestion, securityAnswer, req);

    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.SEC_QA_CHANGED);

    return Util.jsonOk(json);
  }

  @POST
  @Path("twoFactor")
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeTwoFactor(@FormParam("password") String password,
          @FormParam("twoFactor") boolean twoFactor,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

    byte[] qrCode;
    JsonResponse json = new JsonResponse();
    if (user.getTwoFactor() == twoFactor) {
      json.setSuccessMessage("No change made.");
      json.setStatus("OK");
      return Util.jsonOk(json);
    }

    qrCode = userController.changeTwoFactor(user, password, req);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      json.setSuccessMessage("Tow factor authentication disabled.");
    }
    json.setStatus("OK");
    return Util.jsonOk(json);
  }

  @POST
  @Path("QRCode")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getQRCode(@FormParam("password") String password,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (password == null || password.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Password requierd.");
    }
    byte[] qrCode;
    JsonResponse json = new JsonResponse();
    qrCode = userController.getQRCode(user, password);
    if (qrCode != null) {
      json.setQRCode(new String(Base64.encodeBase64(qrCode)));
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Two factor disabled.");
    }
    json.setStatus("OK");
    return Util.jsonOk(json);
  }

  @POST
  @Path("/sshKeys")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response addSshkey(SshKeyDTO sshkey,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
    int id = user.getUid();
    SshKeyDTO dto = userController.addSshKey(id, sshkey.getName(), sshkey.
            getPublicKey());
    return Util.jsonOk(dto);
  }

  @DELETE
  @Path("/sshKeys/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removeSshkey(@PathParam("name") String name,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
    int id = user.getUid();
    userController.removeSshKey(id, name);
    json.setStatus("OK");
    json.setSuccessMessage(ResponseMessages.SSH_KEY_REMOVED);
    return Util.jsonOk(json);
  }

  @GET
  @Path("/sshKeys")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getSshkeys(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
    int id = user.getUid();
    List<SshKeyDTO> sshKeys = userController.getSshKeys(id);

    GenericEntity<List<SshKeyDTO>> sshKeyViews
            = new GenericEntity<List<SshKeyDTO>>(sshKeys) {};
    return Util.jsonOk(sshKeyViews);

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

    return Util.jsonOk(userDTO);
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
    return Util.jsonOk(projectActivities);
  }
}
