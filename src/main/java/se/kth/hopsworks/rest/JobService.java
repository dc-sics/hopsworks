package se.kth.hopsworks.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.JobFinalStatus;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.jobhistory.YarnApplicationAttemptStateFacade;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstateFacade;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.configuration.ScheduleDTO;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.jobs.yarn.YarnLogUtil;
import se.kth.bbc.project.Project;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.controller.JobController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobService {

  private static final Logger logger = Logger.getLogger(JobService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private ExecutionFacade exeFacade;
  @Inject
  private ExecutionService executions;
  @Inject
  private SparkService spark;
  @Inject
  private AdamService adam;
  @Inject
  private FlinkService flink;
  @EJB
  private JobController jobController;
  @EJB
  private YarnApplicationAttemptStateFacade appAttemptStateFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserManager userBean;
  private Project project;
  private static final String PROXY_USER_COOKIE_NAME = "proxy-user";

  JobService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project.
   * <p/>
   * @param sc
   * @param req
   * @return A list of all defined Jobs in this project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<JobDescription> jobs = jobFacade.findForProject(project);
    GenericEntity<List<JobDescription>> jobList
            = new GenericEntity<List<JobDescription>>(jobs) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jobList).build();
  }

  /**
   * Get the job with the given id in the current project.
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getJob(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(job).build();
    }
  }

  /**
   * Get the JobConfiguration object for the specified job. The sole reason of
   * existence of this method is the dodginess
   * of polymorphism in JAXB/JAXRS. As such, the jobConfig field is always empty
   * when a JobDescription object is
   * returned. This method must therefore be called explicitly to get the job
   * configuration.
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/config")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getJobConfiguration(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(job.getJobConfig()).build();
    }
  }

  /**
   * Get the Job UI url for the specified job
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/ui")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getJobUI(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      Execution execution = exeFacade.findForJob(job).get(0);
      Execution updatedExecution = exeFacade.getExecution(execution.getJob().
              getId());
      if (updatedExecution != null) {
        execution = updatedExecution;
      }

      try {
        String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(
                execution.getAppId());
        if (trackingUrl != null && trackingUrl != "") {
          trackingUrl = "/hopsworks/api/project/" + project.getId() + "/jobs/"
                  + jobId + "/prox/" + trackingUrl;
          return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                  entity(trackingUrl).build();
        } else {
          return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                  entity("").build();
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception while geting job ui " + e.
                getLocalizedMessage(), e);
      }
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Get the Yarn UI url for the specified job
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/yarnui")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getYarnUI(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      Execution execution = exeFacade.findForJob(job).get(0);
      Execution updatedExecution = exeFacade.getExecution(execution.getJob().
              getId());
      if (updatedExecution != null) {
        execution = updatedExecution;
      }

      try {
        String yarnUrl = "/hopsworks/api/project/" + project.getId() + "/jobs/"
                + jobId + "/prox/" + settings.getYarnWebUIAddress()
                + "/cluster/app/"
                + execution.getAppId();

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(yarnUrl).build();

      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception while geting job ui " + e.
                getLocalizedMessage(), e);
      }
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  private static final HashSet<String> passThroughHeaders = new HashSet<String>(
          Arrays
          .asList("User-Agent", "user-agent", "Accept", "accept",
                  "Accept-Encoding", "accept-encoding", "Accept-Language",
                  "accept-language",
                  "Accept-Charset", "accept-charset"));

  /**
   * Get the job ui for the specified job.
   * This act as a proxy to get the job ui from yarn
   * <p/>
   * @param jobId
   * @param param
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/prox/{path: .+}")
  @Produces(MediaType.WILDCARD)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getProx(@PathParam("jobId") final int jobId,
          @PathParam("path") final String param,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      Execution execution = exeFacade.findForJob(job).get(0);
      Execution updatedExecution = exeFacade.getExecution(execution.getJob().
              getId());
      if (updatedExecution != null) {
        execution = updatedExecution;
      }
      try {
        String trackingUrl;
        if (param.matches("http([a-z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
          trackingUrl = param;
        } else {
          trackingUrl = "http://" + param;
        }
        trackingUrl = trackingUrl.replace("@hwqm", "?");
        if (!hasAppAccessRight(trackingUrl, job)) {
          logger.log(Level.SEVERE,
                  "A user is trying to access an app outside their project!");
          return Response.status(Response.Status.FORBIDDEN).build();
        }
        org.apache.commons.httpclient.URI uri
                = new org.apache.commons.httpclient.URI(trackingUrl, false);

        HttpClientParams params = new HttpClientParams();
        params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
                true);
        HttpClient client = new HttpClient(params);
        HostConfiguration config = new HostConfiguration();
        InetAddress localAddress = InetAddress.getLocalHost();
        config.setLocalAddress(localAddress);

        final HttpMethod method = new GetMethod(uri.getEscapedURI());
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
          String name = names.nextElement();
          String value = req.getHeader(name);
          if (passThroughHeaders.contains(name)) {
            //yarn does not send back the js if encoding is not accepted
            //but we don't want to accept encoding for the html because we
            //need to be able to parse it
            if (!name.toLowerCase().equals("accept-encoding") || trackingUrl.
                    contains(".js")) {
              method.setRequestHeader(name, value);
            }
          }
        }
        String user = req.getRemoteUser();
        if (user != null && !user.isEmpty()) {
          method.setRequestHeader("Cookie", PROXY_USER_COOKIE_NAME + "="
                  + URLEncoder.encode(user, "ASCII"));
        }

        client.executeMethod(config, method);
        Response.ResponseBuilder response = noCacheResponse.
                getNoCacheResponseBuilder(Response.Status.OK);
        for (Header header : method.getResponseHeaders()) {
          response.header(header.getName(), header.getValue());
        }
        if (method.getResponseHeader("Content-Type") == null || method.
                getResponseHeader("Content-Type").getValue().contains("html")) {
          final String source = "http://" + method.getURI().getHost() + ":"
                  + method.getURI().getPort();
          if (method.getResponseHeader("Content-Length") == null) {
            response.entity(new StreamingOutput() {
              @Override
              public void write(OutputStream out) throws IOException,
                      WebApplicationException {
                Writer writer
                        = new BufferedWriter(new OutputStreamWriter(out));
                InputStream stream = method.getResponseBodyAsStream();
                Reader in = new InputStreamReader(stream, "UTF-8");
                char[] buffer = new char[4 * 1024];
                String remaining = "";
                int n;
                while ((n = in.read(buffer)) != -1) {
                  StringBuilder strb = new StringBuilder();
                  strb.append(buffer, 0, n);
                  String s = remaining + strb.toString();
                  remaining = s.substring(s.lastIndexOf(">") + 1, s.length());
                  s = hopify(s.substring(0, s.lastIndexOf(">") + 1), param,
                          jobId,
                          source);
                  writer.write(s);
                }
                writer.flush();
              }
            });
          } else {
            String s = hopify(method.getResponseBodyAsString(), param, jobId,
                    source);
            response.entity(s);
            response.header("Content-Length", s.length());
          }

        } else {
          response.entity(new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException,
                    WebApplicationException {
              InputStream stream = method.getResponseBodyAsStream();
              org.apache.hadoop.io.IOUtils.copyBytes(stream, out, 4096, true);
              out.flush();
            }
          });
        }
        return response.build();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception while geting job ui " + e.
                getLocalizedMessage(), e);
        return noCacheResponse.
                getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
      }
    }
  }

  private String hopify(String ui, String param, int jobId, String source) {

    //remove the link to the full cluster information in the yarn ui
    ui = ui.replaceAll(
            "<div id=\"user\">[\\s\\S]+Logged in as: dr.who[\\s\\S]+<div id=\"logo\">",
            "<div id=\"logo\">");
    ui = ui.replaceAll(
            "<div id=\"footer\" class=\"ui-widget\">[\\s\\S]+<tbody>",
            "<tbody>");
    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td ", "<td ");
    ui = ui.replaceAll(
            "<li><a ui-sref=\"submit\"[\\s\\S]+new Job</a></li>", "");

    ui = ui.replaceAll("(?<=(href|src)=.[^>]{0,200})\\?", "@hwqm");

    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-z])",
            "/hopsworks/api/project/"
            + project.getId() + "/jobs/" + jobId + "/prox/"
            + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks/api/project/"
            + project.getId() + "/jobs/" + jobId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)",
            "/hopsworks/api/project/"
            + project.getId() + "/jobs/" + jobId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-z])",
            "/hopsworks/api/project/"
            + project.getId() + "/jobs/" + jobId + "/prox/" + param);
    return ui;

  }

  private boolean hasAppAccessRight(String trackingUrl, JobDescription job) {
    String appId = "";
    if (trackingUrl.contains("application_")) {
      for (String elem : trackingUrl.split("/")) {
        if (elem.contains("application_")) {
          appId = elem;
          break;
        }
      }
    } else if (trackingUrl.contains("container_")) {
      appId = "application_";
      for (String elem : trackingUrl.split("/")) {
        if (elem.contains("container_")) {
          String[] containerIdElem = elem.split("_");
          appId = appId + containerIdElem[1] + "_" + containerIdElem[2];
          break;
        }
      }

    }
    if (appId != "") {
      String appUser = yarnApplicationstateFacade.findByAppId(appId).
              getAppuser();
      if (!job.getProject().getName().equals(hdfsUsersBean.getProjectName(
              appUser))) {
        return false;
      }
    }
    return true;
  }

  @GET
  @Path("/template/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getConfigurationTemplate(@PathParam("type") String type,
          @Context SecurityContext sc, @Context HttpServletRequest req) {
    JobConfiguration template = JobConfiguration.JobConfigurationFactory.
            getJobConfigurationTemplate(JobType.valueOf(type));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(template).build();
  }

  /**
   * Get all the jobs in this project that have a running execution. The return
   * value is a JSON object, where each job
   * id is a key and the corresponding boolean indicates whether the job is
   * running or not.
   * <p/>
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getConfigurationTemplate(@Context SecurityContext sc,
          @Context HttpServletRequest req) {
    List<JobDescription> running = jobFacade.getRunningJobs(project);
    List<JobDescription> allJobs = jobFacade.findForProject(project);
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (JobDescription desc : allJobs) {
      try {
        List<Execution> executions = exeFacade.findForJob(desc);
        if (executions != null && executions.isEmpty() == false) {
          Execution execution = executions.get(0);
          builder.add(desc.getId().toString(), Json.createObjectBuilder()
                  .add("running", false)
                  .add("state", execution.getState().toString())
                  .add("finalStatus", execution.getFinalStatus().toString())
                  .add("progress", execution.getProgress())
                  .add("duration", execution.getExecutionDuration())
                  .add("submissiontime", execution.getSubmissionTime().
                          toString())
          );
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        logger.log(Level.WARNING, "No execution was found: {0}", e
                .getMessage());
      }
    }
    for (JobDescription desc : running) {
      try {
        Execution execution = exeFacade.findForJob(desc).get(0);
        Execution updatedExecution = exeFacade.getExecution(execution.getJob().
                getId());
        if (updatedExecution != null) {
          execution = updatedExecution;          
        }
        long executiontime = System.currentTimeMillis() - execution.
                getSubmissionTime().getTime();
        //not given appId (not submited yet)
        if (execution.getAppId() == null && executiontime > 60000l * 5) {
          exeFacade.updateState(execution, JobState.INITIALIZATION_FAILED);
          exeFacade.updateFinalStatus(execution, JobFinalStatus.FAILED);
          continue;
        }

        String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(
                execution.getAppId());
        builder.add(desc.getId().toString(),
                Json.createObjectBuilder()
                .add("running", true)
                .add("state", execution.getState().toString())
                .add("finalStatus", execution.getFinalStatus().toString())
                .add("progress", execution.getProgress())
                .add("duration", execution.getExecutionDuration())
                .add("submissiontime", execution.getSubmissionTime().toString())
                .add("url", trackingUrl)
        );
      } catch (ArrayIndexOutOfBoundsException e) {
        logger.log(Level.WARNING, "No execution was found: {0}", e
                .getMessage());
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(builder.build()).build();
  }

  /**
   * Get the log information related to a job. The return value is a JSON
   * object, with format logset=[{"time":"JOB
   * EXECUTION TIME"}, {"log":"INFORMATION LOG"}, {"err":"ERROR LOG"}]
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/{jobId}/showlog")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getLogInformation(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {

    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      List<Execution> executionHistory = exeFacade.
              findbyProjectAndJobId(project, jobId);
      JsonObjectBuilder arrayObjectBuilder;
      if (executionHistory != null && !executionHistory.isEmpty()) {
        String message;
        String stdPath;
        for (Execution e : executionHistory) {
          arrayObjectBuilder = Json.createObjectBuilder();
          arrayObjectBuilder.add("appId", e.getAppId() == null ? "" : e.
                  getAppId());
          arrayObjectBuilder.add("time", e.getSubmissionTime().toString());
          String hdfsLogPath = "hdfs://" + e.getStdoutPath();
          if (e.getStdoutPath() != null && !e.getStdoutPath().isEmpty() && dfso.
                  exists(hdfsLogPath)) {
            if (dfso.listStatus(new org.apache.hadoop.fs.Path(
                    hdfsLogPath))[0].getLen() > 5000000l) {
              stdPath = e.getStdoutPath().split(this.project.getName())[1];
              arrayObjectBuilder.add("log",
                      "Log is too big to display. Please retrieve it by clicking ");
              arrayObjectBuilder.add("logPath", "/project/" + this.project.
                      getId() + "/datasets" + stdPath);
            } else {
              InputStream input = dfso.open(hdfsLogPath);
              message = IOUtils.toString(input,
                      "UTF-8");
              input.close();
              arrayObjectBuilder.add("log", message.isEmpty()
                      ? "No information." : message);
              if (message.isEmpty() && e.getState().isFinalState() && e.
                      getAppId() != null
                      && e.getFinalStatus().equals(JobFinalStatus.SUCCEEDED)) {
                arrayObjectBuilder.add("retriableOut", "true");
              }
            }

          } else {
            arrayObjectBuilder.add("log", "No log available");
            if (e.getState().isFinalState() && e.getFinalStatus().equals(
                    JobFinalStatus.SUCCEEDED) && e.getAppId() != null) {
              arrayObjectBuilder.add("retriableOut", "true");
            }
          }
          String hdfsErrPath = "hdfs://" + e.getStderrPath();
          if (e.getStderrPath() != null && !e.getStderrPath().isEmpty() && dfso.
                  exists(hdfsErrPath)) {
            if (dfso.listStatus(new org.apache.hadoop.fs.Path(
                    hdfsErrPath))[0].getLen() > 5000000l) {
              stdPath = e.getStderrPath().split(this.project.getName())[1];
              arrayObjectBuilder.add("err",
                      "Log is too big to display. Please retrieve it by clicking ");
              arrayObjectBuilder.add("errPath", "/project/" + this.project.
                      getId() + "/datasets" + stdPath);
            } else {
              InputStream input = dfso.open(hdfsErrPath);
              message = IOUtils.toString(input,
                      "UTF-8");
              input.close();
              arrayObjectBuilder.add("err", message.isEmpty() ? "No error."
                      : message);
              if (message.isEmpty() && e.getState().isFinalState() && e.
                      getAppId() != null) {
                arrayObjectBuilder.add("retriableErr", "err");
              }
            }
          } else {
            arrayObjectBuilder.add("err", "No log available");
            if (e.getState().isFinalState() && e.getAppId() != null) {
              arrayObjectBuilder.add("retriableErr", "err");
            }
          }
          arrayBuilder.add(arrayObjectBuilder);
        }
      } else {
        arrayObjectBuilder = Json.createObjectBuilder();
        arrayObjectBuilder.add("time", "No log available");
        arrayObjectBuilder.add("log", "No log available");
        arrayObjectBuilder.add("err", "No log available");
        arrayBuilder.add(arrayObjectBuilder);
      }
      builder.add("logset", arrayBuilder);
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Error when reading hdfs logs: {0}", ex.
              getMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(builder.build()).build();
  }

  @GET
  @Path("/retryLogAggregation/{appId}/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response retryLogAggregation(@PathParam("appId") String appId,
          @PathParam("type") String type,
          @Context HttpServletRequest req) throws AppException {
    if (appId == null || appId.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(), "Can not get log. No ApplicationId.");
    }
    Execution execution = exeFacade.findByAppId(appId);
    if (execution == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(), "No excution for appId " + appId);
    }
    if (!execution.getState().isFinalState()) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(), "Job still running.");
    }
    if (!execution.getJob().getProject().equals(this.project)) {
      throw new AppException(Response.Status.BAD_REQUEST.
              getStatusCode(), "No excution for appId " + appId
              + ".");
    }

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    Users user = execution.getUser();
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    String aggregatedLogPath = jobController.getAggregatedLogPath(hdfsUser,
            appId);
    if (aggregatedLogPath == null) {
      throw new AppException(Response.Status.NOT_FOUND.
              getStatusCode(),
              "Aggregation is not enabled.");
    }
    try {
      dfso = dfs.getDfsOps();
      udfso = dfs.getDfsOps(hdfsUser);
      if (!dfso.exists(aggregatedLogPath)) {
        throw new AppException(Response.Status.NOT_FOUND.
                getStatusCode(),
                "Logs not available. This could be caused by the rentention policy");
      }
      if (type.equals("out")) {
        String hdfsLogPath = "hdfs://" + execution.getStdoutPath();
        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
                isEmpty()) {
          if (dfso.exists(hdfsLogPath) && dfso.getFileStatus(
                  new org.apache.hadoop.fs.Path(hdfsLogPath)).getLen() > 0) {
            throw new AppException(Response.Status.BAD_REQUEST.
                    getStatusCode(),
                    "Destination file is not empty.");
          } else {
            YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
                    hdfsLogPath, "out");
          }
        }
      } else if (type.equals("err")) {
        String hdfsErrPath = "hdfs://" + execution.getStderrPath();
        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
                isEmpty()) {
          if (dfso.exists(hdfsErrPath) && dfso.getFileStatus(
                  new org.apache.hadoop.fs.Path(hdfsErrPath)).getLen() > 0) {
            throw new AppException(Response.Status.BAD_REQUEST.
                    getStatusCode(),
                    "Destination file is not empty.");
          } else {
            YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
                    hdfsErrPath, "err");
          }
        }
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Log retrieved successfuly.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * Delete the job associated to the project and jobid. The return value is a
   * JSON object stating operation successful
   * or not.
   * <p/>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @DELETE
  @Path("/{jobId}/deleteJob")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response deleteJob(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    logger.log(Level.INFO, "Request to delete job");

    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    } else {
      try {
        logger.log(Level.INFO, "Request to delete job name ={0} job id ={1}",
                new Object[]{job.getName(), job.getId()});
        jobFacade.removeJob(job);
        logger.log(Level.INFO, "Deleted job name ={0} job id ={1}",
                new Object[]{job.getName(), job.getId()});
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Deleted job " + job.getName() + " successfully");
        activityFacade.persistActivity(ActivityFacade.DELETED_JOB + job.
                getName(), project, sc.getUserPrincipal().getName());
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
      } catch (DatabaseException ex) {
        logger.log(Level.WARNING,
                "Job cannot be deleted  job name ={0} job id ={1}",
                new Object[]{job.getName(), job.getId()});
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ex.getMessage());
      }
    }
  }

  /**
   * Get the ExecutionService for the job with given id.
   * <p/>
   * @param jobId
   * @return
   */
  @Path("/{jobId}/executions")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public ExecutionService executions(@PathParam("jobId") int jobId) {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return null;
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return null;
    } else {
      return this.executions.setJob(job);
    }
  }

  @POST
  @Path("/updateschedule/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response updateSchedule(ScheduleDTO schedule,
          @PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    } else {
      try {
        boolean isScheduleUpdated = jobFacade.updateJobSchedule(jobId, schedule);
        if (isScheduleUpdated) {
          boolean status = jobController.scheduleJob(jobId);
          if (status) {
            JsonResponse json = new JsonResponse();
            json.setSuccessMessage("Scheduled job " + job.getName()
                    + " successfully");
            activityFacade.persistActivity(ActivityFacade.SCHEDULED_JOB + job.
                    getName(), project, sc.getUserPrincipal().getName());
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                    entity(json).build();
          } else {
            logger.log(Level.WARNING,
                    "Schedule is not created in the scheduler for the jobid "
                    + jobId);
          }
        } else {
          logger.log(Level.WARNING,
                  "Schedule is not updated in DB for the jobid " + jobId);
        }

      } catch (DatabaseException ex) {
        logger.log(Level.WARNING, "Cannot update schedule " + ex.getMessage());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ex.getMessage());
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  @Path("/spark")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public SparkService spark() {
    return this.spark.setProject(project);
  }

  @Path("/adam")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public AdamService adam() {
    return this.adam.setProject(project);
  }

  @Path("/flink")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public FlinkService flink() {
    return this.flink.setProject(project);
  }
}
