package io.hops.hopsworks.api.device;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import io.hops.hopsworks.api.util.JsonResponse;
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

  private final static Logger LOGGER = Logger.getLogger(
      DeviceService.class.getName());

  private static final String DEVICE_UUID = "deviceUuid";
  private static final String PASS_UUID = "passUuid";
  private static final String USER_ID = "userId";
  private static final String PROJECT_ID = "projectId";
  
  private static final String JWT_HEADER = "jwt";
  private static final String TOPIC = "topic";
  private static final String RECORDS = "records";
  private static final String SCHEMA = "schema";
  private static final String SCHEMA_PAYLOAD = "schemaPayload";

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
  
  private static Response failedJsonResponse(
      Status status, String errorMessage) {
    JsonResponse json = new JsonResponse();
    json.setErrorMsg(errorMessage);
    ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    rb.entity(json);
    return rb.build();
  }

  private static Response successfullJsonResponse(
      Status status, JsonResponse jsonResponse) {
    ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    if (jsonResponse != null) {
      rb.entity(jsonResponse);
    }
    return rb.build();
  }
  
  private static Response successfullJsonResponseWithJwt(
      Status status, JsonResponse jsonResponse, String jwtToken) {
    ResponseBuilder rb = Response.status(status);
    rb.header(JWT_HEADER, jwtToken);
    rb.type(MediaType.APPLICATION_JSON);
    if (jsonResponse != null) {
      rb.entity(jsonResponse);
    }
    return rb.build();
  }

  /**
   * This method uses this JWT implementation: https://github.com/auth0/java-jwt
   */
  private static String generateJwt(
      ProjectSecret projectSecret, ProjectDevice projectDevice) {

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR_OF_DAY, projectSecret.getJwtTokenDuration());
    Date expirationDate = cal.getTime();

    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      String token = JWT.create()
          .withExpiresAt(expirationDate)
          .withClaim(
              PROJECT_ID, projectDevice.getProjectDevicePK().getProjectId())
          .withClaim(USER_ID, projectDevice.getUserId())
          .withClaim(
              DEVICE_UUID, projectDevice.getProjectDevicePK().getDeviceUuid())
          .sign(algorithm);
      return token;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   *  Returns an automated failed JsonResponse if there is something wrong
   *  with the jwtToken.
   *  If the jwtToken is successfully verified then null is returned.
   */
  private static Response verifyJwt(
      ProjectSecret projectSecret, String jwtToken) {
    if (jwtToken == null) {
      return failedJsonResponse(
          Status.UNAUTHORIZED,
          "No jwt token is present in the request.");
    }
    
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      verifier.verify(jwtToken);
      return null;
    }catch (TokenExpiredException exception){
      return failedJsonResponse(
          Status.UNAUTHORIZED,
          "Jwt token has expired. Try to login again.");
    }catch (Exception exception){
      return failedJsonResponse(
          Status.UNAUTHORIZED,
          "The Jwt token is invalid.");
    }
  }
  
  private static DecodedJWT getDecodedJwt(
      ProjectSecret projectSecret, String token) throws Exception {
    Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
    JWTVerifier verifier = JWT.require(algorithm).build();
    return verifier.verify(token);
  }
  
  /**
   * Test end-point
   */
  @GET
  @Path("/test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response testDevice(
      @Context HttpServletRequest req, String jsonString) throws AppException {
    
    return successfullJsonResponse(Status.OK, null);
  }

  /**
   * Register end-point for project devices. COMPLETED.
   */
  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response registerDevice(
      @Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject json = new JSONObject(jsonString);
      String deviceUuid = json.getString(DEVICE_UUID);
      String passUuid = json.getString(PASS_UUID);
      Integer userId = json.getInt(USER_ID);
      Integer projectId = json.getInt(PROJECT_ID);
      try {
        deviceFacade.addProjectDevice(projectId, userId, deviceUuid, passUuid);
        return successfullJsonResponse(Status.OK, null);
      }catch (Exception e) {
        return failedJsonResponse(
            Status.UNAUTHORIZED, MessageFormat.format(
                "Device is already registered for this project " +
                    "and/or {0} is invalid.", USER_ID));
      }
    }catch(JSONException e) {
      return failedJsonResponse(
          Status.BAD_REQUEST, MessageFormat.format("Json request is " +
              "malformed! Required properties are [{0}, {1}, {2}]",
              DEVICE_UUID, PASS_UUID, USER_ID));
    }
  }
  


  /**
   * Login end-point for project devices. COMPLETED.
   */
  @GET
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response loginDevice(
      @Context HttpServletRequest req, String jsonString) throws AppException {
    
    try {
      JSONObject jsonRequest = new JSONObject(jsonString);
      String deviceUuid = jsonRequest.getString(DEVICE_UUID);
      String passUuid = jsonRequest.getString(PASS_UUID);
      Integer projectId = jsonRequest.getInt(PROJECT_ID);

      ProjectSecret secret;
      try {
        secret = deviceFacade.getProjectSecret(projectId);
      }catch (Exception e) {
        return failedJsonResponse(
            Status.FORBIDDEN,
            "Project devices feature is not active.");
      }

      ProjectDevice device;
      try {
        device = deviceFacade.getProjectDevice(projectId, deviceUuid);
      }catch (Exception e) {
        return failedJsonResponse(
            Status.UNAUTHORIZED, MessageFormat.format(
                "No device is registered with the given {0}.",
                DEVICE_UUID));
      }

      if (device.getPassUuid().equals(passUuid)) {
        return successfullJsonResponseWithJwt(
            Status.OK, null, generateJwt(secret, device));
      }else {
        return failedJsonResponse(
            Status.UNAUTHORIZED, MessageFormat.format(
                "{0} is incorrect.", PASS_UUID));
      }
    }catch(JSONException e) {
      return failedJsonResponse(
          Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties " +
                  "are [{0}, {1}]", DEVICE_UUID, PASS_UUID));
    }
  }

  @POST
  @Path("/produce")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response produce(
      @Context HttpServletRequest req, String jsonString) throws AppException {
  
    JSONObject json = new JSONObject(jsonString);
    Integer projectId = json.getInt(PROJECT_ID);
    
    ProjectSecret secret;
    try {
      secret = deviceFacade.getProjectSecret(projectId);
    }catch (Exception e) {
      return failedJsonResponse(
          Status.FORBIDDEN,
          "Project devices feature is not active.") ;
    }
    
    String jwtToken = req.getHeader(JWT_HEADER);
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
      return failedJsonResponse(
          Status.INTERNAL_SERVER_ERROR,
          "I hate it when this happens.");
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
            Status.INTERNAL_SERVER_ERROR,
            "Something went wrong while producing to Kafka.");
      }
      return successfullJsonResponse(Status.OK, null);
    }catch(JSONException e) {
      return failedJsonResponse(
          Status.BAD_REQUEST, MessageFormat.format(
              "Json request is malformed! Required properties " +
                  "are [{0}, {1}]", TOPIC, RECORDS));
    }

  }

  /**
   * Validate the schema of a topic before producing to that topic. COMPLETED.
   */
  @GET
  @Path("/validate-schema")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateSchema(
      @Context HttpServletRequest req, String jsonString) throws AppException {
  
    JSONObject json = new JSONObject(jsonString);
    Integer projectId = json.getInt(PROJECT_ID);
    
    ProjectSecret secret;
    try {
      secret = deviceFacade.getProjectSecret(projectId);
    }catch (Exception e) {
      return failedJsonResponse(Status.FORBIDDEN,
          "Project devices feature is not active.") ;
    }
    
    String jwtToken = req.getHeader(JWT_HEADER);
    Response authFailedResponse = verifyJwt(secret, jwtToken);
    if (authFailedResponse != null) {
      return authFailedResponse;
    }
    //Device is authenticated at this point

    try {
      String topicName = json.getString(TOPIC);
      String schemaName = json.getString(SCHEMA);
      String schemaPayload = json.getString(SCHEMA_PAYLOAD).trim();
      
      SchemaDTO schemaDTO = kafkaFacade.getSchemaForTopic(topicName);
      if(schemaDTO.getName().equals(schemaName)){
        if (schemaDTO.getContents().trim().equals(schemaPayload)) {
          return successfullJsonResponse(Status.OK, null);
        }else {
          return failedJsonResponse(
              Status.BAD_REQUEST,
              "Schema name is the same but the actual schema " +
                  "for the topic is different.");
        }
      }else {
        return failedJsonResponse(Status.BAD_REQUEST,
            "Schema name for topic is different");
      }
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! " +
              "Required properties are [topic, schema, version, payload].");
    }
  }

}


