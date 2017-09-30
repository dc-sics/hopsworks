package io.hops.hopsworks.api.device;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.device.DeviceFacade2;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade2;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceManagementService {

  private final static Logger LOGGER = Logger.getLogger(
      DeviceManagementService.class.getName());
  
  /***
   * Determines the duration of time that a jwt token is valid for.
   * It is measured in number of hours.
   */
  private static final String JWT_DURATION = "jwtDuration"; //

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private UserManager userManager;

  @EJB
  private DeviceFacade2 deviceFacade2;
  
  @EJB
  private KafkaFacade2 kafkaFacade;
  
  private Integer projectId;

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
  
  @GET
  @Path("/endpoints")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getEndpoints(
      @Context SecurityContext sc, @Context HttpServletRequest req,
      String jsonString) throws AppException {
    checkForProjectId();

    try {
      JSONObject json = new JSONObject(jsonString);
      return noCacheResponse.getNoCacheResponseBuilder(Status.OK).build();
    }catch(JSONException e) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! Required properties " +
              "are [deviceUuid, passUuid, userId].");
    }
  }


  
}


