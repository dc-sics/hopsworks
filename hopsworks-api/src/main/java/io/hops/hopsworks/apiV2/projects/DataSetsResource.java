package io.hops.hopsworks.apiV2.projects;


import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.DataSetService;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.DataSetView;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "V2 Datasets", tags = {"V2 Datasets"})
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataSetsResource {

  private final static Logger logger = Logger.getLogger(DataSetsResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserManager userBean;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private PathValidatorV2 pathValidator;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private InodeFacade inodes;
  @EJB
  private UserFacade userFacade;
  @Inject
  private DataSetService dataSetService;
  @Inject
  private BlobsResource blobsResource;

  private Integer projectId;
  private Project project;

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }
  
  @ApiOperation("Get a list of data sets in project")
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDataSets( @Context SecurityContext sc){
    
    List<DataSetView> dsViews = new ArrayList<>();
    for (Dataset dataset : project.getDatasetCollection()){
      dsViews.add(new DataSetView(dataset));
    }
    
    GenericEntity<List<DataSetView>> result = new GenericEntity<List<DataSetView>>(dsViews) {};
    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation("Create a data set")
  @POST
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response createDataSet(DataSetDTO dataSetDTO,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (username == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "User not found");
    }
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    
    try {
      datasetController.createDataset(user, project, dataSetDTO.getName(),
          dataSetDTO.getDescription(), dataSetDTO.getTemplate(), dataSetDTO.isSearchable(),
          false, dfso); // both are dfso to create it as root user
      
      //Generate README.md for the dataset if the user requested it
      if (dataSetDTO.isGenerateReadme()) {
        //Persist README.md to hdfs
        datasetController.generateReadme(udfso, dataSetDTO.getName(), dataSetDTO.getDescription(),
            project.getName());
      }
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Failed to create dataset: " + e.
          getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    
    GenericEntity<DataSetView> created = new GenericEntity<DataSetView>(new DataSetView(getDataSet(dataSetDTO
        .getName()))){};
    
    UriBuilder builder = UriBuilder.fromResource(DataSetsResource.class);
    URI uri = builder.path(DataSetsResource.class,"/").build(dataSetDTO.getName());
    
    return Response.created(uri).type(MediaType.APPLICATION_JSON_TYPE).entity(created).build();
  }
  
  @ApiOperation("Get data set metadata")
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDataSet(@PathParam("name") String name, @Context
      SecurityContext sc) throws AppException {
    
    Dataset toReturn = getDataSet(name);
    GenericEntity<DataSetView> dsView = new GenericEntity<DataSetView>(new DataSetView(toReturn)){};
    
    return Response.ok(dsView,MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  private Dataset getDataSet(String name) throws AppException {
    Dataset byNameAndProjectId =
        datasetFacade.findByNameAndProjectId(project, name);
    if (byNameAndProjectId == null){
      throw new AppException(Response.Status.NOT_FOUND, "Data set with name" + name + " can not be found.");
    }
    
    return byNameAndProjectId;
  }
  
  @ApiOperation("Update data set metadata")
  @POST
  @Path("/{name}")
  public Response updateDataSet(@PathParam("name") String name, DataSetDTO update, @Context SecurityContext sc)
      throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
  }
  
  /**
   * This function is used only for deletion of dataset directories
   * as it does not accept a path
   * @param name
   * @param sc
   * @param req
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @ApiOperation("Delete data set")
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteDataSet(
      @PathParam("name") String name,
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req) throws AppException,
      AccessControlException {
    
    boolean success = false;
    JsonResponse json = new JsonResponse();
    Dataset dataset = getDataSet(name);
    DataSetPath path = new DataSetPath(dataset, "/");
  
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(path);

    if (dataset.isShared()) {
      // The user is trying to delete a dataset. Drop it from the table
      // But leave it in hopsfs because the user doesn't have the right to delete it
      hdfsUsersBean.unShareDataset(project, dataset);
      datasetFacade.removeDataset(dataset);
      json.setSuccessMessage(ResponseMessages.SHARED_DATASET_REMOVED);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(json).build();
    }
    
    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(dataset);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user)
          .equals(AllowedProjectRoles.DATA_OWNER)
          && owning.equals(project)) {
        dfso = dfs.getDfsOps();// do it as super user
      } else {
        dfso = dfs.getDfsOps(username);// do it as project user
      }
      success = datasetController.
          deleteDatasetDir(dataset, fullPath, dfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not delete the file " + fullPath.toString());
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath.toString());
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath.toString());
    }
    
    //remove the group associated with this dataset as it is a toplevel ds
    try {
      hdfsUsersBean.deleteDatasetGroup(dataset);
    } catch (IOException ex) {
      //FIXME: take an action?
      logger.log(Level.WARNING,
          "Error while trying to delete a dataset group", ex);
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value = "Get data set README-file with meta data")
  @GET
  @Path("/{name}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReadme(@PathParam("name") String datasetName, @Context SecurityContext sc) throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Endpoint not implemented yet.");
  }
  
  @ApiOperation("Get a list of projects that share this data set")
  @GET
  @Path("/{name}/projects")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProjects(
      @PathParam("name") String name,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException,
      AccessControlException {
    Dataset ds = getDataSet(name);
    
    List<Project> list = datasetFacade.findProjectSharedWith(project, ds.getInode());
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(
        list) { };
    
    return Response.ok(projects, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation("Check if data set readonly")
  @GET
  @Path("/{name}/readonly")
  public Response isReadonly(@PathParam("name") String name, @Context SecurityContext sc) throws AppException {
    
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
    //if readonly, return no-content
//    boolean isReadonly = false; //TODO: add logic..
//
//    if (isReadonly){
//      return Response.noContent().build();
//    } else {
//      throw new AppException(Response.Status.NOT_FOUND, "Data set editable");
//    }
  }
  
  @ApiOperation("Make data set readonly")
  @PUT
  @Path("/{name}/readonly")
  public Response makeReadonly(@PathParam("name") String name) throws AppException, AccessControlException {
    Dataset dataSet = getDataSet(name);
    datasetController.changeEditable(dataSet, false);
    
    DistributedFileSystemOps dfso = null;
    try {
      // change the permissions as superuser
      dfso = dfs.getDfsOps();
      FsPermission fsPermission = new FsPermission(FsAction.ALL,
          FsAction.READ_EXECUTE,
          FsAction.NONE, false);
      datasetController.recChangeOwnershipAndPermission(
          datasetController.getDatasetPath(dataSet),
          fsPermission, null, null, null, dfso);
      datasetController.changeEditable(dataSet, false);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: Can not change the permission of this file.");
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Error while creating directory: " + e.
          getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    return Response.noContent().build();
  }
  
  @ApiOperation("Make data set editable")
  @DELETE
  @Path("/{name}/readonly")
  public Response makeEditable(@PathParam("name") String name) throws AppException, AccessControlException {
    Dataset dataSet = getDataSet(name);
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      // Change permission as super user
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
          FsAction.NONE, true);
      datasetController.recChangeOwnershipAndPermission(
          datasetController.getDatasetPath(dataSet),
          fsPermission, null, null, null, dfso);
      datasetController.changeEditable(dataSet, true);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: Can not change the permission of this file.");
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Error while creating directory: " + e.
          getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    
    return Response.noContent().build();
  }
  
  
  //File operations
  @ApiOperation("Get data set file/dir listing")
  @GET
  @Path("/{name}/files")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDatasetRoot(@PathParam("name") String name, @Context SecurityContext sc) throws AppException {
    Dataset dataset = getDataSet(name);
    DataSetPath path = new DataSetPath(dataset, "/");
  
    String fullPath = pathValidator.getFullPath(path).toString();
  
    Inode inode = pathValidator.exists(path, inodes, true);
  
    GenericEntity<List<InodeView>> entity =
        getDir(inode, fullPath, dataset.isShared());
    return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation("Get a listing for a path in a data set")
  @GET
  @Path("/{name}/files/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getFileOrDir(@PathParam("name") String name,
      @PathParam ("path") String relativePath,
      @Context SecurityContext sc ) throws
      AppException, AccessControlException {
    
    Dataset dataSet = getDataSet(name);
    DataSetPath path = new DataSetPath(dataSet, relativePath);
    String fullPath = pathValidator.getFullPath(path).toString();
    
    Inode inode = pathValidator.exists(path, inodes, null);
    
    
    if (inode.isDir()){
      GenericEntity<List<InodeView>> entity = getDir(inode, fullPath, dataSet.isShared());
      return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
    } else {
      GenericEntity<InodeView> entity = getFile(inode, fullPath);
      return Response.ok(entity,MediaType.APPLICATION_JSON_TYPE).build();
    }
  }
  
  @ApiOperation("Delete a file or directory")
  @DELETE
  @Path("/{name}/files/{path: .+}")
  public Response deleteFileOrDir(@PathParam("name") String dataSetName, @PathParam("path") String path, @Context
      SecurityContext sc, @Context HttpServletRequest req) throws AccessControlException, AppException {
    return dataSetService.removefile(dataSetName + "/" + path, sc, req);
  }
  
  @ApiOperation(value = "Copy, Move, Zip or Unzip", notes = "Performs the selected operation on the file/dir " +
      "specified in the src parameter. All operations are data set scoped. ")
  @PUT
  @Path("/{name}/files/{target: .+}")
  public Response copyMovePutZipUnzip(@PathParam("name") String dataSetName, @PathParam("target") String target,
      @ApiParam(allowableValues = "copy,move,zip,unzip") @QueryParam("op") String operation, @QueryParam("src")
      String sourcePath ) throws AppException {
    if (operation == null){
      throw new AppException(Response.Status.BAD_REQUEST, "?op= parameter required, possible options: " +
          "copy|move|zip|unzip");
    }
    switch(operation){
      case "copy":
        return copy(dataSetName, target, sourcePath);
      case "move":
        return move(dataSetName, target, sourcePath);
      case "zip":
        return zip(dataSetName, target, sourcePath);
      case "unzip":
        return unzip(dataSetName, target, sourcePath);
      default:
        throw new AppException(Response.Status.BAD_REQUEST, "?op= parameter should be one of: copy|move|zip|unzip");
    }
  }
  
  private Response copy(String dataSet, String targetPath, String sourcePath) throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
  }
  
  private Response move(String dataSet, String targetPath, String sourcePath) throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
  }
  
  private Response zip(String dataSet, String targetPath, String sourcePath) throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
  }
  
  private Response unzip(String dataSet, String targetPath, String sourcePath) throws AppException {
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.");
  }
  
  @Path("/{name}/blobs")
  public BlobsResource blobs(@PathParam("name") String dataSetName, @Context SecurityContext sc) throws AppException {
    Dataset ds = getDataSet(dataSetName);
    this.blobsResource.setProject(project);
    this.blobsResource.setDataset(ds);
    return this.blobsResource;
  }
  
  private GenericEntity<InodeView> getFile(Inode inode, String path){
    InodeView inodeView = new InodeView(inode, path+ "/" + inode.getInodePK().
        getName());
    //inodeView.setUnzippingState(settings.getUnzippingState(
    //    path+ "/" + inode.getInodePK().getName()));
    Users user = userFacade.findByUsername(inodeView.getOwner());
    if (user != null) {
      inodeView.setOwner(user.getFname() + " " + user.getLname());
      inodeView.setEmail(user.getEmail());
    }
    
    return new GenericEntity<InodeView>(inodeView) { };
  }
  
  private GenericEntity<List<InodeView>> getDir(Inode inode,
      String path, boolean isShared){
    List<Inode> cwdChildren = inodes.getChildren(inode);
    
    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      InodeView inodeView = new InodeView(i, path + "/" + i.getInodePK()
          .getName());
      if (isShared) {
        //Get project of project__user the inode is owned by
        inodeView.setOwningProjectName(hdfsUsersBean.getProjectName(i.getHdfsUser().getName()));
      }
      //inodeView.setUnzippingState(settings.getUnzippingState(
      //    path + "/" + i.getInodePK().getName()));
      Users user = userFacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      kids.add(inodeView);
    }
    return new GenericEntity<List<InodeView>>(kids) { };
  }
  
  
}
