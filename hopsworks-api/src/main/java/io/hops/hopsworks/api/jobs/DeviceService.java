package io.hops.hopsworks.api.jobs;

import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.json.JSONException;
import org.json.JSONObject;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.exception.AppException;
import org.apache.avro.Schema;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceService {

  private final static Logger LOGGER = Logger.getLogger(
      DeviceService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private KafkaFacade kafkaFacade;

  private Integer projectId;

  @EJB
  private DeviceFacade deviceFacade;

  public DeviceService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;

  }

  public Integer getProjectId() {
    return projectId;
  }

  private void checkForProjectId() throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request! Project id not present!");
    }
  }

  @POST
  @Path("/activate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response activateService(
      @Context SecurityContext sc, @Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();

    return null;
  }

  @POST
  @Path("/deactivate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response deactivateService(
      @Context SecurityContext sc, @Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();

    return null;
  }

  @GET
  @Path("/endpoints")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response getEndpoints(
      @Context SecurityContext sc, @Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();

    return null;
  }

  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response registerDevice(@Context HttpServletRequest req, String jsonString) throws AppException {

    checkForProjectId();

    try {
      JSONObject json = new JSONObject(jsonString);
      String deviceUuid = json.getString("deviceUuid");
      String passUuid = json.getString("passUuid");
      String projectUserUuid = json.getString("projectUserUuid");
      deviceFacade.registerDevice(projectId, deviceUuid, passUuid, projectUserUuid);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! Required properties are [deviceUuid, passUuid, projectUserUuid].");
    }
  }

  @GET
  @Path("/login") // Returns --> JWT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response loginDevice(@Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();

    return null;
  }

  @POST
  @Path("/produce") //Returns --> Status.OK / Status.NOT_OK
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response produce(@Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();

    return null;
  }

  @GET
  @Path("/validate") //Returns --> Status.OK / Status.NOT_OK
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateSchema(@Context HttpServletRequest req, String jsonString) throws AppException {

    checkForProjectId();

    try {
      JSONObject json = new JSONObject(jsonString);
      String topicName = json.getString("topic");
      String schemaName = json.getString("schema");
      Integer schemaVersion = json.getInt("version");
      SchemaDTO schemaDtos = kafkaFacade.getSchemaContent(schemaName, schemaVersion);

      return null;
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! Required properties are [topic, schema, version].");
    }
  }

}


