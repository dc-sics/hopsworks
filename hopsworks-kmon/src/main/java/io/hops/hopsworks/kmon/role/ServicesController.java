package io.hops.hopsworks.kmon.role;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.InstanceFullInfo;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.common.util.FormatUtils;

@ManagedBean
@RequestScoped
public class ServicesController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.role}")
  private String role;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private List<InstanceFullInfo> instanceInfoList = new ArrayList<>();
  private String health;
//  private boolean renderWebUi;
  private boolean found;
  private static final Logger logger = Logger.getLogger(ServicesController.class.
          getName());

  public ServicesController() {
  }

  @PostConstruct
  public void init() {
  }

  public void loadRoles() {

    instanceInfoList.clear();
    logger.info("init RoleController");
    try {
      HostServicesInfo serviceHost = hostServicesFacade.findHostServices(cluster, service, role,
              hostId);
      String ip = serviceHost.getHost().getPublicOrPrivateIp();
      InstanceFullInfo info = new InstanceFullInfo(serviceHost.getHostServices().
              getCluster(),
              serviceHost.getHostServices().getService(), serviceHost.getHostServices().getService(),
              serviceHost.getHostServices().getHost().getHostname(), ip, 0,
//          roleHost.getRole().getWebPort(),
              serviceHost.getStatus(), serviceHost.getHealth().toString());
      info.setPid(serviceHost.getHostServices().getPid());
      String upTime = serviceHost.getHealth() == Health.Good ? FormatUtils.time(
              serviceHost.getHostServices().getUptime() * 1000) : "";
      info.setUptime(upTime);
      instanceInfoList.add(info);
//      renderWebUi = roleHost.getRole().getWebPort() != null && roleHost.
//              getRole().getWebPort() != 0;
      health = serviceHost.getHealth().toString();
      found = true;
    } catch (Exception ex) {
      logger.warning("init RoleController: ".concat(ex.getMessage()));
    }

  }

  public String getHealth() {
    return health;
  }

  public String getRole() {
    loadRoles();
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getService() {
    loadRoles();
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getHostId() {
    loadRoles();
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public boolean isFound() {
    return found;
  }

  public void setFound(boolean found) {
    this.found = found;
  }

//  public boolean getRenderWebUi() {
//    return renderWebUi;
//  }

  public List<InstanceFullInfo> getInstanceFullInfo() {
    loadRoles();
    return instanceInfoList;
  }

  public String roleLongName() {
    if (!instanceInfoList.isEmpty()) {
      return instanceInfoList.get(0).getName();
    }
    return null;
  }

  public boolean disableStart() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus()
            == Status.Stopped) {
      return false;
    }
    return true;
  }

  public boolean disableStop() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus()
            == Status.Started) {
      return false;
    }
    return true;
  }
}
