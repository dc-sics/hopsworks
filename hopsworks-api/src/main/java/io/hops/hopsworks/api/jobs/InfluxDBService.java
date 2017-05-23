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
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
  @Path("/{database}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getMetrics(
          @PathParam("database") String database,
          @QueryParam("columns") String columns,
          @QueryParam("measurement") String measurement,
          @QueryParam("tags") String tags,
          @QueryParam("groupBy") String groupBy,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {

    // TODO: FIX authentication, check if user has access to project
    // https://github.com/influxdata/influxdb-java/blob/master/src/main/java/org/influxdb/dto/QueryResult.java

    InfluxDB influxdb = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
            settings.getInfluxDBUser(), settings.getInfluxDBPW());
    Response response = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + columns + " from " + measurement);
    query.append(" where " + tags);
    if (groupBy != null) query.append(" group by " + groupBy);

    LOGGER.log(Level.INFO, "Influxdb - Running query: " + query.toString());

    Query q = new Query(query.toString(), database);
    QueryResult reply = influxdb.query(q, TimeUnit.MILLISECONDS);

    if (reply.hasError()) {
      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).
              entity(reply.getError()).build();
    } else if (reply.getResults().get(0).getSeries() != null) {
      InfluxDBResultDTO influxResults = new InfluxDBResultDTO();
      influxResults.setQuery(query.toString());
      influxResults.setResult(reply);

      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(influxResults).build();
    } else {
      response = noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).
              entity("").build();
    }

    influxdb.close();

    return response;
  }
}
