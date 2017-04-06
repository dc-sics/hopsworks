package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.security.AccessControlException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InfluxDBService {
  private static final Logger LOGGER = Logger.getLogger(InfluxDBService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;

  private String appId;

  InfluxDBService setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  @GET
  @Path("/info")
  //@Produces(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response info(
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {

    InfluxDB influxdb = InfluxDBFactory.connect(settings.
            getInfluxDBAddress(), settings.getInfluxDBUser(), settings.
            getInfluxDBPW());

    String version = influxdb.version();

    influxdb.close();

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(version).build();
  }
}
