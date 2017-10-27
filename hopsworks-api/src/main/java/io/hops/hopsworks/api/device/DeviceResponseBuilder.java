package io.hops.hopsworks.api.device;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class DeviceResponseBuilder {

  public static final Response  DEVICES_FEATURE_NOT_ACTIVE = failedJsonResponse(Response.Status.FORBIDDEN,
          "The devices feature for this project is not activated.");

  public static final Response  DEVICE_NOT_REGISTERED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device is not registered.");

  public static final Response  DEVICE_ALREADY_REGISTERED = failedJsonResponse(Response.Status.CONFLICT,
    "The device with the provided identifier is already registered.");

  public static final Response  DEVICE_LOGIN_FAILED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device identifier and/or password is incorrect.");

  public static final Response  DEVICE_DISABLED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device has been disabled.");

  public static final Response  DEVICE_PENDING = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device has not been approved yet. Contact an administrator for approval.");

  public static final Response  DEVICE_UNKNOWN_STATE = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The state of the device can not been identified by the service. Contanct an administrator.");

  public static final Response  JWT_EXPIRED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The jwt token has expired. Try to login again to get a new one.");

  public static final Response  JWT_INVALID_TOKEN = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The jwt token provided is invalid.");

  public static final Response  JWT_GENERATION_FAILED = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The process to generate the authorization jwt token failed.");

  public static final Response  JWT_VALIDATION_FAILED = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The process to validate the authorization jwt token failed.");

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
