package io.hops.hopsworks.kmon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import io.hops.hopsworks.kmon.role.GroupServiceMapper;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import io.hops.hopsworks.kmon.struct.GroupType;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.kmon.utils.FilterUtils;

@ManagedBean
@RequestScoped
public class ServiceInstancesController {

  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.status}")
  private String status;
  @EJB
  private HostServicesFacade hostServicesFacade;
  private static final SelectItem[] statusOptions;
  private static final SelectItem[] healthOptions;
  private List<InstanceInfo> filteredInstances = new ArrayList<>();
  private static final Logger logger = Logger.getLogger(
      ServiceInstancesController.class.getName());

  private enum groupsWithMetrics {
    HDFS,
    YARN
  };
//   private CookieTools cookie = new CookieTools();

  static {
    statusOptions = FilterUtils.createFilterOptions(Status.values());
    healthOptions = FilterUtils.createFilterOptions(Health.values());
  }

  public ServiceInstancesController() {
    logger.info("ServiceInstancesController");
  }

  public String getService() {
    return this.service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public boolean isYarnService() {
    if (group != null) {
      try {
        if (GroupType.valueOf(group).equals(GroupType.YARN)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public boolean isHDFSService() {
    if (group != null) {
      try {
        if (GroupType.valueOf(group).equals(GroupType.HDFS)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public boolean getServiceWithMetrics() {
    if (group != null) {
      try {
        groupsWithMetrics.valueOf(group);
        return true;
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<InstanceInfo> getFilteredInstances() {
    return filteredInstances;
  }

  public void setFilteredInstances(List<InstanceInfo> filteredInstances) {
    this.filteredInstances = filteredInstances;
  }

  public SelectItem[] getStatusOptions() {
    return statusOptions;
  }

  public SelectItem[] getHealthOptions() {
    return healthOptions;
  }

  public SelectItem[] getServiceOptions() {
    try {
      return FilterUtils.createFilterOptions(GroupServiceMapper.getServicesArray(GroupType.valueOf(group)));
    } catch (Exception ex) {
      logger.log(Level.WARNING,
          "Service not found. Returning no option. Error message: {0}", ex.
              getMessage());
      return new SelectItem[]{};
    }
  }

  public List<InstanceInfo> getInstances() {
//      With prettyfaces, parameters (clusters, service, service) will not be null.
//      Without prettyfaces, parameters will be null when filter is changed, they
//      should be stored in cookie
    List<InstanceInfo> instances = new ArrayList<InstanceInfo>();
    List<HostServicesInfo> roleHostList = new ArrayList<HostServicesInfo>();
    if (cluster != null && group != null && group != null && status != null) {
      for (HostServicesInfo hostServicesInfo : hostServicesFacade.findHostServices(cluster, group, group)) {
        if (hostServicesInfo.getStatus() == Status.valueOf(status)) {
          roleHostList.add(hostServicesInfo);
        }
      }
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);         
    } else if (cluster != null && group != null && group != null) {
      roleHostList = hostServicesFacade.findHostServices(cluster, group, group);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);    
    } else if (cluster != null && group != null) {
      roleHostList = hostServicesFacade.findHostServices(cluster, group);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);          
    } else if (cluster != null) {
      roleHostList = hostServicesFacade.findHostServices(cluster);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);             
    }
//      else {
//         roleHostList = roleEjb.findRoleHost(cookie.read("cluster"), cookie.read("service"));
//      }     
    for (HostServicesInfo r : roleHostList) {
      instances.add(new InstanceInfo(r.getHostServices().getCluster(), r.getHostServices().
          getService(), r.getHostServices().getService(),
          r.getHostServices().getHost().getHostname(), r.getStatus(), r.getHealth().toString()));
    }
    filteredInstances.addAll(instances);
    return instances;
  }

  public boolean disableStart() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Stopped) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean disableStop() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Started) {
          return false;
        }
      }
    }
    return true;
  }
}
