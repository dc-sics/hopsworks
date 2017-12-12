package io.hops.hopsworks.ldap.rest.application.config;

import io.swagger.annotations.Api;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends ResourceConfig {
  
  public ApplicationConfig() {
    register(io.hops.hopsworks.ldap.rest.UserService.class);
    register(io.hops.hopsworks.ldap.rest.AuthService.class);
    //swagger
    register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
  }
}
