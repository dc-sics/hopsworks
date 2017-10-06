package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.api.dela.DelaProjectService;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.TourProjectType;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import javax.xml.rpc.ServiceException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/v2/projects")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "V2 Projects")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsResource {
  
  private final static Logger logger = Logger.getLogger(ProjectsResource.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserManager userManager;
  @EJB
  private UsersController usersController;
  @EJB
  private DatasetController datasetController;
  
  // SUB-RESOURCES
  @Inject
  private MembersResource members;
  @Inject
  private DataSetsResource dataSets;
  @Inject
  private DelaProjectService delaService;
  
  @ApiOperation(value= "Get a list of projects")
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getProjects(@Context SecurityContext sc, @Context
      HttpServletRequest req){
    
    if(sc.isUserInRole("HOPS_ADMIN")){
      //Create full project views for admins
      List<ProjectView> projectViews = new ArrayList<>();
      for (Project project : projectFacade.findAll()){
        projectViews.add(new ProjectView(project));
      }
      GenericEntity<List<ProjectView>> projects = new GenericEntity<List<ProjectView>>(projectViews){};
      return Response.ok(projects,MediaType.APPLICATION_JSON_TYPE).build();
    } else {
      //Create limited project views for everyone else
      List<LimitedProjectView> limitedProjectViews = new ArrayList<>();
      for (Project project : projectFacade.findAll()) {
        limitedProjectViews.add(new LimitedProjectView(project));
      }
      GenericEntity<List<LimitedProjectView>> projects =
          new GenericEntity<List<LimitedProjectView>>(limitedProjectViews){};
      return Response.ok(projects, MediaType.APPLICATION_JSON_TYPE).build();
    }
  }
  
  
  @ApiOperation(value= "Create a project")
  @POST
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createProject(
      ProjectDTO projectDTO,
      @Context SecurityContext sc,
      @Context HttpServletRequest req, @QueryParam("template") String starterType) throws AppException {
    
    if (starterType != null){
      return createStarterProject(starterType, sc, req);
    }
    
    //check the user
    String owner = sc.getUserPrincipal().getName();
    Users user = userManager.getUserByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    
    List<String> failedMembers = new ArrayList<>();
    Project project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
  
    JsonResponse json = new JsonResponse();
    json.setStatus("201");// Created
    json.setSuccessMessage(ResponseMessages.PROJECT_CREATED);
    
    if (!failedMembers.isEmpty()) {
      json.setFieldErrors(failedMembers);
    }
  
    URI uri = UriBuilder.fromResource(ProjectsResource.class).path("{id}").build(project.getId());
    logger.info("Created uri: " + uri.toString());
  
    return Response.created(uri).entity(json).build();
  }
  
  private void populateActiveServices(List<String> projectServices,
      TourProjectType tourType) {
    for (ProjectServiceEnum service : tourType.getActiveServices()) {
      projectServices.add(service.name());
    }
  }
  

  private Response createStarterProject(
      @QueryParam("template") String type,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
  
    ProjectDTO projectDTO = new ProjectDTO();
    Project project = null;
    projectDTO.setDescription("A demo project for getting started with " + type);
  
    String owner = sc.getUserPrincipal().getName();
    String username = usersController.generateUsername(owner);
    List<String> projectServices = new ArrayList<>();
    Users user = userManager.getUserByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    //save the project
    List<String> failedMembers = new ArrayList<>();
  
    TourProjectType demoType;
    String readMeMessage;
    if (TourProjectType.SPARK.getTourName().equalsIgnoreCase(type)) {
      // It's a Spark guide
      demoType = TourProjectType.SPARK;
      projectDTO.setProjectName("demo_" + TourProjectType.SPARK.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.SPARK);
      readMeMessage = "jar file to demonstrate the creation of a spark batch job";
    } else if (TourProjectType.KAFKA.getTourName().equalsIgnoreCase(type)) {
      // It's a Kafka guide
      demoType = TourProjectType.KAFKA;
      projectDTO.setProjectName("demo_" + TourProjectType.KAFKA.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.KAFKA);
      readMeMessage = "jar file to demonstrate Kafka streaming";
    } else if (TourProjectType.DISTRIBUTED_TENSORFLOW.getTourName().replace("_", " ").equalsIgnoreCase(type)) {
      // It's a Distributed TensorFlow guide
      demoType = TourProjectType.DISTRIBUTED_TENSORFLOW;
      projectDTO.setProjectName("demo_" + TourProjectType.DISTRIBUTED_TENSORFLOW.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.DISTRIBUTED_TENSORFLOW);
      readMeMessage = "Mnist data to demonstrate the creation of a distributed TensorFlow job";
    } else if (TourProjectType.TENSORFLOW.getTourName().equalsIgnoreCase(type)) {
      // It's a TensorFlow guide
      demoType = TourProjectType.TENSORFLOW;
      projectDTO.setProjectName("demo_" + TourProjectType.TENSORFLOW.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.TENSORFLOW);
      readMeMessage = "Mnist data and python files to demonstrate running TensorFlow noteooks";
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.STARTER_PROJECT_BAD_REQUEST);
    }
    projectDTO.setServices(projectServices);
  
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
      dfso = dfs.getDfsOps();
      username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      projectController.addTourFilesToProject(owner, project, dfso, dfso, demoType);
      //TestJob dataset
      datasetController.generateReadme(udfso, "TestJob", readMeMessage, project.getName());
      //Activate Anaconda and install numppy
      //      if (TourProjectType.TENSORFLOW.getTourName().equalsIgnoreCase(type)){
      //        projectController.initAnacondaForTFDemo(project, req.getSession().getId());
      //      }
    } catch (Exception ex) {
      projectController.cleanup(project, req.getSession().getId());
      throw ex;
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    URI uri = UriBuilder.fromResource(ProjectsResource.class).path("{id}").build(project.getId());
    logger.info("Created uri: " + uri.toString());
  
    return Response.created(uri).entity(project).build();
  }
 
  @ApiOperation(value = "Get project metadata")
  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getProject(@PathParam("id") Integer id, @Context SecurityContext sc) throws AppException {
    Project project = projectController.findProjectById(id);
    return Response.ok(project,MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value= "Update project metadata")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  public Response updateProject(ProjectDTO update, @PathParam("id") Integer id, @Context SecurityContext sc)
      throws AppException {
    JsonResponse json = new JsonResponse();
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userManager.getUserByEmail(userEmail);
    Project project = projectController.findProjectById(id);
  
    boolean updated = false;
  
    if (projectController.updateProjectDescription(project,
        update.getDescription(), user)){
      json.setSuccessMessage(ResponseMessages.PROJECT_DESCRIPTION_CHANGED);
      updated = true;
    }
  
    if (projectController.updateProjectRetention(project,
        update.getRetentionPeriod(), user)){
      json.setSuccessMessage(json.getSuccessMessage() + "\n" +
          ResponseMessages.PROJECT_RETENTON_CHANGED);
      updated = true;
    }
  
    if (!update.getServices().isEmpty()) {
      // Create dfso here and pass them to the different controllers
      DistributedFileSystemOps dfso = dfs.getDfsOps();
      DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersBean.getHdfsUserName(project, user));
    
      for (String s : update.getServices()) {
        ProjectServiceEnum se = null;
        try {
          se = ProjectServiceEnum.valueOf(s.toUpperCase());
          if (projectController.addService(project, se, user, dfso, udfso)) {
            // Service successfully enabled
            json.setSuccessMessage(json.getSuccessMessage() + "\n"
                + ResponseMessages.PROJECT_SERVICE_ADDED
                + s
            );
            updated = true;
          }
        } catch (IllegalArgumentException iex) {
          logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_SERVICE_NOT_FOUND);
          json.setErrorMsg(s + ResponseMessages.PROJECT_SERVICE_NOT_FOUND + "\n "
              + json.getErrorMsg());
        } catch (ServiceException sex) {
          // Error enabling the service
          String error;
          switch (se) {
            case ZEPPELIN:
              error = ResponseMessages.ZEPPELIN_ADD_FAILURE + Settings.ServiceDataset.ZEPPELIN.getName();
              break;
            case JUPYTER:
              error = ResponseMessages.JUPYTER_ADD_FAILURE + Settings.ServiceDataset.JUPYTER.getName();
              break;
            default:
              error = ResponseMessages.PROJECT_SERVICE_ADD_FAILURE;
          }
          json.setErrorMsg(json.getErrorMsg() + "\n" + error);
        }
      }
    
      // close dfsos
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  
    if (!updated) {
      json.setSuccessMessage(ResponseMessages.NOTHING_TO_UPDATE);
    }
  
    return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value= "Delete project")
  @DELETE
  @Path("/{id}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response deleteProject(@PathParam("id") Integer id,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    String userMail = sc.getUserPrincipal().getName();
    projectController.removeProject(userMail, id, req.getSession().getId());
    
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    return Response.ok(json,MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @Path("/{id}/datasets")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public DataSetsResource getDataSets(@PathParam("id") Integer id, @Context SecurityContext sc) throws AppException {
    Project project = projectController.findProjectById(id);
    dataSets.setProjectId(project.getId());
    return dataSets;
  }
  
  @ApiOperation(value = "Members sub-resource", tags = {"V2 Members"})
  @Path("/{id}/members")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public MembersResource getMembers(@PathParam("id") Integer id, @Context SecurityContext sc) throws AppException {
    Project project = projectController.findProjectById(id);
    members.setProjectId(project.getId());
    return members;
  }
  
  @Path("/{id}/activity")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getActivityForProject(@PathParam("id") Integer projectId, @Context SecurityContext sc)
      throws AppException {
    List<Activity> activities = new ArrayList<>();
    if (projectId != null){
      Project project = projectFacade.find(projectId);
      if (project == null){
        throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No such project");
      }
      activities.addAll(activityFacade.getAllActivityOnProject(project));
    }
    GenericEntity<List<Activity>> result = new GenericEntity<List<Activity>>(activities){};
    return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  
  @Path("{id}/dela")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public DelaProjectService dela(
      @PathParam("id") Integer id) throws AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.delaService.setProjectId(id);
    
    return this.delaService;
  }
}
