package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.util.ClusterUtil;
import io.swagger.annotations.Api;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("/clusterUtilisation")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Cluster Utilisation Service", description = "Cluster Utilisation Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterUtilisationService {

  private final static Logger LOGGER = Logger.getLogger(
          ClusterUtilisationService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ClusterUtil clusterUtil;

  @GET
  @Path("/multiplicator")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrentMultiplicator() {
    YarnPriceMultiplicator multiplicator = clusterUtil.getMultiplicator();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            multiplicator).build();
  }

}
