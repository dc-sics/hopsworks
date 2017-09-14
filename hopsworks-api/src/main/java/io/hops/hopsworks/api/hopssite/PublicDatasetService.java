package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.CategoryDTO;
import io.hops.hopsworks.api.hopssite.dto.DatasetIssueReqDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.dto.common.UserDTO;
import io.hops.hopsworks.dela.dto.hopssite.DatasetDTO;
import io.hops.hopsworks.dela.dto.hopssite.HopsSiteDatasetDTO;
import io.hops.hopsworks.dela.hopssite.HopsSiteController;
import io.hops.hopsworks.dela.old_hopssite_dto.DatasetIssueDTO;
import io.swagger.annotations.Api;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
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

@Path("/hopssite/publicDataset")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Service",
        description = "PublicDataset Service")
public class PublicDatasetService {

  private final static Logger LOGGER = Logger.getLogger(PublicDatasetService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private HopsSiteController hopsSite;
  @Inject
  private CommentService commentService;
  @Inject
  private RatingService ratingService;

  @GET
  @Path("all")
  public Response getAllPublicDatasets() {
    List<HopsSiteDatasetDTO> datasets = hopsSite.getAll();
    GenericEntity<List<HopsSiteDatasetDTO>> datasetsJson = new GenericEntity<List<HopsSiteDatasetDTO>>(datasets) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all datasets");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasetsJson).build();
  }

  @GET
  @Path("{publicDSId}")
  public Response getPublicDataset(@PathParam("publicDSId") String publicDSId) {
    DatasetDTO.Complete datasets = hopsSite.getDataset(publicDSId);
    LOGGER.log(Settings.DELA_DEBUG, "Get a dataset");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasets).build();
  }

  @GET
  @Path("topRated")
  public Response getTopTenPublicDatasets() {
    List<HopsSiteDatasetDTO> datasets = hopsSite.getAll();
    GenericEntity<List<HopsSiteDatasetDTO>> datasetsJson = new GenericEntity<List<HopsSiteDatasetDTO>>(datasets) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all top rated datasets");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasetsJson).build();
  }

  @GET
  @Path("new")
  public Response getNewPublicDatasets() {
    List<HopsSiteDatasetDTO> datasets = hopsSite.getAll();
    GenericEntity<List<HopsSiteDatasetDTO>> datasetsJson = new GenericEntity<List<HopsSiteDatasetDTO>>(datasets) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all top rated datasets");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasetsJson).build();
  }

  @GET
  @Path("displayCategories")
  public Response getDisplayCategories() {
    CategoryDTO categoryAll = new CategoryDTO("all", "All", false);
    CategoryDTO categoryNew = new CategoryDTO("new", "Recently added", false);
    CategoryDTO categoryTopRated = new CategoryDTO("topRated", "Top Rated", false);
    List<CategoryDTO> categories = Arrays.asList(categoryAll, categoryNew, categoryTopRated);
    GenericEntity<List<CategoryDTO>> categoriesEntity = new GenericEntity<List<CategoryDTO>>(categories) {
    };
    LOGGER.log(Settings.DELA_DEBUG, "Get all display categories.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(categoriesEntity).build();
  }

  @GET
  @Path("categories")
  public Response getAllCategories() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @GET
  @Path("byCategory/{category}")
  public Response getPublicDatasetsByCategory(@PathParam("category") String category) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @POST
  @Path("{publicDSId}/issue")
  public Response addDatasetIssue(@PathParam("publicDSId") String publicDSId, DatasetIssueReqDTO datasetIssueReq,
          @Context SecurityContext sc) throws ThirdPartyException {
    if (datasetIssueReq == null) {
      throw new IllegalArgumentException("Dataset issue not set.");
    }
    UserDTO.Complete user = hopsSite.getUser(sc.getUserPrincipal().getName());
    DatasetIssueDTO datasetIssue = new DatasetIssueDTO(publicDSId, user, datasetIssueReq.getType(),
            datasetIssueReq.getMsg());
    boolean added = hopsSite.addDatasetIssue(datasetIssue);
    if (added) {
      LOGGER.log(Settings.DELA_DEBUG, "Added issue for dataset {0}", publicDSId);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).build();
  }

  @GET
  @Path("role")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRole() throws ThirdPartyException {
    String role = hopsSite.getRole();
    LOGGER.log(Level.INFO, "Cluster role on hops-site: {0}", role);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(role).build();
  }

  @GET
  @Path("userId")
  public Response getUserId(@Context SecurityContext sc) throws ThirdPartyException {
    Integer id = 1;//hopsSite.getUserId(sc.getUserPrincipal().getName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(""+id).build();
  }

  @GET
  @Path("user")
  public Response getUser(@Context SecurityContext sc) throws ThirdPartyException {
    UserDTO.Complete user = hopsSite.getUser(sc.getUserPrincipal().getName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(user).build();
  }

  @Path("{publicDSId}/comments")
  public CommentService getComments(@PathParam("publicDSId") String publicDSId) {
    this.commentService.setPublicDSId(publicDSId);
    return this.commentService;
  }

  @Path("{publicDSId}/rating")
  public RatingService getRating(@PathParam("publicDSId") String publicDSId) {
    this.ratingService.setPublicDSId(publicDSId);
    return this.ratingService;
  }

}
