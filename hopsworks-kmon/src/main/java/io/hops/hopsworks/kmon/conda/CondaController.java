/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package io.hops.hopsworks.kmon.conda;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaStatus;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class CondaController implements Serializable {

  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private Settings settings;
  @EJB
  private WebCommunication web;
  @Resource(lookup = "concurrent/kagentExecutorService")
  ManagedExecutorService kagentExecutorService;

  private List<CondaCommands> failedCommands;
  private List<CondaCommands> ongoingCommands;
  private List<CondaCommands> newCommands;

  private String output;

  private boolean showFailed = true;
  private boolean showNew = false;
  private boolean showOngoing = false;

  private static final Logger logger = Logger.getLogger(CondaController.class.getName());

  public CondaController() {

  }

  @PostConstruct
  public void init() {
    logger.info("init CondaController");
    loadCommands();
  }

  public void deleteAllFailedCommands() {
    pythonDepsFacade.deleteAllCommandsByStatus(CondaStatus.FAILED);
    message("deleting all commands with the state 'failed'");
    loadFailedCommands();
  }

  public void deleteAllOngoingCommands() {
    pythonDepsFacade.deleteAllCommandsByStatus(CondaStatus.ONGOING);
    message("deleting all commands with the state 'ongoing'");
    loadOngoingCommands();
  }

  public void deleteAllNewCommands() {
    pythonDepsFacade.deleteAllCommandsByStatus(CondaStatus.NEW);
    message("deleting all commands with the state 'new'");
    loadNewCommands();
  }

  public void deleteCommand(CondaCommands command) {
    pythonDepsFacade.removeCondaCommand(command.getId());
    message("deleting");
    loadCommands();
  }

  public void execCommand(CondaCommands command) {
    // ssh to the host, run the command, print out the results to the terminal.

    Map<String, String> depVers = new HashMap<>();
    try {
      if (command.getStatus() != CondaStatus.FAILED) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "You can only execute failed commands.", "Not executed");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return;
      }

      if (command.getOp() == null) {
        this.output = "Conda command was null. Report a bug.";
      } else {
        message("executing");
        PythonDepsFacade.CondaOp op = command.getOp();
        if (op.isEnvOp()) {
          // anaconda environment command: <host> <op> <proj> <arg> <offline> <hadoop_home>
          String prog = settings.getHopsworksDomainDir() + "/bin/anaconda-command-ssh.sh";
          String hostname = command.getHostId().getHostIp();
          String projectName = command.getProj();
          String arg = command.getArg();
          String offline = "";
          String hadoopHome = settings.getHadoopSymbolicLinkDir();
          String[] scriptCommand = {prog, hostname, op.toString(), projectName, arg, offline, hadoopHome};
          String msg = String.join(" ", scriptCommand);
          logger.log(Level.INFO, "Executing: {0}", msg);
          ProcessBuilder pb = new ProcessBuilder(scriptCommand);
          Process process = pb.start();
          // Send both stdout and stderr to the same stream
          pb.redirectErrorStream(true);
          BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          StringBuilder sb = new StringBuilder();
          while ((line = br.readLine()) != null) {
            sb.append(line).append("\r\n");
          }
          boolean status = process.waitFor(600, TimeUnit.SECONDS);
          if (status == false) {
            this.output = "COMMAND TIMED OUT: \r\n" + sb.toString();
            return;
          }
          if (process.exitValue() == 0) {
            // delete from conda_commands tables
            command.setStatus(CondaStatus.SUCCESS);
            pythonDepsFacade.removeCondaCommand(command.getId());

            this.output = "SUCCESS. \r\n" + sb.toString();
            loadCommands();

            try {
              pythonDepsFacade.updateCondaCommandStatus(command.getId(), CondaStatus.SUCCESS, command.getInstallType(),
                  command.getMachineType(), command.getArg(), command.getProj(), command.getOp(), command.getLib(),
                  command.getVersion(), command.getChannelUrl());
            } catch (AppException ex) {
              Logger.getLogger(CondaController.class.getName()).log(Level.SEVERE, null, ex);
            }
          } else {
            this.output = "FAILED. \r\n" + sb.toString();
          }
        } else { // Conda operation
          Hosts host = command.getHostId();
          Project proj = command.getProjectId();
          String prog = settings.getHopsworksDomainDir() + "/bin/conda-command-ssh.sh";
          String hostname = command.getHostId().getHostIp();
          String projectName = command.getProj();
          String arg = command.getArg();
          String channelUrl = command.getChannelUrl();
          String[] scriptCommand = {prog, hostname, op.toString(), projectName, channelUrl, command.getInstallType().
            toString(), command.getLib(), command.getVersion()};
          String msg = String.join(" ", scriptCommand);
          logger.log(Level.INFO, "Executing: {0}", msg);
          ProcessBuilder pb = new ProcessBuilder(scriptCommand);
          Process process = pb.start();
          pb.redirectErrorStream(true); // Send both stdout and stderr to the same stream
          BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          StringBuilder sb = new StringBuilder();
          while ((line = br.readLine()) != null) {
            sb.append(line).append("\r\n");
          }
          boolean status = process.waitFor(600, TimeUnit.SECONDS);
          if (status == false) {
            this.output = "COMMAND TIMED OUT: \r\n" + sb.toString();
            return;
          }
          if (process.exitValue() == 0) {
            // delete from conda_commands tables
            command.setStatus(CondaStatus.SUCCESS);
            pythonDepsFacade.removeCondaCommand(command.getId());
            this.output = "SUCCESS. \r\n" + sb.toString();
            loadCommands();
            try {
              pythonDepsFacade.updateCondaCommandStatus(command.getId(), CondaStatus.SUCCESS, command.getInstallType(),
                  command.getMachineType(), command.getArg(), command.getProj(), command.getOp(), command.getLib(),
                  command.getVersion(), command.getChannelUrl());
            } catch (AppException ex) {
              Logger.getLogger(CondaController.class.getName()).log(Level.SEVERE, null, ex);
            }
          } else {
            this.output = "FAILED. \r\n" + sb.toString();
          }
        }
      }
      logger.log(Level.INFO, "Output: {0}", this.output);

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  public List<CondaCommands> getFailedCondaCommands() {
    loadFailedCommands();
    return failedCommands;
  }

  public List<CondaCommands> getOngoingCondaCommands() {
    loadOngoingCommands();
    return ongoingCommands;
  }

  public List<CondaCommands> getNewCondaCommands() {
    loadNewCommands();
    return newCommands;
  }

  private void loadFailedCommands() {
    failedCommands = pythonDepsFacade.findByStatus(PythonDepsFacade.CondaStatus.FAILED);
    if (failedCommands == null) {
      failedCommands = new ArrayList<>();
    }
  }

  private void loadOngoingCommands() {
    ongoingCommands = pythonDepsFacade.findByStatus(PythonDepsFacade.CondaStatus.ONGOING);
    if (ongoingCommands == null) {
      ongoingCommands = new ArrayList<>();
    }
  }

  private void loadNewCommands() {
    newCommands = pythonDepsFacade.findByStatus(PythonDepsFacade.CondaStatus.NEW);
    if (newCommands == null) {
      newCommands = new ArrayList<>();
    }
  }

  private void loadCommands() {
    loadNewCommands();
    loadOngoingCommands();
    loadFailedCommands();
  }

  public List<CondaCommands> getFailedCommands() {
    return failedCommands;
  }

  public List<CondaCommands> getOngoingCommands() {
    return ongoingCommands;
  }

  public List<CondaCommands> getNewCommands() {
    return newCommands;
  }

  public String getOutput() {
    if (!isOutput()) {
      return "No Output to show for command executions.";
    }
    return this.output;
  }

  public boolean isOutput() {
    if (this.output == null || this.output.isEmpty()) {
      return false;
    }
    return true;
  }

  public boolean isShowFailed() {
    return showFailed;
  }

  public boolean isShowNew() {
    return showNew;
  }

  public boolean isShowOngoing() {
    return showOngoing;
  }

  public void setShowFailed(boolean showFailed) {
    this.showFailed = showFailed;
  }

  public void setShowNew(boolean showNew) {
    this.showNew = showNew;
  }

  public void setShowOngoing(boolean showOngoing) {
    this.showOngoing = showOngoing;
  }

  public void message(String msg) {
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Conda Command " + msg));
  }
}
