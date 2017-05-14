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
  public Response getSparkMetrics(
          @QueryParam("startTime") String startTime,
          @QueryParam("fields") List<String> fields,
          @QueryParam("service") String service,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {
    // e.g. /spark?fields=total_used,heap_used&startTime=1491827969000&service=driver
    InfluxDB influxdb = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
            settings.getInfluxDBUser(), settings.getInfluxDBPW());
    Response httpResponse = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + String.join(",", fields) + " from spark ");
    query.append("where appid=\'" + this.appId + "\' ");
    query.append("and service =~ /.*" + service + ".*/ ");
    query.append("and time > " + startTime);
    query.append("ms");

    LOGGER.log(Level.INFO, "Influxdb:Spark - Sending query: " + query.toString());

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

  @GET
  @Path("/tgcpu")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getTelegrafMetrics(
          @QueryParam("fields") List<String> fields,
          @QueryParam("host") String host,
          @QueryParam("startTime") String startTime,
          @QueryParam("endTime") String endTime,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {
    // Query the CPU measurement by using hostname and time from any table on telegraf database
    // e.g. /tgcpu?fields=usage_user,usage_idle,usage_iowait&startTime=1491827969000&host=vagrant
    //      &endTime=1491827984000
    InfluxDB influxdb = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
            settings.getInfluxDBUser(), settings.getInfluxDBPW());
    Response httpResponse = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + String.join(",", fields) + " from cpu ");
    query.append("where host=\'" + host + "\' ");
    query.append("and cpu=\'cpu-total\' ");
    query.append("and time > " + startTime + "ms ");
    query.append("and time < " + endTime + "ms");

    LOGGER.log(Level.INFO, "Influxdb:telegraf:cpu-Sending query: " + query.toString());

    Query q = new Query(query.toString(), "telegraf");
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

  @GET
  @Path("/nodemanager")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getNodemanagerMetrics(
          @QueryParam("fields") List<String> fields,
          @QueryParam("container") String container,
          @QueryParam("startTime") String startTime,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException, AccessControlException {
    // e.g. /nodemanager?fields=MilliVcoreUsageIMinMilliVcores&startTime=1491827969000&service=driver
    InfluxDB influxdb = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
            settings.getInfluxDBUser(), settings.getInfluxDBPW());
    Response httpResponse = null;

    StringBuffer query = new StringBuffer();
    query.append("select " + String.join(",", fields) + " from nodemanager ");
    query.append("where source =~ /.*" + container + ".*/ ");
    query.append("and time > " + startTime);
    query.append("ms");

    LOGGER.log(Level.INFO, "Influxdb:nodemanager - Sending query: " + query.toString());

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
