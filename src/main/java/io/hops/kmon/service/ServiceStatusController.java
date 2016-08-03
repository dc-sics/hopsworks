package io.hops.kmon.service;

import io.hops.kmon.struct.ServiceType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.role.RoleEJB;
import io.hops.kmon.role.ServiceRoleMapper;
import io.hops.kmon.struct.RoleType;
import io.hops.kmon.struct.Health;
import io.hops.kmon.struct.RoleHostInfo;
import io.hops.kmon.struct.RoleInstancesInfo;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ServiceStatusController {

  @EJB
  private RoleEJB roleEjb;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private Health serviceHealth;
  private List<RoleInstancesInfo> serviceRoles = new ArrayList<RoleInstancesInfo>();
  private static final Logger logger = Logger.getLogger(ServiceStatusController.class.getName());

  public ServiceStatusController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init ServiceStatusController");
//        loadRoles();
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public Health getHealth() {
    return serviceHealth;
  }

  public List<RoleInstancesInfo> getRoles() {
    loadRoles();
    return serviceRoles;
  }

  public boolean renderTerminalLink() {
    return service.equalsIgnoreCase(ServiceType.HDFS.toString())
        || service.equalsIgnoreCase(ServiceType.NDB.toString());
  }

  public boolean renderInstancesLink() {
//    return !service.equalsIgnoreCase(ServiceType.Spark.toString());
    return true;
  }

  public boolean renderNdbInfoTable() {
    return service.equals(ServiceType.NDB.toString());
  }

  public boolean renderLog() {
    return service.equals(ServiceType.NDB.toString());
  }

  public boolean renderConfiguration() {
    return service.equals(ServiceType.NDB.toString());
  }

  private void loadRoles() {
    serviceHealth = Health.Good;
    try {
      for (RoleType role : ServiceRoleMapper.getRoles(service)) {
        serviceRoles.add(createRoleInstancesInfo(cluster, service, role));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Invalid service type: {0}", service);
    }
  }

  private RoleInstancesInfo createRoleInstancesInfo(String cluster, String service, RoleType role) {

    RoleInstancesInfo roleInstancesInfo = new RoleInstancesInfo(ServiceRoleMapper.getRoleFullName(role), role);
    List<RoleHostInfo> roleHosts = roleEjb.findRoleHost(cluster, service, role.toString());
    for (RoleHostInfo roleHost : roleHosts) {
      roleInstancesInfo.addInstanceInfo(roleHost.getStatus(), roleHost.getHealth());
    }
    if (roleInstancesInfo.getOverallHealth() == Health.Bad) {
      serviceHealth = Health.Bad;
    }
    return roleInstancesInfo;
  }
}
