package io.hops.hopsworks.api.device;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class DeviceResponseBuilder {

  public static final Response  DEVICES_FEATURE_NOT_ACTIVE = failedJsonResponse(Response.Status.FORBIDDEN,
          "The devices feature for this project is not activated.");

  public static final Response  PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND = failedJsonResponse(
    Response.Status.INTERNAL_SERVER_ERROR,
    "The passwords for the key store and the trust store that is used to produce to Kafka were not found.");

  public static Response failedJsonResponse(Response.Status status, String errorMessage) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase(), errorMessage);
    rb.entity(resp);
    return rb.build();
  }

  public static Response successfulJsonResponse(Response.Status status) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase());
    rb.entity(resp);
    return rb.build();
  }

  public static Response successfulJsonResponse(Response.Status status, String jwt) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase());
    resp.setJwt(jwt);
    rb.entity(resp);
    return rb.build();
  }

}
