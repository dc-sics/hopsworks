package io.hops.hopsworks.apiV2.filter;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class NoCacheResponse {
  
  public static ResponseBuilder getNoCacheResponseBuilder(Response.Status
      status) {
    CacheControl cc = getNoCacheControl();
    return Response.status(status).cacheControl(cc);
  }
  
  private static CacheControl getNoCacheControl(){
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    cc.setMaxAge(-1);
    cc.setMustRevalidate(true);
    return cc;
  }
}
