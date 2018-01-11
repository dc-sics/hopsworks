package io.hops.hopsworks.admin.jupyter;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean(name = "JupyterNotebooks")
@ViewScoped
public class JupyterNotebooksBean {

  private static final Logger LOGGER = Logger.getLogger(JupyterNotebooksBean.class.getName());

  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private JupyterProcessFacade jupyterProcessFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;

  public String action;

  private List<JupyterProject> filteredNotebooks;

  private List<JupyterProject> allNotebooks;

  public void setFilteredNotebooks(List<JupyterProject> filteredNotebooks) {
    this.filteredNotebooks = filteredNotebooks;
  }

  public List<JupyterProject> getFilteredNotebooks() {
    return filteredNotebooks;
  }

  public void setAllNotebooks(List<JupyterProject> allNotebooks) {
    this.allNotebooks = allNotebooks;
  }

  public List<JupyterProject> getAllNotebooks() {
    this.allNotebooks = jupyterProcessFacade.getAllNotebooks();
    return this.allNotebooks;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getHdfsUser(JupyterProject notebook) {
    int hdfsId = notebook.getHdfsUserId();
    if (hdfsId == -1) {
      return "Orphaned";
    }
    HdfsUsers hdfsUser = hdfsUsersFacade.find(hdfsId);
    return hdfsUser.getName();
  }

  public String kill(JupyterProject notebook) {
    String projectPath;
    String hdfsUser = getHdfsUser(notebook);
    if (hdfsUser.compareTo("Orphaned") == 0) {
      if (jupyterProcessFacade.killHardJupyterWithPid(notebook.getPid()) == -1) {
        return "KILL_NOTEBOOK_FAILED";
      }
    } else {
      try {
        projectPath = jupyterProcessFacade.getJupyterHome(hdfsUser, notebook);
        jupyterProcessFacade.killServerJupyterUser(projectPath, notebook.getPid(), notebook.getPort());
        jupyterFacade.removeNotebookServer(hdfsUser);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Successful", "Successfully killed Jupyter Notebook Server."));
      } catch (AppException ex) {
        Logger.getLogger(JupyterNotebooksBean.class.getName()).log(Level.SEVERE, null, ex);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Failure", "Failed to kill Jupyter Notebook Server."));
        return "KILL_NOTEBOOK_FAILED";
      }
    }
    return "KILL_NOTEBOOK_SUCCESS";
  }

}
