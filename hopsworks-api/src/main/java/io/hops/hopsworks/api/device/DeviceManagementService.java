package io.hops.hopsworks.api.device;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.device.DeviceFacade2;
import io.hops.hopsworks.common.dao.device.ProjectDeviceDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceManagementService {

  private final static Logger LOGGER = Logger.getLogger(DeviceManagementService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private ProjectController projectController;

  @EJB
  private DeviceFacade2 deviceFacade;

  private Integer projectId;

  private static final String DEFAULT_DEVICE_USER_EMAIL = "devices@hops.io";

  private static final String JWT_DURATION_IN_HOURS = "jwtTokenDurationInHours";

  private static final String STATE = "state";


  public DeviceManagementService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  private void checkForProjectId() throws AppException {
    if (projectId == null) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request! Project id not present!");
    }
  }

  /**
   * This endpoint activates the "devices" feature for the project associated with the project_id.
   * This endpoint adds the default device-user to the project as a Data Owner so that approved devices can produce to
   * the project's kafka topics using the device-user's certificates.
   * This endpoint creates the project secret that is necessary for signing jwt tokens and for authenticating them.
   */
  @POST
  @Path("/activate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response postActivateEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();
    try {
      JSONObject json = new JSONObject(jsonString);
      Integer projectTokenDurationInHours = json.getInt(JWT_DURATION_IN_HOURS);

      // Adds the device-user to the project as a Data Owner
      List<ProjectTeam> list = new ArrayList<>();
      ProjectTeam pt = new ProjectTeam(new ProjectTeamPK(projectId, DEFAULT_DEVICE_USER_EMAIL));
      pt.setTeamRole(AllowedRoles.DATA_OWNER);
      pt.setTimestamp(new Date());
      list.add(pt);

      Project project = projectController.findProjectById(projectId);
      List<String>  failed = projectController.addMembers(project, project.getOwner().getEmail(), list);
      if (failed != null && failed.size() > 0){
        LOGGER.severe("Failure for user: " + failed.get(0));
        throw new AppException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "Default Devices User could not be added to the project.");
      }

      // Generates a random UUID to serve as the project secret.
      String projectSecret = UUID.randomUUID().toString();

      // Saves Project Secret
      deviceFacade.addProjectSecret(projectId, projectSecret, projectTokenDurationInHours);
      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
        "Json request is malformed! Required properties are [{0}]", JWT_DURATION_IN_HOURS));
    }
  }

  @POST
  @Path("/deactivate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response postDeactivateEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      checkForProjectId();
      Project project = projectController.findProjectById(projectId);
      try {
        projectController.removeMemberFromTeam(project, project.getOwner().getEmail(), DEFAULT_DEVICE_USER_EMAIL);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // Saves Project Secret
      deviceFacade.removeProjectSecret(projectId);
      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
        "Json request is malformed! Required properties are [{0}]", JWT_DURATION_IN_HOURS));
    }
  }

  @GET
  @Path("/devices")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response getDevicesEndpoint(@Context HttpServletRequest req) throws AppException {
    checkForProjectId();

    String state = req.getParameter(STATE);
    List<ProjectDeviceDTO> listDevices;
    if (state != null){
      listDevices = deviceFacade.getProjectDevices(projectId, Integer.valueOf(state));
    }else{
      listDevices = deviceFacade.getProjectDevices(projectId);
    }
    GenericEntity<List<ProjectDeviceDTO>> projectDevices = new GenericEntity<List<ProjectDeviceDTO>>(listDevices){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectDevices).build();

  }

  @POST
  @Path("/devices")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response postDevicesEndpoint(
    @Context HttpServletRequest req, List<ProjectDeviceDTO> listDevices) throws AppException {
    checkForProjectId();
    deviceFacade.updateDevicesState(listDevices);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

}


