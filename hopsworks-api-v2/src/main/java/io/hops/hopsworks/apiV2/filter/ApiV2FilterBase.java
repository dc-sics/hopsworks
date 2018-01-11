package io.hops.hopsworks.apiV2.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

public abstract class ApiV2FilterBase implements ContainerRequestFilter{
  
  @Override
  public final void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getPath();
    String[] pathParts = path.split("/");
  
    if (pathParts.length > 0 && "v2".equalsIgnoreCase(pathParts[0])) {
      //Only apply filter to v2-endpoints
      filterInternal(requestContext);
    }
  }
  
  protected abstract void filterInternal(ContainerRequestContext v2RequestContext)
      throws IOException;
}
