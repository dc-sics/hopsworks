/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.security.AccessControlException;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api("V2 Blobs")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BlobsResource {
  private final static Logger logger = Logger.getLogger(BlobsResource.class.getName());
  
  
  @EJB
  private PathValidatorV2 pathValidator;
  @EJB
  private UserManager userBean;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @Inject
  private DistributedFsService dfs;
  @Inject
  private DatasetController datasetController;
  
  private Dataset ds;
  
  private Project project;
  
  public void setDataset(Dataset ds){
    this.ds = ds;
  }
  public void setProject(Project project){
    this.project = project;
  }
  
  @ApiOperation("Download a file")
  @GET
  @Path("/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadFile(@PathParam("path") String path,
      @Context SecurityContext sc) throws AppException, AccessControlException {
    if(ds == null){
      throw new AppException(Response.Status.NOT_FOUND, "Data set not found.");
    }
    
    if (ds.isShared() && !ds.isEditable() && !ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DOWNLOAD_ERROR);
    }
    
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String hdfsUserName = hdfsUsersBean.getHdfsUserName(project, user);
    
    DataSetPath dsPath = new DataSetPath(ds, path);
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(dsPath);
    return downloadFromHdfs(hdfsUserName, fullPath);
  }
  
 /* @ApiOperation(value="Upload a large file in parts")
  @POST
  @Path("/{path: .+}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response uploadMethod(@Context SecurityContext sc, @PathParam("path") String relativePath,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file")
          FormDataContentDisposition fileDetail,
      @FormDataParam("flowChunkNumber") String flowChunkNumber,
      @FormDataParam("flowChunkSize") String flowChunkSize,
      @FormDataParam("flowCurrentChunkSize") String flowCurrentChunkSize,
      @FormDataParam("flowFilename") String flowFilename,
      @FormDataParam("flowIdentifier") String flowIdentifier,
      @FormDataParam("flowRelativePath") String flowRelativePath,
      @FormDataParam("flowTotalChunks") String flowTotalChunks,
      @FormDataParam("flowTotalSize") String flowTotalSize,
      @QueryParam("templateId") int templateId)
      throws AppException, IOException {
    
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);
  
    Project owning = datasetController.getOwningProject(ds);
    //Is user a member of this project? If so get their role
    boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
    String role = null;
    if(isMember){
      role = projectTeamFacade.findCurrentRole(owning, user);
    }
    
    DataSetPath dsPath = new DataSetPath(ds, relativePath);
    uploadService.confFileUpload(pathValidator.getFullPath(dsPath).toString(),username, templateId, role);
    
    return uploadService.uploadMethod(uploadedInputStream,fileDetail,flowChunkNumber,flowChunkSize,flowCurrentChunkSize,
        flowFilename,flowIdentifier,flowRelativePath,flowTotalChunks, flowTotalSize);
  }*/
  
  private Response downloadFromHdfs(String projectUsername, org.apache.hadoop.fs.Path fullPath) throws AppException,
      AccessControlException {
    
    FSDataInputStream stream;
    DistributedFileSystemOps udfso;
    try {
      if (projectUsername != null) {
        udfso = dfs.getDfsOps(projectUsername);
        stream = udfso.open(fullPath);
        Response.ResponseBuilder response = Response.ok(buildOutputStream(stream, udfso));
        response.header("Content-disposition", "attachment;");
        return response.build();
      } else {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR,
            "No matching HDFS-user found.");
      }
      
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.NOT_FOUND,
          "File does not exist: " + fullPath);
    }
  }
  
  private StreamingOutput buildOutputStream(final FSDataInputStream stream,
      final DistributedFileSystemOps udfso) {
    StreamingOutput output = new StreamingOutput() {
      @Override
      public void write(OutputStream out) throws IOException,
          WebApplicationException {
        try {
          int length;
          byte[] buffer = new byte[1024];
          while ((length = stream.read(buffer)) != -1) {
            out.write(buffer, 0, length);
          }
          out.flush();
          stream.close();
        } finally {
          dfs.closeDfsClient(udfso);
        }
      }
    };
    
    return output;
  }
  
}
