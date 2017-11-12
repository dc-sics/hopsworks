package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import com.google.common.base.Strings;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.adam.AdamCommand;
import io.hops.hopsworks.common.jobs.adam.AdamCommandDTO;
import io.hops.hopsworks.common.jobs.adam.AdamJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.Settings;

@RequestScoped
public class AdamService {

  private static final Logger logger = Logger.getLogger(AdamService.class.
          getName());

  private Project project;

  @EJB
  private JobFacade jobFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobController jobController;
  @EJB
  private Settings settings;

  AdamService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project of type Adam.
   * <p/>
   * @param sc
   * @param req
   * @return A list of all Jobs objects of type Adam in this
 project.
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findAllAdamJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<Jobs> jobs = jobFacade.findJobsForProjectAndType(project,
            JobType.ADAM);
    GenericEntity<List<Jobs>> jobList
            = new GenericEntity<List<Jobs>>(jobs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(jobList).build();
  }

  /**
   * Get a list of the available Adam commands. This returns a list of command
   * names.
   *
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/commands")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAdamCommands(@Context SecurityContext sc,
          @Context HttpServletRequest req) {
    JsonArrayBuilder array = Json.createArrayBuilder();
    AdamCommand[] allcommands = AdamCommand.values();
    for (AdamCommand ac : allcommands) {
      JsonObjectBuilder obj = Json.createObjectBuilder();
      obj.add("name", ac.getCommand());
      obj.add("description", ac.getDescription());
      array.add(obj);
    }
    return Response.ok(array.build()).build();
  }

  /**
   * Returns a AdamJobConfiguration for the selected command.
   *
   * @param commandName
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/commands/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getCommandDetails(@PathParam("name") String commandName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    AdamCommandDTO selected = new AdamCommandDTO(AdamCommand.getFromCommand(
            commandName));
    AdamJobConfiguration config = new AdamJobConfiguration(selected);
    //Set the HistoryServerIP to AdamJobConfiguration
    ((SparkJobConfiguration) config).setHistoryServerIp(settings.
            getSparkHistoryServerIp());

    return Response.ok(config).build();
  }

  /**
   * Create a new Job definition. If successful, the job is returned.
   *
   * @param config The configuration from which to create a Job.
   * @param sc
   * @param req
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response createJob(AdamJobConfiguration config,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    if (config == null) {
      throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
              "Cannot create job for a null argument.");
    } else {
      String email = sc.getUserPrincipal().getName();
      Users user = userFacade.findByEmail(email);
      if (user == null) {
        //Should not be possible, but, well...
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "You are not authorized for this invocation.");
      }
      if (Strings.isNullOrEmpty(config.getAppName())) {
        config.setAppName("Untitled ADAM job");
      }
      Jobs created = jobController.createJob(user, project, config);
      activityFacade.persistActivity(ActivityFacade.CREATED_JOB + created.
              getName(), project, email);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(created).build();
    }
  }
}
