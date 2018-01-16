package io.hops.hopsworks.kmon.service;

import io.hops.hopsworks.kmon.struct.GroupType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.kmon.role.GroupServiceMapper;
import io.hops.hopsworks.kmon.struct.ServiceType;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.kmon.struct.ServiceInstancesInfo;

@ManagedBean
@RequestScoped
public class GroupStatusController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private Health groupHealth;
  private List<ServiceInstancesInfo> groupServices = new ArrayList<ServiceInstancesInfo>();
  private static final Logger logger = Logger.getLogger(GroupStatusController.class.getName());

  public GroupStatusController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init ServiceStatusController");
//        loadRoles();
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public Health getHealth() {
    return groupHealth;
  }

  public List<ServiceInstancesInfo> getServices() {
    loadServices();
    return groupServices;
  }

  public boolean renderTerminalLink() {
    return group.equalsIgnoreCase(GroupType.HDFS.toString())
        || group.equalsIgnoreCase(GroupType.NDB.toString());
  }

  public boolean renderInstancesLink() {
//    return !group.equalsIgnoreCase(GroupType.Spark.toString());
    return true;
  }

  public boolean renderNdbInfoTable() {
    return group.equals(GroupType.NDB.toString());
  }

  public boolean renderLog() {
    return group.equals(GroupType.NDB.toString());
  }

  public boolean renderConfiguration() {
    return group.equals(GroupType.NDB.toString());
  }

  private void loadServices() {
    groupHealth = Health.Good;
    try {
      for (ServiceType role : GroupServiceMapper.getServices(group)) {
        groupServices.add(createRoleInstancesInfo(cluster, group, role));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Invalid service type: {0}", group);
    }
  }

  private ServiceInstancesInfo createRoleInstancesInfo(String cluster,
      String service, ServiceType role) {

    ServiceInstancesInfo roleInstancesInfo = 
        new ServiceInstancesInfo(GroupServiceMapper.getServiceFullName(role), role);
    List<HostServicesInfo> serviceHosts = hostServicesFacade.findHostServices(cluster, service, role.toString());
    for (HostServicesInfo serviceHost : serviceHosts) {
      roleInstancesInfo.addInstanceInfo(serviceHost.getStatus(), serviceHost.getHealth());
    }
    if (roleInstancesInfo.getOverallHealth() == Health.Bad) {
      groupHealth = Health.Bad;
    }
    return roleInstancesInfo;
  }
}
