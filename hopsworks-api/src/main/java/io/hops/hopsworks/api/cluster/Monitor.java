package io.hops.hopsworks.api.cluster;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/kmon")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Monitor Cluster Service")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class Monitor {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private HostsFacade hostEjb;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Path("/services")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllRoles(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findAll();
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/groups/{groupName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServiceRoles(@PathParam("groupName") String groupName, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findGroupServices(groupName);
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/hosts/{hostId}/services")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHostRoles(@PathParam("hostId") String hostId, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findHostRoles(hostId);
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/groups/{groupName}/services/{serviceName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRoles(@PathParam("groupName") String groupName, @PathParam("serviceName") String serviceName,
      @Context SecurityContext sc, @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findServices(groupName, serviceName);
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/hosts")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<Hosts> list = hostEjb.find();
    GenericEntity<List<Hosts>> hosts = new GenericEntity<List<Hosts>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(hosts).build();
  }

  @GET
  @Path("/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@PathParam("hostId") String hostId, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    Hosts h = hostEjb.findByHostname(hostId);
    if (h != null) {
      GenericEntity<Hosts> host = new GenericEntity<Hosts>(h) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(host).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }

  }

  @POST
  @Path("/groups/{groupName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOp(@PathParam("groupName") String groupName, @Context SecurityContext sc,
      @Context HttpServletRequest req, ServicesActionDTO action) throws AppException {
    String result = hostServicesFacade.serviceOp(groupName, action.getAction());
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(result);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("/groups/{groupName}/services/{serviceName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOp(@PathParam("groupName") String groupName, @PathParam("serviceName") String serviceName,
      @Context SecurityContext sc, @Context HttpServletRequest req, ServicesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(hostServicesFacade.serviceOp(groupName, serviceName, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("/groups/{groupName}/services/{serviceName}/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOnHostOp(@PathParam("groupName") String groupName,
      @PathParam("serviceName") String serviceName,
      @PathParam("hostId") String hostId, @Context SecurityContext sc, @Context HttpServletRequest req,
      ServicesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(hostServicesFacade.roleOnHostOp(groupName, serviceName, hostId, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
}
