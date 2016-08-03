package io.hops.kmon.role;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.struct.Health;
import io.hops.kmon.struct.InstanceFullInfo;
import io.hops.kmon.struct.RoleHostInfo;
import io.hops.kmon.struct.Status;
import io.hops.kmon.utils.FormatUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class RoleController {

  @EJB
  private RoleEJB roleEjb;
  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.role}")
  private String role;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private List<InstanceFullInfo> instanceInfoList;
  private String health;
  private boolean renderWebUi;
  private boolean found;
  private static final Logger logger = Logger.getLogger(RoleController.class.getName());

  public RoleController() {
  }

  @PostConstruct
  public void init() {
  }

  public void loadRoles() {

    logger.info("init RoleController");
    instanceInfoList = new ArrayList<InstanceFullInfo>();
    try {
      RoleHostInfo roleHost = roleEjb.findRoleHost(cluster, service, role, hostId);
      String ip = roleHost.getHost().getPublicOrPrivateIp();
      InstanceFullInfo info = new InstanceFullInfo(roleHost.getRole().getCluster(),
          roleHost.getRole().getService(), roleHost.getRole().getRole(),
          roleHost.getRole().getHostId(), ip, roleHost.getRole().getWebPort(),
          roleHost.getStatus(), roleHost.getHealth().toString());
      info.setPid(roleHost.getRole().getPid());
      String upTime = roleHost.getHealth() == Health.Good ? FormatUtils.time(roleHost.getRole().getUptime() * 1000) : "";
      info.setUptime(upTime);
      instanceInfoList.add(info);
      renderWebUi = roleHost.getRole().getWebPort() != null && roleHost.getRole().getWebPort() != 0;
      health = roleHost.getHealth().toString();
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

  public boolean getRenderWebUi() {
    return renderWebUi;
  }

  public List<InstanceFullInfo> getInstanceFullInfo() {
    return instanceInfoList;
  }

  public String roleLongName() {
    if (!instanceInfoList.isEmpty()) {
      return instanceInfoList.get(0).getName();
    }
    return null;
  }

  public boolean disableStart() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus() == Status.Stopped) {
      return false;
    }
    return true;
  }

  public boolean disableStop() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus() == Status.Started) {
      return false;
    }
    return true;
  }
}
