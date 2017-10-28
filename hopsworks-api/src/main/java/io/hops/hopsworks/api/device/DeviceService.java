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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.device.ProjectDevice2;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettings;
import io.hops.hopsworks.common.dao.device.AuthProjectDeviceDTO;
import io.hops.hopsworks.common.dao.device.DeviceFacade4;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.project.ProjectController;

import io.hops.hopsworks.common.user.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import org.apache.avro.generic.GenericData;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade2;
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
  private static final String PROJECT_ID = "projectId";

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
  private DeviceFacade4 deviceFacade;
  
  @EJB
  private KafkaFacade2 kafkaFacade;

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

  private ProjectDevicesSettings getProjectDevicesSettings(Integer projectId) throws DeviceServiceException{
    try {
      return deviceFacade.readProjectDevicesSettings(projectId);
    }catch (Exception e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().DEVICES_FEATURE_NOT_ACTIVE);
    }
  }

  private ProjectDevice2 getProjectDevice(Integer projectId, String deviceUuid) throws DeviceServiceException{
    ProjectDevice2 device;
    try {
      device = deviceFacade.readProjectDevice(projectId, deviceUuid);
    }catch (Exception e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().DEVICE_NOT_REGISTERED);
    }
    if (device.getState() != ProjectDevice2.State.Approved){
      if (device.getState() == ProjectDevice2.State.Disabled){
        throw new DeviceServiceException(new DeviceResponseBuilder().DEVICE_DISABLED);
      }
      if (device.getState() == ProjectDevice2.State.Pending){
        throw new DeviceServiceException(new DeviceResponseBuilder().DEVICE_PENDING);
      }
      throw new DeviceServiceException(new DeviceResponseBuilder().DEVICE_UNKNOWN_STATE);
    }
    return device;
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
      projectController.addMembers(project, project.getOwner().getEmail(), list);

      // Generates a random UUID to serve as the project secret.
      String projectSecret = UUID.randomUUID().toString();

      // Saves Project Secret
      deviceFacade.createProjectDevicesSettings(
        new ProjectDevicesSettings(projectId, projectSecret, projectTokenDurationInHours));
      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}]",
              PROJECT_ID, JWT_DURATION_IN_HOURS));
    }
  }

  //===============================================================================================================
  // DEVICE ENDPOINTS
  //===============================================================================================================

  /**
   * Registers a device under a project.
   */
  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRegisterEndpoint(
    @Context HttpServletRequest req, AuthProjectDeviceDTO authDTO) throws AppException {
    try {
      getProjectDevicesSettings(authDTO.getProjectId());
      InputValidator.validate(authDTO);
      try {
        authDTO.setPassword(DigestUtils.sha256Hex(authDTO.getPassword()));
        deviceFacade.createProjectDevice(authDTO);
        return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
      }catch (Exception e) {
        throw new DeviceServiceException(new DeviceResponseBuilder().DEVICE_ALREADY_REGISTERED);
      }
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Logs in a device under a project.
   */
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postLoginEndpoint(@Context HttpServletRequest req, AuthProjectDeviceDTO authDTO) throws AppException {
    try {
      ProjectDevicesSettings devicesSettings = getProjectDevicesSettings(authDTO.getProjectId());
      InputValidator.validate(authDTO);
      ProjectDevice2 device = getProjectDevice(authDTO.getProjectId(), authDTO.getDeviceUuid());
      if (device.getPassword().equals(DigestUtils.sha256Hex(authDTO.getPassword()))) {
        deviceFacade.updateProjectDeviceLastLoggedIn(device);
        return DeviceResponseBuilder.successfulJsonResponse(
          Status.OK, DeviceServiceSecurity.generateJwt(devicesSettings, device));
      }
      return new DeviceResponseBuilder().DEVICE_LOGIN_FAILED;
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

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

      ProjectDevicesSettings projectDevicesSettings = getProjectDevicesSettings(projectId);

      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      DeviceServiceSecurity.verifyJwt(projectDevicesSettings, jwtToken);
      // Device is authenticated at this point.

      return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return DeviceResponseBuilder.failedJsonResponse(Status.BAD_REQUEST,
        "GET Request is malformed! Required params are: project_id");
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

      ProjectDevicesSettings projectDevicesSettings = getProjectDevicesSettings(projectId);

      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      DeviceServiceSecurity.verifyJwt(projectDevicesSettings, jwtToken);
      // Device is authenticated at this point.

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
      Integer projectId = json.getInt(PROJECT_ID);
      String topicName = json.getString(TOPIC);
      JSONArray records = json.getJSONArray(RECORDS);

      // Retrieves the project secret
      ProjectDevicesSettings projectDevicesSettings = getProjectDevicesSettings(projectId);

      // Verifies jwtToken
      String jwtToken = getJwtFromAuthorizationHeader(req.getHeader(AUTHORIZATION_HEADER));
      DecodedJWT decodedJwt = DeviceServiceSecurity.verifyJwt(projectDevicesSettings, jwtToken);
      // The device is authenticated at this point.

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
        return new DeviceResponseBuilder().PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND;
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
