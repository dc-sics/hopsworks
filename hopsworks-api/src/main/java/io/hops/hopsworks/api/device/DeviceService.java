package io.hops.hopsworks.api.device;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.hops.hopsworks.api.filter.NoCacheResponse;
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
public class DeviceService {

  private final static Logger logger = Logger.getLogger(DeviceService.class.getName());

  private static final String DEVICE_UUID = "deviceUuid";
  private static final String PASS_UUID = "passUuid";
  private static final String USER_ID = "userId";
  private static final String PROJECT_ID = "projectId";
  
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOPIC = "topic";
  private static final String RECORDS = "records";
  private static final String SCHEMA = "schema";
  private static final String SCHEMA_PAYLOAD = "schemaPayload";

  private static final String JWT_DURATION_IN_HOURS = "jwtTokenDurationInHours";

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private UserManager userManager;

  @EJB
  private DeviceFacade deviceFacade;
  
  @EJB
  private KafkaFacade kafkaFacade;

  public DeviceService() {
  }

  private static Response failedJsonResponse(Status status, String errorMessage) {
    ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    JsonResp resp = new JsonResp(status.getStatusCode(), status.getReasonPhrase(), errorMessage);
    rb.entity(resp);
    return rb.build();
  }

  private static Response successfulJsonResponse(Status status) {
    ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    JsonResp resp = new JsonResp(status.getStatusCode(), status.getReasonPhrase());
    rb.entity(resp);
    return rb.build();
  }
  
  private static Response successfulJsonResponse(Status status, String jwt) {
    ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    JsonResp resp = new JsonResp(status.getStatusCode(), status.getReasonPhrase());
    resp.setJwt(jwt);
    rb.entity(resp);
    return rb.build();
  }

  /***
   * This method generates a jwt token (RFC 7519) which is unencrypted but signed with the given projectSecret.
   *
   * @param projectSecret Contains the secret which is used to sign the jwt token.
   * @param projectDevice Contains the device identification information for the project.
   * @return Returns the jwt token.
   */
  private static String generateJwt(ProjectSecret projectSecret, ProjectDevice projectDevice) {

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR_OF_DAY, projectSecret.getJwtTokenDuration());
    Date expirationDate = cal.getTime();

    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      String token = JWT.create()
              .withExpiresAt(expirationDate)
              .withClaim(PROJECT_ID, projectDevice.getProjectDevicePK().getProjectId())
              .withClaim(DEVICE_UUID, projectDevice.getProjectDevicePK().getDeviceUuid())
              .withClaim(USER_ID, projectDevice.getUserId())
              .sign(algorithm);
      return token;
    } catch (Exception e) {
      logger.log(Level.WARNING, "JWT token generation failed.", e);
      return null;
    }
  }

  /***
   * This method verifies the validity of a jwt token (RFC 7519) by checking the signature of the token
   * against the provided projectSecret.
   *
   * @param projectSecret Contains the secret which is used to verify the jwt token.
   * @param authorizationHeader The Authorization header that contains the jwt token
   * @return Returns null if the token is verified or an Unauthorized Response with the reason for the failure.
   */
  private static Response verifyJwt(ProjectSecret projectSecret, String authorizationHeader) {
    if (authorizationHeader == null) {
      return failedJsonResponse(Status.UNAUTHORIZED, "No Authorization header is present.");
    }

    if(!authorizationHeader.startsWith("Bearer")){
      return failedJsonResponse(Status.BAD_REQUEST,
              "The value of the Authorization header must start with 'Bearer ' " +
                      "followed by the jwt token.");
    }
    
    try {
      String jwtToken = authorizationHeader.substring("Bearer".length()).replaceAll("\\s","");
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      verifier.verify(jwtToken);
      return null;
    }catch (TokenExpiredException exception){
      return failedJsonResponse(Status.UNAUTHORIZED, "Jwt token has expired. Try to login again.");
    }catch (Exception exception){
      return failedJsonResponse(Status.UNAUTHORIZED, "The Jwt token is invalid.");
    }
  }

  /***
   * This method decodes the jwt token (RFC 7519). Must be used only after the jwt token has been verified.
   *
   * @param projectSecret Contains the secret which is used to decode the jwt token.
   * @param jwtToken The jwt token
   * @return Returns a DecodedJWT object or null if the token could not be decoded.
   */
  private static DecodedJWT getDecodedJwt(ProjectSecret projectSecret, String jwtToken) throws Exception {
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      return verifier.verify(jwtToken);
    }catch (Exception e){
      logger.log(Level.WARNING, "JWT token decoding failed", e);
      return null;
    }
  }

  private ProjectSecret getProjectSecret(Integer projectId){
    try {
      return deviceFacade.getProjectSecret(projectId);
    }catch (Exception e) {
      return null;
    }
  }
  
  /**
   * Endpoint for testing purposes
   */
  @GET
  @Path("/test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response testEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    return successfulJsonResponse(Status.OK, "jwtTokenValue");
  }


  /**
   * Needs to be activated only once per project.
   */
  @POST
  @Path("/activate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response activate(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      JSONObject json = new JSONObject(jsonString);
      Integer projectId = json.getInt(PROJECT_ID);
      Integer projectTokenDurationInHours = json.getInt(JWT_DURATION_IN_HOURS);
      String projectSecret = UUID.randomUUID().toString();
      deviceFacade.addProjectSecret(projectId, projectSecret, projectTokenDurationInHours);
      return successfulJsonResponse(Status.OK);
    } catch (JSONException e) {
      return failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}]",
              PROJECT_ID, JWT_DURATION_IN_HOURS));
    }
  }


  /**
   * Endpoint to verify the Jwt token.
   */
  @GET
  @Path("/verify-token")
  @Produces(MediaType.APPLICATION_JSON)
  public Response verifyTokenEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      Integer projetId = Integer.valueOf(req.getParameter(PROJECT_ID));
      ProjectSecret projectSecret = deviceFacade.getProjectSecret(projetId);
      Response verification = verifyJwt(projectSecret, req.getHeader(AUTHORIZATION_HEADER));
      if (verification != null){
        return verification;
      }
      return successfulJsonResponse(Status.OK);
    } catch (Exception e) {
      return failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "GET Request is malformed! Required params are [{0}]", PROJECT_ID));
    }
  }



  /**
   * Register end-point for project devices. COMPLETED.
   */
  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response registerDeviceEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject json = new JSONObject(jsonString);
      String deviceUuid = json.getString(DEVICE_UUID);
      String passUuid = json.getString(PASS_UUID);
      Integer userId = json.getInt(USER_ID);
      Integer projectId = json.getInt(PROJECT_ID);
      
      //TODO: Check if device registration for the given project is activated
      
      try {
        deviceFacade.addProjectDevice(projectId, userId, deviceUuid, passUuid);
        return successfulJsonResponse(Status.OK);
      }catch (Exception e) {
        return failedJsonResponse(Status.UNAUTHORIZED, MessageFormat.format(
                "Device is already registered for this project and/or {0} is invalid.", USER_ID));
      }
    }catch(JSONException e) {
      return failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}, {2}, {3}]",
              PROJECT_ID, DEVICE_UUID, PASS_UUID, USER_ID));
    }
  }
  

  /**
   * Login end-point for project devices. COMPLETED.
   */
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response loginDeviceEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject jsonRequest = new JSONObject(jsonString);
      String deviceUuid = jsonRequest.getString(DEVICE_UUID);
      String passUuid = jsonRequest.getString(PASS_UUID);
      Integer projectId = jsonRequest.getInt(PROJECT_ID);

      ProjectSecret secret;
      try {
        secret = deviceFacade.getProjectSecret(projectId);
      }catch (Exception e) {
        return failedJsonResponse(Status.FORBIDDEN, "Project devices feature is not active.");
      }

      ProjectDevice device;
      try {
        device = deviceFacade.getProjectDevice(projectId, deviceUuid);
      }catch (Exception e) {
        return failedJsonResponse(Status.UNAUTHORIZED, MessageFormat.format(
                "No device is registered with the given {0}.", DEVICE_UUID));
      }

      if (device.getPassUuid().equals(passUuid)) {
        return successfulJsonResponse(Status.OK, generateJwt(secret, device));
      }else {
        return failedJsonResponse(Status.UNAUTHORIZED, MessageFormat.format("{0} is incorrect.", PASS_UUID));
      }
    }catch(JSONException e) {
      return failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}, {2}]",
              PROJECT_ID, DEVICE_UUID, PASS_UUID));
    }
  }

  @POST
  @Path("/produce")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response produceRecordsEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {
  
    JSONObject json = new JSONObject(jsonString);
    Integer projectId = json.getInt(PROJECT_ID);
    
    ProjectSecret secret;
    try {
      secret = deviceFacade.getProjectSecret(projectId);
    }catch (Exception e) {
      return failedJsonResponse(Status.FORBIDDEN, "Project devices feature is not active.") ;
    }
    
    String jwtToken = req.getHeader(AUTHORIZATION_HEADER);
    Response authFailedResponse = verifyJwt(secret, jwtToken);
    if (authFailedResponse != null) {
      return authFailedResponse;
    }
    // Device is authenticated at this point.
    
    DecodedJWT decodedJwt;
    Integer userId;
    try {
      decodedJwt = getDecodedJwt(secret, jwtToken);
      userId = decodedJwt.getClaim(USER_ID).asInt();
    }catch(Exception e) {
      return failedJsonResponse(Status.INTERNAL_SERVER_ERROR, "I hate it when this happens.");
    }
    // Device is correlated to a userId at this point.

    try {
      String topicName = json.getString(TOPIC);
      JSONArray records = json.getJSONArray(RECORDS);

      //TODO: Check if ArrayList of String works.
      ArrayList<String> recordsStringified = new ArrayList<>();
      for(int i=0;i<records.length(); i++) {
        recordsStringified.add(records.getString(i));
      }

      Users user = userManager.getUserByUid(userId);

      try {
        kafkaFacade.produce(projectId, user, topicName, recordsStringified);
      } catch (Exception e) {
        return failedJsonResponse(
            Status.INTERNAL_SERVER_ERROR,"Something went wrong while producing to Kafka.");
      }
      return successfulJsonResponse(Status.OK, null);
    }catch(JSONException e) {
      return failedJsonResponse(Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties are [{0}, {1}]", TOPIC, RECORDS));
    }

  }

  /**
   * Validate the schema of a topic before producing to that topic.
   */
  @POST
  @Path("/validate-schema")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateSchemaEndpoint(@Context HttpServletRequest req, String jsonString) throws AppException {

    try {
      JSONObject json = new JSONObject(jsonString);
      Integer projectId = json.getInt(PROJECT_ID);
      String topicName = json.getString(TOPIC);
      String schemaName = json.getString(SCHEMA);
      String schemaPayload = json.getString(SCHEMA_PAYLOAD).trim();

      ProjectSecret secret = getProjectSecret(projectId);
      if (secret == null){
        return failedJsonResponse(Status.FORBIDDEN, "Project devices feature is not active.");
      }

      String jwtToken = req.getHeader(AUTHORIZATION_HEADER);
      Response authFailedResponse = verifyJwt(secret, jwtToken);
      if (authFailedResponse != null) {
        return authFailedResponse;
      }
      // Device is authenticated at this point

      SchemaDTO schemaDTO = kafkaFacade.getSchemaForTopic(topicName);
      if(schemaDTO.getName().equals(schemaName)){
        if (schemaDTO.getContents().trim().equals(schemaPayload)) {
          return successfulJsonResponse(Status.OK, null);
        }else {
          return failedJsonResponse(Status.BAD_REQUEST,
                  "Schema name is the same but the actual schema for the topic is different.");
        }
      }else {
        return failedJsonResponse(Status.BAD_REQUEST, "Schema name for topic is different");
      }
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), MessageFormat.format(
          "Json request is malformed! Required properties are [{0}, {1}, {2}, {3}].",
              PROJECT_ID, TOPIC, SCHEMA, SCHEMA_PAYLOAD));
    }
  }

}


