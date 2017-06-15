package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFactory;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.GenericEntity;
import org.apache.commons.codec.digest.DigestUtils;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterService {

  private final static Logger LOGGER = Logger.getLogger(JupyterService.class.
          getName());
  private static final Logger logger = Logger.getLogger(
          JupyterService.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserManager userManager;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JupyterConfigFactory jupyterConfigFactory;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;

  private Integer projectId;
  private Project project;

  public JupyterService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Launches a Jupyter notebook server for this project-specific user
   *
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response getAllNotebookServersInProject(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Collection<JupyterProject> servers = project.getJupyterProjectCollection();

    if (servers == null) {
      throw new AppException(
              Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find any Jupyter notebook servers for this project.");
    }

    List<JupyterProject> listServers = new ArrayList<>();
    listServers.addAll(servers);

    GenericEntity<List<JupyterProject>> notebookServers
            = new GenericEntity<List<JupyterProject>>(listServers) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            notebookServers).build();
  }

  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response isMyNotebookServerRunning(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(
              Response.Status.NOT_FOUND.getStatusCode(),
              "Could not find any Jupyter notebook server for this project.");
    }
    if (jp != null) {
      // Check to make sure the jupyter notebook server is running
      boolean running = jupyterConfigFactory.pingServerJupyterUser(jp.getPid());
      // if the notebook is not running but we have a database entry for it,
      // we should remove the DB entry (and restart the notebook server).
      if (!running) {
        jupyterFacade.removeNotebookServer(hdfsUser);
        throw new AppException(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Found Jupyter notebook server for you, but it wasn't running.");
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jp).build();
  }

  @POST
  @Path("/start")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response startNotebookServer(JupyterDTO jupyterConfig,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not find your username. Report a bug.");
    }

    boolean enabled = project.getConda();
    if (!enabled) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "First enable Anaconda. Click on 'Settings -> Python'");
    }

    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);

    if (jp == null) {
      HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

      String secret = DigestUtils.sha256Hex(Integer.toString(
              ThreadLocalRandom.current().nextInt()));
      JupyterDTO dto;
      try {

        dto = jupyterConfigFactory.startServerAsJupyterUser(project, secret,
                hdfsUser,
                jupyterConfig.getDriverCores(), jupyterConfig.getDriverMemory(),
                jupyterConfig.getNumExecutors(),
                jupyterConfig.getExecutorCores(), jupyterConfig.
                getExecutorMemory(), jupyterConfig.getGpus(),
                jupyterConfig.getArchives(), jupyterConfig.getJars(),
                jupyterConfig.getFiles(), jupyterConfig.getPyFiles());
      } catch (InterruptedException | IOException ex) {
        Logger.getLogger(JupyterService.class.getName()).log(Level.SEVERE, null,
                ex);
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Problem starting a Jupyter notebook server.");
      }

      if (dto == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Incomplete request!");
      }

      jp = jupyterFacade.
              saveServer(project, secret, dto.getPort(), user.getId(), dto.
                      getToken(), dto.getPid(), dto.getDriverCores(), dto.
                      getDriverMemory(), dto.getNumExecutors(), dto.
                      getExecutorCores(),
                      dto.getExecutorMemory(), dto.getGpus(), dto.getArchives(),
                      dto.
                      getJars(), dto.getFiles(), dto.getPyFiles());

      if (jp == null) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Could not save Jupyter Settings.");
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jp).build();
  }

  @GET
  @Path("/stopAll")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({"HOPS_ADMIN"})
  public Response stopAll(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    Collection<ProjectTeam> team = this.project.getProjectTeamCollection();
    for (ProjectTeam pt : team) {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, pt.
              getUser());
      try {
        stop(hdfsUsername);
      } catch (AppException ex) {
        // continue
      }
    }

    String prog = settings.getHopsworksDomainDir()
            + "/bin/jupyter-project-cleanup.sh";
    int exitValue;
    Integer id = 1;
    String projectPath = settings.getJupyterDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.project.getName();

    String[] command = {"/usr/bin/sudo", prog, projectPath};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }
      process.waitFor(2l, TimeUnit.SECONDS);
      exitValue = process.exitValue();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem cleaning up project: " 
              + projectPath + ": {0}", ex.toString());
      exitValue = -2;
    }

    if (exitValue != 0) {
      throw new AppException(Response.Status.REQUEST_TIMEOUT.getStatusCode(),
              "Couldn't stop Jupyter Notebook Server.");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stopDataOwner")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response stopDataOwner(@PathParam("hdfsUsername") String hdfsUsername,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("/stop")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response stopNotebookServer(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String hdfsUsername = getHdfsUser(sc);
    stop(hdfsUsername);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private void stop(String hdfsUser) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    // We need to stop the jupyter notebook server with the PID
    // If we can't stop the server, delete the Entity bean anyway
    JupyterProject jp = jupyterFacade.findByUser(hdfsUser);
    if (jp == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not find Jupyter entry for user: " + hdfsUser);
    }
    String projectPath = jupyterConfigFactory.getJupyterHome(hdfsUser, jp);

    // stop the server, remove the user in this project's local dirs
    jupyterConfigFactory.stopServerJupyterUser(projectPath, jp.getPid(), jp.
            getPort());
    // remove the reference to th e server in the DB.
    jupyterFacade.removeNotebookServer(hdfsUser);
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    return hdfsUsername;
  }

}
