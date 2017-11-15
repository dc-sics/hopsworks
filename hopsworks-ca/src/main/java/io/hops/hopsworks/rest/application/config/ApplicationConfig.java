package io.hops.hopsworks.rest.application.config;

import io.hops.hopsworks.api.filter.CORSFilter;
import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("ca")
public class ApplicationConfig extends ResourceConfig {

  /**
   * adding manually all the restful services of the application.
   */
  public ApplicationConfig() {
    register(io.hops.hopsworks.api.certs.CertSigningService.class);
    
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    
    //response filters
    register(CORSFilter.class);
    
    //Exception mappers
    register(io.hops.hopsworks.api.exception.mapper.EJBExceptionMapper.class);
 
    //swagger
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
