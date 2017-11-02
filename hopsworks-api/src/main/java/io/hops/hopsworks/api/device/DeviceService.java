package io.hops.hopsworks.api.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.hops.hopsworks.api.filter.NoCacheResponse;
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

  private static final String DEFAULT_DEVICE_USER_EMAIL = "devices@hops.io";

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER = "Bearer";
  private static final String UUID_V4_REGEX =
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";

  private static final String PROJECT_ID = "projectId";
  private static final String DEVICE_UUID = "deviceUuid";
  private static final String TOPIC = "topic";
  private static final String RECORDS = "records";

  @EJB
  private DeviceFacade4 deviceFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private KafkaFacade2 kafkaFacade;

  @EJB
  private CertsFacade userCerts; // Only used for the produce endpoint

  @EJB
  private CertificateMaterializer certificateMaterializer; // Only used for the produce endpoint

  @EJB
  private Settings settings; // Only used for the produce endpoint

  @EJB
  private UserManager userManager; // Only used for the produce endpoint

  @EJB
  private ProjectController projectController; // Only used for the produce endpoint

  @EJB
  private ProjectFacade projectFacade; // Only used for the produce endpoint


  public DeviceService() {
  }

  /**
   * Returns the jwtToken from the contents of the Authorization header.
   *
   * @param authorizationHeader The entire contents of the Authorization header of a request.
   * @return The jwtToken as a string
   * @throws DeviceServiceException Throws an exception if the
   * Authorization header is missing or if the Bearer is not present.
   */
  private String getJwtFromAuthorizationHeader(String authorizationHeader) throws DeviceServiceException{
    if (authorizationHeader == null) {
      throw new DeviceServiceException(new DeviceResponseBuilder().AUTH_HEADER_MISSING);
    }

    if(!authorizationHeader.startsWith(BEARER)){
      throw new DeviceServiceException(new DeviceResponseBuilder().AUTH_HEADER_BEARER_MISSING);
    }

    return authorizationHeader.substring(BEARER.length()).replaceAll("\\s","");
  }

  /**
   * Retrieves the Project Devices Settings from the database for the specified project.
   * If no such record exists the device feature is considered to be disabled.
   *
   * @param projectId The projectId of a project
   * @return The Project Devices Settings of the project with the specified projectId
   * @throws DeviceServiceException  It is thrown when there is no Project Devices Settings in the database and as
   * such all endpoints defined in this EJB must catch this exception and block all incoming requests.
   */
  private ProjectDevicesSettings getProjectDevicesSettings(Integer projectId) throws DeviceServiceException{
    try {
      return deviceFacade.readProjectDevicesSettings(projectId);
    }catch (Exception e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().DEVICES_FEATURE_NOT_ACTIVE);
    }
  }

  /**
   * Retrieves the Project Device from the database for the specified pair (projectId, deviceUuid).
   *
   * @param projectId The projectId of a project
   * @param deviceUuid The deviceUuid of a device
   * @return The Project Device
   * @throws DeviceServiceException  It is thrown when there is no such record in the database and as such the device
   * is not yet registered in this project or there is such a record but the device is not in the Approved state.
   */
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

  /**
   * This method validates an AuthProjectDeviceDTO object and checks that no critical information is missing and that
   * the deviceUuid is a valid UUID version 4. A "No news is good news" policy is applied. If no exception is thrown
   * then the object is considered validated.
   *
   * @param authProjectDeviceDTO The object to validate
   * @throws DeviceServiceException It is thrown when there is a validation problem with the provided object.
   */
  private void validate(AuthProjectDeviceDTO authProjectDeviceDTO) throws DeviceServiceException {
    if (authProjectDeviceDTO == null || authProjectDeviceDTO.getProjectId() == null ||
      authProjectDeviceDTO.getDeviceUuid() == null || authProjectDeviceDTO.getPassword() == null){
      throw new DeviceServiceException(new DeviceResponseBuilder().AUTH_BAD_REQ);
    }
    if (!authProjectDeviceDTO.getDeviceUuid().matches(UUID_V4_REGEX)){
      throw new DeviceServiceException(new DeviceResponseBuilder().AUTH_UUID4_BAD_REQ);
    }
  }

  /**
   * Retrieves the entire keystore file of the provided path into a Base64 encoded string.
   *
   * @param keystoreFilePath the filepath to the keystore file.
   * @return a Base64 encoded string
   * @throws DeviceServiceException It is thrown when something went wrong with the retrieval of the file.
   */
  private String keystoreEncode(String keystoreFilePath) throws DeviceServiceException {
    try {
      FileInputStream kfin = new FileInputStream(new File(keystoreFilePath));
      byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
      return Base64.encodeBase64String(kStoreBlob);
    } catch (FileNotFoundException e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().PRODUCE_KEYSTORE_FILE_NOT_FOUND);
    } catch (IOException e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().PRODUCE_KEYSTORE_IO_EXCEPTION);
    }
  }

  /**
   * This method converts the provided records in JSONArray format into a List of Avro-formatted records.
   *
   * @param avroSchemaContents the avro schema for the records.
   * @param records the records to be converted.
   * @return A List of Avro-formatted records
   * @throws IOException
   */
  private List<GenericData.Record> toAvro(String avroSchemaContents, JSONArray records)
    throws IOException {

    ArrayList<GenericData.Record> list = new ArrayList<>();
    for (int i = 0; i < records.length(); i++) {
      JSONObject json = records.getJSONObject(i);
      Schema.Parser parser = new Schema.Parser();
      Schema schema = parser.parse(avroSchemaContents);
      Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json.toString());
      DatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
      list.add(reader.read(null, decoder));
    }
    return list;
  }

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
      validate(authDTO);
      try {
        authDTO.setPassword(DigestUtils.sha256Hex(authDTO.getPassword()));
        deviceFacade.createProjectDevice(authDTO);
        return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
      }catch (Exception e) {
        return new DeviceResponseBuilder().DEVICE_ALREADY_REGISTERED;
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
      validate(authDTO);
      ProjectDevice2 device = getProjectDevice(authDTO.getProjectId(), authDTO.getDeviceUuid());
      if (device.getPassword().equals(DigestUtils.sha256Hex(authDTO.getPassword()))) {
        deviceFacade.updateProjectDeviceLastLoggedIn(authDTO);
        return DeviceResponseBuilder.successfulJsonResponse(
          Status.OK, DeviceServiceSecurity.generateJwt(devicesSettings, device));
      }
      return new DeviceResponseBuilder().DEVICE_LOGIN_FAILED;
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Endpoint to  verify the jwt token provided in the Authorization Header.
   * Useful for testing purposes for developers of devices that are integrating towards hopsworks.
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
      return new DeviceResponseBuilder().JWT_VERIFY_TOKEN_BAD_REQ;
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Endpoint to get the schema of a topic under a project.
   * Useful for checking the schema of the topic on the client before producing.
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

      SchemaDTO schemaDTO;
      try {
        schemaDTO = kafkaFacade.getSchemaForProjectTopic(projectId, topicName);
      } catch (Exception e) {
        return new DeviceResponseBuilder().PROJECT_TOPIC_NOT_FOUND;
      }

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemaDTO).build();

    }catch(JSONException e) {
      return new DeviceResponseBuilder().GET_TOPIC_SCHEMA_BAD_REQ;
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Endpoint to produce to kafka the specified records to the specified topic of the specified project.
   */
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

      // Retrieves the project devices settings that contain the project's jwtSecret
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
        certPwDTO = projectController.getProjectSpecificCertPw(user, project.getName(), base64EncodedKeyStore);
      } catch (Exception e) {
        return new DeviceResponseBuilder().PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND;
      }

      // Extracts the Avro Schema contents from the database
      SchemaDTO schema = kafkaFacade.getSchemaForProjectTopic(projectId, topicName);
      try {
        List<GenericData.Record> avroRecords = toAvro(schema.getContents(), records);
        boolean success = kafkaFacade.produce(
          false, project, user, certPwDTO, deviceUuid, topicName, schema.getContents(), avroRecords);
        if (success){
          return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
        }else{
          return new DeviceResponseBuilder().PRODUCE_FAILED;
        }
      } catch (Exception e) {
        return new DeviceResponseBuilder().PRODUCE_FAILED;
      }
    }catch(JSONException e) {
      return new DeviceResponseBuilder().PRODUCE_BAD_REQ;
    }catch(DeviceServiceException e) {
      return e.getResponse();
    }

  }

}
