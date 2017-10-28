package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.ProjectPermission;
import io.hops.hopsworks.api.filter.ProjectPermissionLevel;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetDTO;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetHelper;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.dela.cluster.ClusterDatasetController;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/delacluster")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Cluster Service",
  description = "Dela Cluster Service")
public class DelaClusterService {
  private final static Logger LOG = Logger.getLogger(DelaClusterService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ClusterDatasetController datasetCtrl;
  
  @EJB
  private InodeFacade inodes;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ProjectPermission(ProjectPermissionLevel.ANYONE)
  public Response getPublicDatasets(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    List<Dataset> clusterDatasets = datasetCtrl.getPublicDatasets();
    List<LocalDatasetDTO> localDS = LocalDatasetHelper.parse(inodes, clusterDatasets);
    GenericEntity<List<LocalDatasetDTO>> datasets = new GenericEntity<List<LocalDatasetDTO>>(localDS) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasets).build();
  }
}
