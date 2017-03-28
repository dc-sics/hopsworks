package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.host.Host;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class HostsController implements Serializable {

  @EJB
  private HostEJB hostEJB;
  private List<Host> hosts;
  private static final Logger logger = Logger.getLogger(HostsController.class.
          getName());

  public HostsController() {

  }

  @PostConstruct
  public void init() {
    logger.info("init HostsController");
    loadHosts();
  }

  public List<Host> getHosts() {
    loadHosts();
    return hosts;
  }

  private void loadHosts() {
    hosts = hostEJB.find();
    if (hosts == null) {
      hosts = new ArrayList<>();
    }
  }

}
