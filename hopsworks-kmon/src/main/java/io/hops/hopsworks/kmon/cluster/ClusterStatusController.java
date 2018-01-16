package io.hops.hopsworks.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.kmon.struct.ClusterInfo; 
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.ServiceInfo;

@ManagedBean
@RequestScoped
public class ClusterStatusController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private static final Logger logger = Logger.getLogger(ClusterStatusController.class.getName());
  private List<ServiceInfo> services = new ArrayList<>();
  private Health clusterHealth;
  private boolean found;
  private ClusterInfo clusterInfo;

  public ClusterStatusController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init ClusterStatusController");
    loadServices();
    loadCluster();
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

  public List<ServiceInfo> getServices() {
    return services;
  }

  public Health getClusterHealth() {
    loadCluster();
    return clusterHealth;
  }

  public void loadServices() {
    clusterHealth = Health.Good;
    List<String> servicesList = hostServicesFacade.findServices(cluster);
    if (!servicesList.isEmpty()) {
      found = true;
    }
    for (String s : servicesList) {
      ServiceInfo serviceInfo = new ServiceInfo(s);
      Health health = serviceInfo.addServices(hostServicesFacade.findHostServices(cluster, s));
      if (health == Health.Bad) {
        clusterHealth = Health.Bad;
      }
      services.add(serviceInfo);
    }
  }

  private void loadCluster() {
    if (clusterInfo != null) {
      return;
    }
    clusterInfo = new ClusterInfo(cluster);
    clusterInfo.setNumberOfHosts(hostServicesFacade.countHosts(cluster));
    clusterInfo.setTotalCores(hostServicesFacade.totalCores(cluster));
    clusterInfo.setTotalMemoryCapacity(hostServicesFacade.totalMemoryCapacity(cluster));
    clusterInfo.setTotalDiskCapacity(hostServicesFacade.totalDiskCapacity(cluster));
    clusterInfo.addServices(hostServicesFacade.findHostServices(cluster));
    found = true;
  }

  public ClusterInfo getClusterInfo() {
    loadCluster();
    return clusterInfo;
  }
}
