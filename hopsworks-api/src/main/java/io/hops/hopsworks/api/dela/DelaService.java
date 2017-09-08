package io.hops.hopsworks.api.dela;

import com.google.gson.Gson;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.RemoteDelaController;
import io.hops.hopsworks.dela.TransferDelaController;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.ErrorDescJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.dela.dto.hopssite.SearchServiceDTO;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksSearchDTO;
import io.hops.hopsworks.dela.hopssite.HopsSiteController;
import io.hops.hopsworks.dela.old_hopssite_dto.PopularDatasetJSON;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

@Path("/dela")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Service",
  description = "Dela Service")
public class DelaService {

  private final static Logger LOG = Logger.getLogger(DelaService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  
  @EJB
  private HopsSiteController hopsSite;
  @EJB
  private ProjectController projectCtrl;
  @EJB
  private TransferDelaController transferDelaCtrl;
  @EJB
  private RemoteDelaController remoteDelaCtrl;
  

  //********************************************************************************************************************
  @GET
  @Path("/search/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response publicSearch(@PathParam("searchTerm") String searchTerm)
    throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:search");
    searchSanityCheck(searchTerm);
    SearchServiceDTO.SearchResult searchResult = hopsSite.search(searchTerm);
    SearchServiceDTO.Item[] pageResult = hopsSite.page(searchResult.getSessionId(), 0, searchResult.getNrHits());
    HopsworksSearchDTO.Item[] parsedResult = parseSearchResult(pageResult);
    String auxResult = new Gson().toJson(parsedResult);
    LOG.log(Settings.DELA_DEBUG, "dela:search:done");
    return success(auxResult);
  }

  private void searchSanityCheck(String searchTerm) throws ThirdPartyException {
    if (searchTerm == null || searchTerm.isEmpty()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "search term",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  private HopsworksSearchDTO.Item[] parseSearchResult(SearchServiceDTO.Item[] items) {
    HopsworksSearchDTO.Item[] result = new HopsworksSearchDTO.Item[items.length];
    for (int i = 0; i < items.length; i++) {
      result[i] = new HopsworksSearchDTO.Item(items[i]);
    }
    return result;
  }
  
  @GET
  @Path("/dataset/{publicDSId}/details")
  @Produces(MediaType.APPLICATION_JSON)
  public Response details(@PathParam("publicDSId")String publicDSId) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:dataset:details {0}", publicDSId);
    SearchServiceDTO.ItemDetails result = hopsSite.details(publicDSId);
    String auxResult = new Gson().toJson(result);
    LOG.log(Settings.DELA_DEBUG, "dela:dataset:details:done {0}", publicDSId);
    return success(auxResult);
  }

  @POST
  @Path("/dataset/{publicDSId}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readme(@PathParam("publicDSId") String publicDSId, String peersJSON) 
    throws ThirdPartyException {
    ClusterAddressDTO[] peers = new Gson().fromJson(peersJSON, ClusterAddressDTO[].class);
    for(ClusterAddressDTO peer : peers) {
      try {
        FilePreviewDTO readme = remoteDelaCtrl.readme(publicDSId, peer);
        return success(readme);
      } catch (ThirdPartyException ex) {
        continue;
      }
    }
    throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "communication fail",
        ThirdPartyException.Source.REMOTE_DELA, "all peers for:" + publicDSId);
  }
  
  
  //********************************************************************************************************************
  @GET
  @Path("/user/contents")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getContentsForUser(@Context SecurityContext sc) throws ThirdPartyException {

    String email = sc.getUserPrincipal().getName();
    List<ProjectTeam> teams = projectCtrl.findProjectByUser(email);
    List<Integer> projectIds = new LinkedList<>();
    for (ProjectTeam t : teams) {
      projectIds.add(t.getProject().getId());
    }

    HopsContentsSummaryJSON.Contents resp = transferDelaCtrl.getContents(projectIds);
    List<UserContentsSummaryJSON> userContents = new ArrayList<>();
    Iterator<Map.Entry<Integer, ElementSummaryJSON[]>> it = resp.getContents().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, ElementSummaryJSON[]> n = it.next();
      userContents.add(new UserContentsSummaryJSON(n.getKey(), n.getValue()));
    }
    GenericEntity<List<UserContentsSummaryJSON>> userContentsList
      = new GenericEntity<List<UserContentsSummaryJSON>>(userContents) {
      };
    return success(userContentsList);
  }
  
  @GET
  @Path("/dataset/popular")
  @Produces(MediaType.APPLICATION_JSON)
  public Response popularDatasets(@Context SecurityContext sc) throws ThirdPartyException {
    List<PopularDatasetJSON> popularDatasets = hopsSite.getPopularDatasets();
    return Response.ok(popularDatasets.toString(), MediaType.APPLICATION_JSON).build();
  }

  private Response success(Object content) {
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(content).build();
  }
  

  private Response errorResponse(String msg) {
    ErrorDescJSON errorDesc = new ErrorDescJSON(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).entity(errorDesc).build();
  }
}
