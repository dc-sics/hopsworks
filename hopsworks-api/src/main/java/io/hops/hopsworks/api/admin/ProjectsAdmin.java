package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.admin.dto.ProjectDeletionLog;
import io.hops.hopsworks.api.admin.dto.ProjectAdminInfoDTO;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/admin")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsAdmin {
  private final Logger LOG = Logger.getLogger(ProjectsAdmin.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/{id}")
  public Response deleteProject(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("id") Integer id) throws AppException {
    Project project = projectFacade.find(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    
    String sessionId = req.getSession().getId();
    projectController.removeProject(project.getOwner().getEmail(), id, sessionId);
    LOG.log(Level.INFO, "Deleted project with id: " + id);
  
    JsonResponse response = new JsonResponse();
    response.setStatus(Response.Status.OK.toString());
    response.setSuccessMessage("Project with id " + id + " has been successfully deleted");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/createas")
  public Response createProjectAsUser(@Context SecurityContext sc, @Context HttpServletRequest request,
      ProjectDTO projectDTO) throws AppException {
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    if (user == null || !user.getEmail().equals(Settings.SITE_EMAIL)) {
      LOG.log(Level.WARNING, "Unauthorized or unknown user tried to create a Project as another user");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
          ResponseMessages.AUTHENTICATION_FAILURE);
    }

    String ownerEmail = projectDTO.getOwner();
    if (ownerEmail == null) {
      LOG.log(Level.WARNING, "Owner username is null");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Owner email cannot be null");
    }

    Users owner = userFacade.findByEmail(ownerEmail);
    if (owner == null) {
      LOG.log(Level.WARNING, "Owner is not in the database");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Unknown owner user " + ownerEmail);
    }

    List<String> failedMembers = null;
    projectController.createProject(projectDTO, owner, failedMembers, request.getSession().getId());

    JsonResponse response = new JsonResponse();
    response.setStatus("201");
    response.setSuccessMessage(ResponseMessages.PROJECT_CREATED);

    if (failedMembers != null && !failedMembers.isEmpty()) {
      response.setFieldErrors(failedMembers);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED)
        .entity(response).build();
  }

  /**
   * Returns admin information about all the projects
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects")
  public Response getProjectsAdminInfo(@Context SecurityContext sc, @Context HttpServletRequest req) {

    List<Project> projects = projectFacade.findAll();
    List<ProjectAdminInfoDTO> projectAdminInfoDTOList = new ArrayList<>();
    for (Project project : projects) {
      projectAdminInfoDTOList.add(new ProjectAdminInfoDTO(project,
          projectController.getQuotasInternal(project)));
    }

    GenericEntity<List<ProjectAdminInfoDTO>> projectsEntity =
        new GenericEntity<List<ProjectAdminInfoDTO>>(projectAdminInfoDTOList) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectsEntity).build();
  }

  /**
   * Returns admin information about the requested project
   * @param sc
   * @param req
   * @param projectId
   * @return
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/{id}")
  public Response getProjectAdminInfo(@Context SecurityContext sc, @Context HttpServletRequest req,
                                      @PathParam("id") Integer projectId) throws AppException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    ProjectAdminInfoDTO projectAdminInfoDTO = new ProjectAdminInfoDTO(project,
        projectController.getQuotasInternal(project));

    GenericEntity<ProjectAdminInfoDTO> projectEntity =
        new GenericEntity<ProjectAdminInfoDTO>(projectAdminInfoDTO) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectEntity).build();
  }


  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/projects")
  public Response setProjectAdminInfo (@Context SecurityContext sc, @Context HttpServletRequest req,
                                       ProjectAdminInfoDTO projectAdminInfoDTO) throws AppException {
    // for changes in space quotas we need to check that both space and ns options are not null
    QuotasDTO quotasDTO = projectAdminInfoDTO.getProjectQuotas();
    if (quotasDTO != null &&
        (((quotasDTO.getHdfsQuotaInBytes() == null) != (quotasDTO.getHdfsNsQuota() == null)) ||
        ((quotasDTO.getHiveHdfsQuotaInBytes() == null) != (quotasDTO.getHiveHdfsNsQuota() == null)))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.QUOTA_REQUEST_NOT_COMPLETE);
    }

    // Build the new project state as Project object
    Project project = new Project();
    project.setName(projectAdminInfoDTO.getProjectName());
    project.setArchived(projectAdminInfoDTO.getArchived());
    project.setPaymentType(projectAdminInfoDTO.getPaymentType());

    projectController.adminProjectUpdate(project, quotasDTO);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
  
  @DELETE
  @Path("/projects/{name}/force")
  @Produces(MediaType.APPLICATION_JSON)
  public Response forceDeleteProject(@Context SecurityContext sc, @Context HttpServletRequest request,
      @PathParam("name") String projectName) throws AppException {
    String userEmail = sc.getUserPrincipal().getName();
    String[] logs = projectController.forceCleanup(projectName, userEmail, request.getSession().getId());
    ProjectDeletionLog deletionLog = new ProjectDeletionLog(logs[0], logs[1]);
  
    GenericEntity<ProjectDeletionLog> response = new GenericEntity<ProjectDeletionLog>(deletionLog) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
}
