package io.hops.hopsworks.api.device;


import javax.ws.rs.core.Response;

public class DeviceServiceException extends Exception {

  private Response deviceResponse;

  public DeviceServiceException(Response deviceResponse){
    this.deviceResponse = deviceResponse;
  }

  public Response getResponse(){
    return this.deviceResponse;
  }
}
