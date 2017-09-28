package io.hops.hopsworks.cluster;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("cluster")
public class Cluster {

  public Cluster() {
  }
  
  @POST
  @Path("register")
  @Consumes(MediaType.APPLICATION_JSON)
  public void register(ClusterDTO cluster) {
    
  }
  
  @POST
  @Path("unregister")
  @Consumes(MediaType.APPLICATION_JSON)
  public void unregister(ClusterDTO cluster) {
    
  }
  
  @PUT
  @Path("register/confirm/{}")
  public void confirmRegister() {
    
  }
  
  @PUT
  @Path("unregister/confirm/{}")
  public void confirmUnregister() {
    
  }
}
