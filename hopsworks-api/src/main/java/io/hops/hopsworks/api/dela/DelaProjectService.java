package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaHdfsController;
import io.hops.hopsworks.dela.DelaWorkerController;
import io.hops.hopsworks.dela.TransferDelaController;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.DetailsRequestDTO;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import io.hops.hopsworks.dela.old_dto.SuccessJSON;
import io.hops.hopsworks.dela.old_dto.TorrentExtendedStatusJSON;
import io.hops.hopsworks.dela.old_dto.TorrentId;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksTransferDTO;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaProjectService {

  private final static Logger logger = Logger.getLogger(DelaProjectService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private DelaWorkerController delaWorkerCtrl;
  @EJB
  private TransferDelaController delaCtrl;
  @EJB 
  private DelaHdfsController delaHdfsCtrl;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private UserManager userBean;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodeFacade;

  private Project project;
  private Integer projectId;

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }
  
  private Response successResponse(Object content) {
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(content).build();
  }

  @POST
  @Path("/dataset/publish/inodeId/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response publish(@PathParam("inodeId") Integer inodeId, @Context SecurityContext sc)
    throws ThirdPartyException {
    Inode inode = getInode(inodeId);
    Dataset dataset = getDatasetByInode(inode);
    Users user = getUser(sc.getUserPrincipal().getName());
    delaWorkerCtrl.publishDataset(project, dataset, user);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is started - published");
    return successResponse(json);
  }

  @POST
  @Path("/dataset/cancel/inodeId/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeByInodeId(@PathParam("inodeId") Integer inodeId, @Context SecurityContext sc)
    throws ThirdPartyException {
    Inode inode = getInode(inodeId);
    Dataset dataset = getDatasetByInode(inode);
    Users user = getUser(sc.getUserPrincipal().getName());
    delaWorkerCtrl.cancel(project, dataset, user);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is now stopped - cancelled");
    return successResponse(json);
  }

  @POST
  @Path("/dataset/cancel/publicDSId/{publicDSId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removePublic(@PathParam("publicDSId") String publicId, @Context SecurityContext sc)
    throws ThirdPartyException {
    Dataset dataset = getDatasetByPublicId(publicId);
    Users user = getUser(sc.getUserPrincipal().getName());
    delaWorkerCtrl.cancel(project, dataset, user);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is now stopped - cancelled");
    return successResponse(json);
  }
  
  @POST
  @Path("/dataset/cancelclean/publicDSId/{publicDSId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeCancel(@PathParam("publicDSId") String publicId, @Context SecurityContext sc)
    throws ThirdPartyException {
    Dataset dataset = getDatasetByPublicId(publicId);
    Users user = getUser(sc.getUserPrincipal().getName());
    delaWorkerCtrl.cancelAndClean(project, dataset, user);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is now stopped - cancelled");
    return successResponse(json);
  }

  @GET
  @Path("/dataset/manifest/inodeId/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response showManifest(@PathParam("inodeId") Integer inodeId, @Context SecurityContext sc)
    throws ThirdPartyException {
    JsonResponse json = new JsonResponse();
    Inode inode = getInode(inodeId);
    Dataset dataset = getDatasetByInode(inode);
    Users user = getUser(sc.getUserPrincipal().getName());
    if (!dataset.isPublicDs()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset not public - no manifest",
        ThirdPartyException.Source.LOCAL, "bad request");
    }

    ManifestJSON manifestJSON = delaHdfsCtrl.readManifest(project, dataset, user);
    return successResponse(manifestJSON);
  }

  @PUT
  @Path("/dataset/download/start")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response startDownload(@Context SecurityContext sc, HopsworksTransferDTO.Download downloadDTO)
    throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    //dataset not createed yet

    ManifestJSON manifest = delaWorkerCtrl.startDownload(project, user, downloadDTO);
    return successResponse(manifest);
  }

  @PUT
  @Path("/dataset/download/hdfs")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response downloadDatasetHdfs(@Context SecurityContext sc, HopsworksTransferDTO.Download downloadDTO) 
    throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    Dataset dataset = getDatasetByPublicId(downloadDTO.getPublicDSId());

    delaWorkerCtrl.advanceDownload(project, dataset, user, downloadDTO, null, null);
    return successResponse(new SuccessJSON(""));
  }

  @PUT
  @Path("/dataset/download/kafka")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response downloadDatasetKafka(@Context SecurityContext sc, @Context HttpServletRequest req,
    HopsworksTransferDTO.Download downloadDTO) throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    Dataset dataset = getDatasetByPublicId(downloadDTO.getPublicDSId());

    String certPath = kafkaController.getKafkaCertPaths(project);
    String brokerEndpoint = settings.getKafkaConnectStr();
    String restEndpoint = settings.getKafkaRestEndpoint();
    String keyStore = certPath + "/keystore.jks";
    String trustStore = certPath + "/truststore.jks";
    KafkaEndpoint kafkaEndpoint = new KafkaEndpoint(brokerEndpoint, restEndpoint, settings.getDELA_DOMAIN(),
      "" + project.getId(), keyStore, trustStore);

    delaWorkerCtrl.advanceDownload(project, dataset, user, downloadDTO, req.getSession().getId(),
      kafkaEndpoint);
    return successResponse(new SuccessJSON(""));
  }

  @GET
  @Path("contents")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getProjectContents(@Context SecurityContext sc) throws ThirdPartyException {

    List<Integer> projectIds = new LinkedList<>();
    projectIds.add(projectId);

    HopsContentsSummaryJSON.Contents resp = delaCtrl.getContents(projectIds);
    ElementSummaryJSON[] projectContents = resp.getContents().get(projectId);
    if (projectContents == null) {
      projectContents = new ElementSummaryJSON[0];
    }
    return successResponse(projectContents);
  }

  @PUT
  @Path("details")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getExtendedDetails(@Context SecurityContext sc, DetailsRequestDTO detailsRequestDTO) 
    throws ThirdPartyException {

    TorrentId torrentId = new TorrentId(detailsRequestDTO.getTorrentId());
    TorrentExtendedStatusJSON resp = delaCtrl.details(torrentId);
    return successResponse(resp);
  }

  private Users getUser(String email) throws ThirdPartyException {
    Users user = userBean.getUserByEmail(email);
    if (user == null) {
      throw new ThirdPartyException(Response.Status.FORBIDDEN.getStatusCode(), "user not found",
        ThirdPartyException.Source.LOCAL, "exception");
    }
    return user;
  }

  private Dataset getDatasetByPublicId(String publicDSId) throws ThirdPartyException {
    Optional<Dataset> d = datasetFacade.findByPublicDsIdProject(publicDSId, project);
    if (!d.isPresent()) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(),
        "dataset by publicId and project", ThirdPartyException.Source.MYSQL, "not found");
    }
    return d.get();
  }

  private Dataset getDatasetByInode(Inode inode) throws ThirdPartyException {
    Dataset dataset = datasetFacade.findByProjectAndInode(this.project, inode);
    if (dataset == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset not found",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
    return dataset;
  }

  private Inode getInode(Integer inodeId) throws ThirdPartyException {
    if (inodeId == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "inode not found",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
    Inode inode = inodeFacade.findById(inodeId);
    if (inode == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "inode not found",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
    return inode;
  }

}
