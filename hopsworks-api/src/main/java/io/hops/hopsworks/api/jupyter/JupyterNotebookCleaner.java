package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyService;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterConfigFactory;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;

@Singleton
public class JupyterNotebookCleaner {

  private final static Logger LOGGER = Logger.getLogger(
          JupyterNotebookCleaner.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  LivyService livyService;

  @EJB
  Settings settings;

  @EJB
  JupyterFacade jupyterFacade;

  @EJB
  JupyterConfigFactory jupyterConfigFactory;

  // Run once per hour
  @Schedule(persistent = false,
          minute = "0",
          hour = "*")
  public void execute(Timer timer) {

    // 1. Get all Running Jupyter Notebook Servers
    // 2. For each running Notebook Server, get the project_user and
    // then get the Livy sessions for that project_user
    // 3. For each livy session, check 
  }

}
