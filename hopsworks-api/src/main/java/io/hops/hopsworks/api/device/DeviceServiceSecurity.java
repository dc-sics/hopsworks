package io.hops.hopsworks.api.device;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.common.dao.device.ProjectDevice;
import io.hops.hopsworks.common.dao.device.ProjectSecret;

import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;

class DeviceServiceSecurity {

  private static final String DEVICE_UUID = "deviceUuid";

  private static final String PROJECT_ID = "projectId";


  /***
   * This method generates a jwt token (RFC 7519) which is unencrypted but signed with the given projectSecret.
   *
   * @param projectSecret Contains the secret which is used to sign the jwt token.
   * @param projectDevice Contains the device identification information for the project.
   * @return Returns the jwt token.
   */
  static String generateJwt(ProjectSecret projectSecret, ProjectDevice projectDevice) {

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR_OF_DAY, projectSecret.getJwtTokenDuration());
    Date expirationDate = cal.getTime();

    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      return JWT.create()
        .withExpiresAt(expirationDate)
        .withClaim(PROJECT_ID, projectDevice.getProjectDevicePK().getProjectId())
        .withClaim(DEVICE_UUID, projectDevice.getProjectDevicePK().getDeviceUuid())
        .sign(algorithm);
    } catch (Exception e) {
      return null;
    }
  }


  /***
   * This method verifies the validity of a jwt token (RFC 7519) by checking the signature of the token
   * against the provided projectSecret.
   *
   * @param projectSecret Contains the secret which is used to verify the jwt token.
   * @param jwtToken The jwt token
   * @return Returns null if the token is verified or an Unauthorized Response with the reason for the failure.
   */
   static Response verifyJwt(ProjectSecret projectSecret, String jwtToken) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      verifier.verify(jwtToken);
      return null;
    }catch (TokenExpiredException exception){
      return DeviceResponseBuilder.failedJsonResponse(
        Response.Status.UNAUTHORIZED, "Jwt token has expired. Try to login again.");
    }catch (Exception exception){
      return DeviceResponseBuilder.failedJsonResponse(
        Response.Status.UNAUTHORIZED, "The Jwt token is invalid.");
    }
  }

  /***
   * This method decodes the jwt token (RFC 7519). Must be used only after the jwt token has been verified.
   *
   * @param projectSecret Contains the secret which is used to decode the jwt token.
   * @param jwtToken The jwt token
   * @return Returns a DecodedJWT object or null if the token could not be decoded.
   */
  static DecodedJWT getDecodedJwt(ProjectSecret projectSecret, String jwtToken){
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectSecret.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      return verifier.verify(jwtToken);
    }catch (Exception e){
      return null;
    }
  }
}
