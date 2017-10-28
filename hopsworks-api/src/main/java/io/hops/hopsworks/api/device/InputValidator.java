package io.hops.hopsworks.api.device;

import io.hops.hopsworks.common.dao.device.AuthProjectDeviceDTO;

import javax.ws.rs.core.Response;


public class InputValidator {

  public static void validate(AuthProjectDeviceDTO authProjectDeviceDTO) throws DeviceServiceException {
    if (authProjectDeviceDTO == null || authProjectDeviceDTO.getProjectId() == null ||
      authProjectDeviceDTO.getDeviceUuid() == null || authProjectDeviceDTO.getPassword() == null){
      throw new DeviceServiceException(DeviceResponseBuilder.failedJsonResponse(
        Response.Status.BAD_REQUEST,
        "One of the following params is missing: projectId, deviceUuid, password, (alias)"));
    }
  }
}
