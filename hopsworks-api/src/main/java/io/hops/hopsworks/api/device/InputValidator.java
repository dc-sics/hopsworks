package io.hops.hopsworks.api.device;

import io.hops.hopsworks.common.dao.device.AuthProjectDeviceDTO;

import javax.ws.rs.core.Response;


public class InputValidator {

  private static final String UUID_V4_REGEX =
    "/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/";

  public static void validate(AuthProjectDeviceDTO authProjectDeviceDTO) throws DeviceServiceException {
    if (authProjectDeviceDTO == null || authProjectDeviceDTO.getProjectId() == null ||
      authProjectDeviceDTO.getDeviceUuid() == null || authProjectDeviceDTO.getPassword() == null){
      throw new DeviceServiceException(DeviceResponseBuilder.failedJsonResponse(
        Response.Status.BAD_REQUEST,
        "One or more of the mandatory params is missing: projectId, deviceUuid, password"));
    }
    if (!authProjectDeviceDTO.getDeviceUuid().matches(UUID_V4_REGEX)){
      throw new DeviceServiceException(DeviceResponseBuilder.failedJsonResponse(
        Response.Status.BAD_REQUEST,
        "The deviceUuid param must be a valid UUID version 4."));
    }
  }
}
