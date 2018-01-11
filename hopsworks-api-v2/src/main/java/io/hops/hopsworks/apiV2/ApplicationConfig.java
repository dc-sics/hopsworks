package io.hops.hopsworks.apiV2;

import io.hops.hopsworks.apiV2.mapper.AccessControlExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.AppExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.AuthExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.ThrowableExceptionMapper;
import io.hops.hopsworks.apiV2.mapper.TransactionExceptionMapper;
import io.hops.hopsworks.apiV2.projects.BlobsResource;
import io.hops.hopsworks.apiV2.projects.DatasetsResource;
import io.hops.hopsworks.apiV2.projects.MembersResource;
import io.hops.hopsworks.apiV2.projects.PathValidator;
import io.hops.hopsworks.apiV2.projects.ProjectsResource;
import io.hops.hopsworks.apiV2.users.UsersResource;
import io.swagger.annotations.Api;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;

@Api
@javax.ws.rs.ApplicationPath("/")
public class ApplicationConfig extends ResourceConfig {
  public ApplicationConfig(){
  
    register(AccessControlExceptionMapper.class);
    register(AppExceptionMapper.class);
    register(AuthExceptionMapper.class);
    register(ThrowableExceptionMapper.class);
    register(TransactionExceptionMapper.class);
    
    //API V2
    //Projects & Datasets
    //register(ProjectAuthFilter.class);
    register(ProjectsResource.class);
    register(DatasetsResource.class);
    register(MembersResource.class);
    register(BlobsResource.class);
    register(PathValidator.class);
  
    //Hopsworks-Users
    register(UsersResource.class);
  
    //swagger
    register(ApiListingResource.class);
    register(SwaggerSerializers.class);
  }
}
