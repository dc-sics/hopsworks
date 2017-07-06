package io.hops.hopsworks.api.jobs;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceService {

  private final static Logger LOGGER = Logger.getLogger(
      DeviceService.class.getName());
  
  private static final Integer TOKEN_STATUS_INVALID = 0; // Token is bogus or malformed. Device is not authorized.
  private static final Integer TOKEN_STATUS_VALID = 1;   // Token is authentic. Device is authorized.
  private static final Integer TOKEN_STATUS_EXPIRED = 2; // Token has expired. Device needs to login to gain a new one.
  
  private static final String DEVICE_UUID = "deviceUuid";
  private static final String PASS_UUID = "passUuid";
  private static final String PROJECT_USER_UUID = "projectUserUuid";
  private static final String PROJECT_ID = "projectId";

  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private KafkaFacade kafkaFacade;

  private Integer projectId;
  
  @EJB
  private UserManager userManager;

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
  
  private static String getJwtToken(HttpServletRequest req) {
    return req.getHeader("jwt");
  }
  
  private static void setJwtToken(HttpServletResponse resp, String jwtToken) {
    resp.setHeader("jwt", jwtToken);
  }
  
  /**
   * This method uses this JWT implementation: https://github.com/auth0/java-jwt
   */
  private static String generateJwt(Integer projectId, String projectJwtSecret, Integer projectJwtDurationInHours, String deviceUuid, String projectUserUuid) {
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR_OF_DAY, projectJwtDurationInHours);
    Date expirationDate = cal.getTime();
    
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectJwtSecret);
      String token = JWT.create()
          .withExpiresAt(expirationDate)
          .withClaim(DEVICE_UUID, deviceUuid)
          .withClaim(PROJECT_ID, projectId) // Optional
          .withClaim(PROJECT_USER_UUID, projectUserUuid) // Consider if worth it.
          .sign(algorithm);
      return token;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private static Integer verifyJwt(String projectJwtSecret, Integer projectJwtDurationInHours, String token) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectJwtSecret);
      JWTVerifier verifier = JWT.require(algorithm).build();
      verifier.verify(token);
      return TOKEN_STATUS_VALID;
    }catch (TokenExpiredException exception){
      return TOKEN_STATUS_EXPIRED;
    }catch (Exception exception){
      return TOKEN_STATUS_INVALID;
    }
  }
  
  private static DecodedJWT getDecodedJwt(String projectJwtSecret, Integer projectJwtDurationInHours, String token) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectJwtSecret);
      JWTVerifier verifier = JWT.require(algorithm)
          .build();
      return verifier.verify(token);
    }catch (Exception exception){
      return null;
    }
  }
  
  private static Claim getClaim(DecodedJWT decodedJwt, String claimName) {
    return decodedJwt.getClaim(claimName);
  }

  @POST
  @Path("/add-data-owner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response addProjectUserUuid(
      @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    
    checkForProjectId();
    
    //TODO: if projectSecrets record does not exist create a new one.

    String projectUserUuid = UUID.randomUUID().toString();
    String userEmail = sc.getUserPrincipal().getName();
    deviceFacade.addProjectUserUuid(projectId, userEmail, projectUserUuid);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
  
  @POST
  @Path("/remove-data-owner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectUserUuid(
      @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    
    checkForProjectId();
    
    String userEmail = sc.getUserPrincipal().getName();
    deviceFacade.removeProjectUserUuid(projectId, userEmail);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
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
      String deviceUuid = json.getString(DEVICE_UUID);
      String passUuid = json.getString(PASS_UUID);
      String projectUserUuid = json.getString(PROJECT_USER_UUID);
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
  @Path("/produce")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response produce(@Context HttpServletRequest req, String jsonString) throws AppException {
    checkForProjectId();
    
    //TODO: requires JWT token. Get and Verify JWT token
    
    try {
      JSONObject json = new JSONObject(jsonString);
      String topicName = json.getString("topic");
      JSONArray records = json.getJSONArray("records");
      
      //TODO: Check if ArrayList of String works.
      ArrayList<String> recordsStringified = new ArrayList<>();
      for(int i=0;i<records.length(); i++) {
        recordsStringified.add(records.getString(i));
      }
      
      String deviceUuid = getClaim(null, DEVICE_UUID).asString();
      String projectUserUuid = getClaim(null, PROJECT_USER_UUID).asString();
      
      //TODO: Find user email from projectUserUuid
      String userEmail = null;
      
      Users user = userManager.getUserByEmail(userEmail);
      
      //TODO: Get template for the given topicName (maybe) if validation is required during the producing stage.
      //TODO: Extra check can be added to check if the deviceUuid of the posted records are the same with the Jwt token.
      
      try {
        kafkaFacade.produce(projectId, user, topicName, recordsStringified);
      } catch (Exception e) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! Required properties are [topic, schema, version].");
    }
   
  }

  @GET
  @Path("/validate-schema")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateSchema(@Context HttpServletRequest req, String jsonString) throws AppException {

    checkForProjectId();
    
    //TODO: requires JWT token. Get and Verify JWT token

    try {
      JSONObject json = new JSONObject(jsonString);
      String topicName = json.getString("topic");
      
      //TODO: Get schemaName and version form topicName instead.
      
      String schemaName = json.getString("schema");
      Integer schemaVersion = json.getInt("version");
      
      String schemaPayload = json.getString("payload").trim();
      String schemaStored = kafkaFacade.getSchemaContent(schemaName, schemaVersion).getContents().trim();
      if (schemaStored.equals(schemaPayload)) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
      }else {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
      }
    }catch(JSONException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Json request is malformed! Required properties are [topic, schema, version, payload].");
    }
  }  

}


