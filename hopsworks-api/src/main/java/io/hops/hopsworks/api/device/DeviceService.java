package io.hops.hopsworks.api.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.project.ProjectController;

import io.hops.hopsworks.common.user.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import org.apache.avro.generic.GenericData;
import org.apache.commons.net.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.device.ProjectDeviceDTO;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.device.ProjectDevice;
import io.hops.hopsworks.common.dao.device.ProjectSecret;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;

@Path("/device")
@Api(value = "Device Service",
    description = "Device Service")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceService {

  private final static Logger logger = Logger.getLogger(DeviceService.class.getName());

  private static final String DEVICE_UUID = "deviceUuid";
  private static final String PASS_UUID = "passUuid";
  private static final String PROJECT_ID = "projectId";
  private static final String ALIAS = "alias";
  private static final String STATE = "state";
  private static final String DEFAULT_DEVICE_USER_EMAIL = "devices@hops.io";
  
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOPIC = "topic";
  private static final String RECORDS = "records";

  private static final String JWT_DURATION_IN_HOURS = "jwtTokenDurationInHours";

  @EJB
  private CertsFacade userCerts;

  @EJB
  private CertificateMaterializer certificateMaterializer;

  @EJB
  private Settings settings;

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private UserManager userManager;

  @EJB
  private ProjectController projectController;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private DeviceFacade deviceFacade;
  
  @EJB
  private KafkaFacade kafkaFacade;

  public DeviceService() {
  }

  private String getJwtFromAuthorizationHeader(String authorizationHeader){
    if (authorizationHeader == null) {
      return null;
    }

    if(!authorizationHeader.startsWith("Bearer")){
      return null;
    }

    return authorizationHeader.substring("Bearer".length()).replaceAll("\\s","");
  }

  private ProjectSecret getProjectSecret(Integer projectId) throws DeviceServiceException{
    try {
      return deviceFacade.getProjectSecret(projectId);
    }catch (Exception e) {
      throw new DeviceServiceException(DeviceResponseBuilder.DEVICES_FEATURE_NOT_ACTIVE);
    }
  }

  //===============================================================================================================
  // DEVICE MANAGEMENT ENDPOINTS
  //===============================================================================================================

  /**
   * This endpoint activates the "devices" feature for the project associated with the project_id.
   * This endpoint adds the default device-user to the project as a Data Owner so that approved devices can produce to
   * the project's kafka topics.
   * This endpoint creates the project secret that is necessary for signing jwt tokens and for authenticating them.
   */
  @POST
  @Path("/activate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response postActivateEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      JSONObject json = new JSONObject(jsonString);
      Integer projectId = json.getInt(PROJECT_ID);
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
        logger.warning("Failure for user: " + failed.get(0));
      }else{
        logger.warning("No failure detected");
      }

      // Generates a random UUID to serve as the project secret.
      String projectSecret = UUID.randomUUID().toString();

      // Saves Project Secret
      deviceFacade.addProjectSecret(projectId, projectSecret, projectTokenDurationInHours);
      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}]",
              PROJECT_ID, JWT_DURATION_IN_HOURS));
    }
  }

  //TODO: Add deactivation endpoint that deletes the project secret.

  @GET
  @Path("/devices")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDevicesEndpoint(@Context HttpServletRequest req) throws AppException {

    Integer projectId = Integer.valueOf(req.getParameter(PROJECT_ID));
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
  public Response postDevicesStateEndpoint(
    @Context HttpServletRequest req, List<ProjectDeviceDTO> listDevices) throws AppException {
    deviceFacade.updateDevicesState(listDevices);
    return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
  }

  @GET
  @Path("/instructions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getInstructionsEndpoint(@Context HttpServletRequest req) throws AppException {
    //TODO: Change instructions text and provide the correct endpoints and parameters.
    String instructions = "Instructions for how to connect the devices to hopsworks will go here";
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(instructions).build();

  }

  //===============================================================================================================
  // DEVICE ENDPOINTS
  //===============================================================================================================

  /**
   * Endpoint to verify the jwt token provided in the Authorization Header.
   */
  @POST
  @Path("/verify-token")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postVerifyTokenEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      JSONObject json = new JSONObject(jsonString);
      Integer projectId = json.getInt(PROJECT_ID);

      ProjectSecret secret = getProjectSecret(projectId);

      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      Response verification = DeviceServiceSecurity.verifyJwt(secret, jwtToken);
      if (verification != null){
        return verification;
      }
      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "GET Request is malformed! Required params are [{0}]", PROJECT_ID));
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Endpoint for registering a new device into a project.
   */
  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRegisterEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject json = new JSONObject(jsonString);
      String deviceUuid = json.getString(DEVICE_UUID);
      String passUuid = json.getString(PASS_UUID);
      Integer projectId = json.getInt(PROJECT_ID);
      String alias = json.getString(ALIAS);

      ProjectSecret secret = getProjectSecret(projectId);

      try {
        deviceFacade.addProjectDevice(projectId, deviceUuid, passUuid, alias);
        return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
      }catch (Exception e) {
        return DeviceResponseBuilder.failedJsonResponse(
          Status.UNAUTHORIZED, "Device is already registered for this project.");
      }
    }catch(JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}, {2}]",
              PROJECT_ID, DEVICE_UUID, PASS_UUID));
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }
  

  /**
   * Login end-point for project devices
   */
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postLoginEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject jsonRequest = new JSONObject(jsonString);
      String deviceUuid = jsonRequest.getString(DEVICE_UUID);
      String passUuid = jsonRequest.getString(PASS_UUID);
      Integer projectId = jsonRequest.getInt(PROJECT_ID);

      ProjectSecret secret = getProjectSecret(projectId);

      ProjectDevice device;
      try {
        device = deviceFacade.getProjectDevice(projectId, deviceUuid);
      }catch (Exception e) {
        return DeviceResponseBuilder.failedJsonResponse(Status.UNAUTHORIZED, MessageFormat.format(
                "No device is registered with the given {0}.", DEVICE_UUID));
      }

      if (device.getPassUuid().equals(passUuid)) {
        return DeviceResponseBuilder.successfulJsonResponse(
          Status.OK, DeviceServiceSecurity.generateJwt(secret, device));
      }else {
        return DeviceResponseBuilder.failedJsonResponse(
          Status.UNAUTHORIZED, MessageFormat.format("{0} is incorrect.", PASS_UUID));
      }
    }catch(JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}, {2}]",
              PROJECT_ID, DEVICE_UUID, PASS_UUID));
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Get the schema of a topic before producing to that topic
   */
  @GET
  @Path("/topic-schema")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTopicSchemaEndpoint(@Context HttpServletRequest req) throws AppException {

    try {
      Integer projectId = Integer.valueOf(req.getParameter(PROJECT_ID));
      String topicName = req.getParameter(TOPIC);

      ProjectSecret secret = getProjectSecret(projectId);

      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      Response authFailedResponse = DeviceServiceSecurity.verifyJwt(secret, jwtToken);
      if (authFailedResponse != null) {
        return authFailedResponse;
      }
      // Device is authenticated at this point

      SchemaDTO schemaDTO = kafkaFacade.getSchemaForProjectTopic(projectId, topicName);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemaDTO).build();

    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), MessageFormat.format(
        "Json request is malformed! Required properties are [{0}, {1}].", PROJECT_ID, TOPIC));
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  @POST
  @Path("/produce")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response postProduceEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      // Extracts all the json parameters
      JSONObject json = new JSONObject(jsonString);
      logger.info(json.toString()); //TODO: Remove after debugging
      Integer projectId = json.getInt(PROJECT_ID);
      String topicName = json.getString(TOPIC);
      JSONArray records = json.getJSONArray(RECORDS);

      // Retrieves the project secret
      ProjectSecret secret = getProjectSecret(projectId);

      // Verifies jwtToken
      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      Response authFailedResponse = DeviceServiceSecurity.verifyJwt(secret, jwtToken);
      if (authFailedResponse != null) {
        return authFailedResponse;
      }

      // The device is authenticated at this point.

      // Extracts deviceUuid from jwtToken
      DecodedJWT decodedJwt = DeviceServiceSecurity.getDecodedJwt(secret, jwtToken);
      String deviceUuid = decodedJwt.getClaim(DEVICE_UUID).asString();

      // Extracts the default device-user from the database
      Users user = userManager.getUserByEmail(DEFAULT_DEVICE_USER_EMAIL);
      Project project = projectFacade.find(projectId);

      HopsUtils.copyUserKafkaCerts(userCerts, project,  user.getUsername(),
        settings.getHopsworksTmpCertDir(), settings.getHdfsTmpCertDir(), certificateMaterializer);

      String keyStoreFilePath = settings.getHopsworksTmpCertDir() + File.separator +
        HopsUtils.getProjectKeystoreName(project.getName(), user.getUsername());

      String base64EncodedKeyStore = keystoreEncode(keyStoreFilePath);

      CertPwDTO certPwDTO;
      try {
        certPwDTO = projectController.getProjectSpecificCertPw(
          user, project.getName(), base64EncodedKeyStore);
      } catch (Exception e) {
        return DeviceResponseBuilder.PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND;
      }

      // Extracts the Avro Schema contents from the database
      SchemaDTO schema = kafkaFacade.getSchemaForProjectTopic(projectId, topicName);
      try {
        List<GenericData.Record> avroRecords = JsonToAvroConverter.toAvro(schema.getContents(), records);
        boolean success = kafkaFacade.produce(
          false, project, user, certPwDTO, deviceUuid, topicName, schema.getContents(), avroRecords);
        if (success){
          return DeviceResponseBuilder.successfulJsonResponse(Status.OK, MessageFormat.format(
            "projectId:{0}, deviceUuid:{1}, userEmail:{2}, topicName:{3}",
            projectId, deviceUuid, user.getEmail(), topicName));
        }else{
          return DeviceResponseBuilder.failedJsonResponse(Status.INTERNAL_SERVER_ERROR,
            "Produce was not successful");
        }


      } catch (Exception e) {
        return DeviceResponseBuilder.failedJsonResponse(
            Status.INTERNAL_SERVER_ERROR,"Something went wrong while producing to Kafka.");
      }
    }catch(JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}, {2}]", PROJECT_ID, TOPIC, RECORDS));
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }

  }

  private String keystoreEncode(String keystoreFilePath) throws DeviceServiceException {
    try {
      FileInputStream kfin = new FileInputStream(new File(keystoreFilePath));
      byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
      return Base64.encodeBase64String(kStoreBlob);
    } catch (FileNotFoundException e) {
      throw new DeviceServiceException(DeviceResponseBuilder.failedJsonResponse(
        Status.INTERNAL_SERVER_ERROR, "File not found"));
    } catch (IOException e) {
      throw new DeviceServiceException(DeviceResponseBuilder.failedJsonResponse(
        Status.INTERNAL_SERVER_ERROR, "File to Byte IO exception"));
    }
  }

}


