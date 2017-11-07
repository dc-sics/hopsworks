package io.hops.hopsworks.admin.llap;

import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptReport;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ContainerReport;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.YarnApplicationAttemptState;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class LlapClusterFacade {

  private static final Logger logger = Logger.getLogger(LlapClusterFacade.class.getName());

  @EJB
  private YarnClientService yarnClientService;
  @EJB
  private Settings settings;
  @EJB
  private VariablesFacade variablesFacade;

  public boolean isClusterUp() {
    String llapAppID = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_APP_ID);
    if (llapAppID == null || llapAppID.isEmpty()) {
      return false;
    }

    ApplicationId appId = ApplicationId.fromString(llapAppID);
    YarnClient yarnClient = yarnClientService.getYarnClientSuper(settings.getConfiguration()).getYarnClient();
    ApplicationReport applicationReport = null;
    try {
      applicationReport = yarnClient.getApplicationReport(appId);
    } catch (IOException | YarnException e) {
      logger.log(Level.SEVERE, "Could not retrieve application state for llap cluster with appId: "
          + appId.toString(), e);
      return false;
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    YarnApplicationState appState = applicationReport.getYarnApplicationState();
    return appState == YarnApplicationState.RUNNING ||
        appState == YarnApplicationState.SUBMITTED ||
        appState == YarnApplicationState.ACCEPTED ||
        appState == YarnApplicationState.NEW ||
        appState == YarnApplicationState.NEW_SAVING;
  }


  public boolean isClusterStarting() {
    String pidString = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_START_PROC);
    long pid = -1;
    if (pidString != null) {
      pid = Long.valueOf(pidString);
    }

    if (pid == -1) {
      return false;
    } else {
      // Check if the process is still running
      File procDir = new File("/proc/" + String.valueOf(pid));
      return procDir.exists();
    }
  }

  public List<String> getLlapHosts() {
    ArrayList<String> hosts = new ArrayList<>();

    if (!isClusterUp() || isClusterStarting()) {
      return hosts;
    }

    // The cluster is app, so the appId exists
    String llapAppID = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_APP_ID);

    ApplicationId appId = ApplicationId.fromString(llapAppID);
    YarnClient yarnClient = yarnClientService.getYarnClientSuper(settings.getConfiguration()).getYarnClient();
    try {
      List<ApplicationAttemptReport> attempts = yarnClient.getApplicationAttempts(appId);
      ApplicationAttemptReport current = null;
      for (ApplicationAttemptReport attempt : attempts) {
        // Only if the app is running the metrics are available
        if (attempt.getYarnApplicationAttemptState() == YarnApplicationAttemptState.RUNNING) {
          current = attempt;
          break;
        }
      }

      if (current == null) {
        return hosts;
      }

      List<ContainerReport> containerReports = yarnClient.getContainers(current.getApplicationAttemptId());

      // For all the new/running containers, which are not the application master, get the host
      for (ContainerReport containerReport : containerReports) {
        // Only if the container is running the metrics are available
        if (containerReport.getContainerState() == ContainerState.RUNNING &&
            !containerReport.getContainerId().equals(current.getAMContainerId())) {
          hosts.add(containerReport.getAssignedNode().getHost());
        }
      }

    } catch (IOException | YarnException ex) {
      logger.log(Level.SEVERE, "Couldn't retrieve the containers for LLAP cluster", ex);
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    return hosts;
  }
}
