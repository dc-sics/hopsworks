package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.alert.Alert;
import io.hops.hopsworks.common.dao.alert.AlertEJB;
import io.hops.hopsworks.common.dao.host.Health;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.BlockReport;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaOp;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.mail.MessagingException;
import javax.ws.rs.POST;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import org.json.simple.JSONArray;

@Path("/agentresource")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "AGENT"})
@Api(value = "Agent Service",
    description = "Agent Service")
public class AgentResource {

  @EJB
  private HostsFacade hostFacade;
  @EJB
  private HostServicesFacade hostServiceFacade;
  @EJB
  private AlertEJB alertFacade;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private EmailBean emailBean;

  final static Logger logger = Logger.getLogger(AgentResource.class.getName());

  public class CondaCommandsComparator implements Comparator<CondaCommands> {

    @Override
    public int compare(CondaCommands c1, CondaCommands c2) {
      if (c1.getId() > c2.getId()) {
        return 1;
      } else if (c1.getId() < c2.getId()) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  @GET
  @Path("ping")
  @Produces(MediaType.TEXT_PLAIN)
  public String ping() {
    return "Kmon: Pong";
  }

  @POST
  @Path("/heartbeat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response heartbeat(@Context SecurityContext sc,
      @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders, String jsonHb) {
    // Commands are sent back to the kagent as a response to this heartbeat.
    // Kagent then executes the commands received in order.
    List<CondaCommands> commands = new ArrayList<>();

    try {

      InputStream stream = new ByteArrayInputStream(jsonHb.getBytes(
          StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      long agentTime = json.getJsonNumber("agent-time").longValue();
      String hostname = json.getString("host-id");
      Hosts host = hostFacade.findByHostname(hostname);
      if (host == null) {
        logger.log(Level.WARNING, "Host with id {0} not found.", hostname);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (!host.isRegistered()) {
        logger.log(Level.WARNING, "Host with id {0} is not registered.", hostname);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
      }
      host.setLastHeartbeat((new Date()).getTime());
      host.setLoad1(json.getJsonNumber("load1").doubleValue());
      host.setLoad5(json.getJsonNumber("load5").doubleValue());
      host.setLoad15(json.getJsonNumber("load15").doubleValue());
      Integer numGpus = json.getJsonNumber("num-gpus").intValue();
      host.setNumGpus( numGpus);  // '1' means has a GPU, '0' means doesn't have one.
      Long previousDiskUsed = host.getDiskUsed() == null ? 0l : host.getDiskUsed(); 
      host.setDiskUsed(json.getJsonNumber("disk-used").longValue());
      host.setMemoryUsed(json.getJsonNumber("memory-used").longValue());
      host.setPrivateIp(json.getString("private-ip"));
      host.setDiskCapacity(json.getJsonNumber("disk-capacity").longValue());
      if (previousDiskUsed < host.getDiskUsed() && ((float) host.getDiskUsed()) / host.getDiskCapacity() > 0.8) {
        String subject = "alert: hard drive full on " + host.getHostname();
        String body = host.getHostname() + " hard drive utilisation is " + host.getDiskUsageInfo();
        emailAlert(subject, body);
      }
      host.setMemoryCapacity(json.getJsonNumber("memory-capacity").longValue());
      host.setCores(json.getInt("cores"));
      hostFacade.storeHost(host, false);

      JsonArray roles = json.getJsonArray("services");
      for (int i = 0; i < roles.size(); i++) {
        JsonObject s = roles.getJsonObject(i);

        if (!s.containsKey("cluster") || !s.containsKey("group") || !s.
            containsKey("service")) {
          logger.warning("Badly formed JSON object describing a service.");
          continue;
        }
        String cluster = s.getString("cluster");
        String serviceName = s.getString("service");
        String group = s.getString("group");
        HostServices hostService = null;
        try {
          hostService = hostServiceFacade.find(hostname, cluster, group, serviceName);
        } catch (Exception ex) {
          logger.log(Level.FINE, "Could not find a service for the kagent heartbeat.");
          continue;
        }

        if (hostService == null) {
          hostService = new HostServices();
          hostService.setHost(host);
          hostService.setCluster(cluster);
          hostService.setGroup(group);
          hostService.setService(serviceName);
          hostService.setStartTime(agentTime);
        }

        String webPort = s.containsKey("web-port") ? s.getString("web-port")
            : "0";
        String pid = s.containsKey("pid") ? s.getString("pid") : "-1";
        try {
//          role.setWebPort(Integer.parseInt(webPort));
          hostService.setPid(Integer.parseInt(pid));
        } catch (NumberFormatException ex) {
          logger.log(Level.WARNING, "Invalid webport or pid - not a number for: {0}", hostService);
          continue;
        }
        Health previousHealthOfService = hostService.getHealth();
        if (s.containsKey("status")) {
          if ((hostService.getStatus() == null || !hostService.getStatus().equals(Status.Started)) && Status.valueOf(s.
              getString(
                  "status")).equals(Status.Started)) {
            hostService.setStartTime(agentTime);
          }
          hostService.setStatus(Status.valueOf(s.getString("status")));
        } else {
          hostService.setStatus(Status.None);
        }

        Long startTime = hostService.getStartTime();
        Status status = Status.valueOf(s.getString("status"));
        if (status.equals(Status.Started)) {
          hostService.setStopTime(agentTime);
        }
        Long stopTime = hostService.getStopTime();

        if (startTime != null && stopTime != null) {
          hostService.setUptime(stopTime - startTime);
        } else {
          hostService.setUptime(0);
        }
        hostServiceFacade.store(hostService);
        if (!hostService.getHealth().equals(previousHealthOfService) && hostService.getHealth().equals(Health.Bad)) {
          String subject = "alert: " + hostService.getGroup() + "." + hostService.getService() + "@" + hostService.
              getHost().getHostname();
          String body = hostService.getGroup() + "." + hostService.getService() + "@" + hostService.getHost().
              getHostname() + " transitioned from state " + previousHealthOfService + " to " + hostService.getHealth();
          emailAlert(subject, body);
        }

      }

      if (json.containsKey("conda-ops")) {
        JsonArray condaOps = json.getJsonArray("conda-ops");
        for (int j = 0; j < condaOps.size(); j++) {
          JsonObject entry = condaOps.getJsonObject(j);

          String projName = entry.getString("proj");
          String op = entry.getString("op");
          PythonDepsFacade.CondaOp opType = PythonDepsFacade.CondaOp.valueOf(op.toUpperCase());
          String channelurl = entry.getString("channelurl");
          String lib = entry.containsKey("lib") ? entry.getString("lib") : "";
          String version = entry.containsKey("version") ? entry.getString("version") : "";
          String arg = entry.containsKey("arg") ? entry.getString("arg") : "";
          String status = entry.getString("status");
          PythonDepsFacade.CondaStatus agentStatus = PythonDepsFacade.CondaStatus.valueOf(status.toUpperCase());
          int commmandId = Integer.parseInt(entry.getString("id"));

          CondaCommands command = pythonDepsFacade.
              findCondaCommand(commmandId);
          // If the command object does not exist, then the project
          // has probably been removed. We needed to send a compensating action if
          // this action was successful.
          if (command != null) {
            if (agentStatus == PythonDepsFacade.CondaStatus.SUCCESS) {
              // remove command from the DB
              pythonDepsFacade.
                  updateCondaComamandStatus(commmandId, agentStatus, arg, projName, opType, lib, version);
            } else {
              pythonDepsFacade.
                  updateCondaComamandStatus(commmandId, agentStatus, arg, projName, opType, lib, version);
            }
          }
        }
      }

      List<CondaCommands> differenceList = new ArrayList<>();

      if (json.containsKey("block-report")) {
        // Map<'project', 'installed-libs'>
        Map<String, BlockReport> mapReports = new HashMap<>();

        JsonObject envs = json.getJsonObject("block-report");
        for (String s : envs.keySet()) {
          JsonArray installedLibs = envs.getJsonArray(s);

          String projName = s;
          BlockReport br = new BlockReport();
          mapReports.put(projName, br);
          br.setProject(projName);
          for (int k = 0; k < installedLibs.size(); k++) {
            JsonObject libObj = installedLibs.getJsonObject(k);
            String libName = libObj.getString("name");
            String libUrl = libObj.getString("channel");
            String libVersion = libObj.getString("version");
            br.addLib(libName, libUrl, libVersion);
          }
        }

        // get all the projects and send them down and all the dependencies
        // for all the projects and send them down, too.
        List<Project> allProjs = projFacade.findAll();
        // For each project, verify all its libs are in the blockreport list
        // Any extra blocks reported need to be removed. Any missing need to
        // be added
        for (Project project : allProjs) {

          Collection<CondaCommands> allCcs = project.
              getCondaCommandsCollection();
          logger.log(Level.INFO, "AnacondaReport: {0}", project.getName());

          if ((!mapReports.containsKey(project.getName())) && (project.getName().compareToIgnoreCase(settings.
              getAnacondaEnv())) != 0) {
            // project not a conda environment
            // check if a conda-command exists for creating the project and is valid.

            boolean noExistingCommandInDB = true;
            for (CondaCommands command : allCcs) {
              if (command.getOp() == CondaOp.CREATE && command.getProj().
                  compareTo(project.getName()) == 0) {
                noExistingCommandInDB = false; // command already exists
              }
            }
            if (noExistingCommandInDB) {
              CondaCommands cc = new CondaCommands(host, settings.getSparkUser(), CondaOp.CREATE, CondaStatus.ONGOING,
                  project, "", "", "", null, "");
              // commandId == '-1' implies this is a block report command that
              // doesn't need to be acknowledged by the agent (no need to send as a
              // reponse a command-status). No need to persist this command to the DB either.
              cc.setId(-1);
              // Need to create env on node
              differenceList.add(cc);
            }

          } else { // This project exists as a conda env
            BlockReport br = mapReports.get(project.getName());
            for (PythonDep lib : project.getPythonDepCollection()) {
              BlockReport.Lib blockLib = br.getLib(lib.getDependency());
              if (blockLib == null || blockLib.compareTo(lib) != 0) {
                CondaCommands cc = new CondaCommands(host, settings.
                    getAnacondaUser(),
                    CondaOp.INSTALL, CondaStatus.ONGOING, project,
                    lib.getDependency(),
                    lib.getRepoUrl().getUrl(), lib.getVersion(),
                    Date.from(Instant.now()), "");
                cc.setId(-1);
                differenceList.add(cc);
              }
              // we mark the library as checked by deleting it from the incoming br
              if (blockLib != null) {
                br.removeLib(blockLib.getLib());
              }
            }
            // remove any extra libraries in the conda-env, not in the project
            // get removed from the conda env.
            for (BlockReport.Lib blockLib : br.getLibs()) {
              CondaCommands cc
                  = new CondaCommands(host, settings.getAnacondaUser(),
                      CondaOp.UNINSTALL, CondaStatus.ONGOING, project,
                      blockLib.getLib(),
                      blockLib.getChannelUrl(), blockLib.getVersion(),
                      null, "");
              cc.setId(-1);
              differenceList.add(cc);
            }
            mapReports.remove(project.getName());
          }

          // The LIB_SYNC command should come after all the environment conda_commands -
          // the environments need to exist and be correct, before we can sync up their libraries.
          // Kagent needs to execute these conda_commands in the correct order.
          // Get all the 'libs' for this project. Send them down as a block-report
          Collection<PythonDep> projectLibs = project.getPythonDepCollection();
          JSONArray libs = new JSONArray();
          for (PythonDep pd : projectLibs) {
            libs.add("library : " + pd.getDependency() + "-" + pd.getVersion());
          }
          CondaCommands cc = new CondaCommands();
          cc.setId(-1);
          cc.setHostId(host);
          cc.setUser(settings.getAnacondaUser());
          cc.setProj(project.getName());
          cc.setLib(libs.toJSONString());
          cc.setOp(PythonDepsFacade.CondaOp.LIB_SYNC);
          differenceList.add(cc);
        }
        // All the conda environments that weren't in the project list, remove them.
        for (BlockReport br : mapReports.values()) {
          // Don't delete our default environment

          logger.log(Level.INFO, "BlockReport: {0} - {1}", new Object[]{br.
            getProject(), br.getLibs().size()});

          if (br.getProject().compareToIgnoreCase(settings.getAnacondaEnv())
              == 0) {
            continue;
          }
          CondaCommands cc = new CondaCommands();
          cc.setId(-1);
          cc.setHostId(host);
          cc.setUser(settings.getAnacondaUser());
          cc.setProj(br.getProject());
          cc.setOp(PythonDepsFacade.CondaOp.REMOVE);
          differenceList.add(cc);
        }

      }

      Collection<CondaCommands> allCommands = host.
          getCondaCommandsCollection();

      Collection<CondaCommands> commandsToExec = new ArrayList<>();
      for (CondaCommands cc : allCommands) {
        if (cc.getStatus() != PythonDepsFacade.CondaStatus.FAILED) {
          commandsToExec.add(cc);
          cc.setHostId(host);
        }
      }
      commands.addAll(commandsToExec);
      commands.addAll(differenceList);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    Collections.sort(commands, new CondaCommandsComparator());
    GenericEntity<List<CondaCommands>> kcs = new GenericEntity<List<CondaCommands>>(commands) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        kcs).build();
  }

  private void emailAlert(String subject, String body) {
    try {
      emailBean.sendEmails(settings.getAlertEmailAddrs(), subject, body);
    } catch (MessagingException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  @POST
  @Path("/alert")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response alert(@Context SecurityContext sc,
      @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders, String jsonString
  ) {
    // TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
    Alert alert = new Alert();
    try {
      InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      alert.setAlertTime(new Date());
      alert.setProvider(Alert.Provider.valueOf(json.getString("Provider")).toString());
      alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")).toString());
      alert.setAgentTime(json.getJsonNumber("Time").bigIntegerValue());
      alert.setMessage(json.getString("Message"));
      String hostname = json.getString("host-id");
      Hosts h = hostFacade.findByHostname(hostname);
      alert.setHost(h);
      alert.setPlugin(json.getString("Plugin"));
      if (json.containsKey("PluginInstance")) {
        alert.setPluginInstance(json.getString("PluginInstance"));
      }
      if (json.containsKey("Type")) {
        alert.setType(json.getString("Type"));
      }
      if (json.containsKey("TypeInstance")) {
        alert.setTypeInstance(json.getString("TypeInstance"));
      }
      if (json.containsKey("DataSource")) {
        alert.setDataSource(json.getString("DataSource"));
      }
      if (json.containsKey("CurrentValue")) {
        alert.setCurrentValue(Boolean.toString(json.getBoolean("CurrentValue")));
      }
      if (json.containsKey("WarningMin")) {
        alert.setWarningMin(json.getString("WarningMin"));
      }
      if (json.containsKey("WarningMax")) {
        alert.setWarningMax(json.getString("WarningMax"));
      }
      if (json.containsKey("FailureMin")) {
        alert.setFailureMin(json.getString("FailureMin"));
      }
      if (json.containsKey("FailureMax")) {
        alert.setFailureMax(json.getString("FailureMax"));
      }
      alertFacade.persistAlert(alert);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: {0}", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    if (!settings.getAlertEmailAddrs().isEmpty()) {
      try {
        emailBean.sendEmails(settings.getAlertEmailAddrs(), UserAccountsEmailMessages.ALERT_SERVICE_DOWN, alert.
            toString());
      } catch (MessagingException ex) {
        Logger.getLogger(AgentResource.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return Response.ok().build();
  }
}
