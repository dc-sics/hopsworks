package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.security.AccessControlException;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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
  @Path("/spark")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response info(
          @QueryParam("startTime") String startTime,
          @QueryParam("fields") List<String> fields,
          @QueryParam("service") String service,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {
    // e.g. /spark?fields=total_used,heap_used&startTime=1491827969000&service=driver
    InfluxDB influxdb = InfluxDBFactory.connect(settings.
            getInfluxDBAddress(), settings.getInfluxDBUser(), settings.
            getInfluxDBPW());
    Response httpResponse = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + String.join(",", fields) + " from spark ");
    query.append("where appid=\'" + this.appId + "\' ");
    query.append("and service=\'" + service + "\' ");
    query.append("and time > " + startTime);
    query.append("ms");

    LOGGER.log(Level.INFO, "Sending query: " + query.toString());

    Query q = new Query(query.toString(), "graphite");
    QueryResult response = influxdb.query(q, TimeUnit.MILLISECONDS);
    List<QueryResult.Result> results = response.getResults();

    if (results.get(0).getSeries() != null) {
      QueryResult.Series series = results.get(0).getSeries().get(0);
      InfluxDBResultDTO influxResults = new InfluxDBResultDTO();
      influxResults.setSeries(series);

      httpResponse = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(influxResults).build();
    } else {
      httpResponse = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).
              entity("").build();
    }

    influxdb.close();

    return httpResponse;
  }
}
