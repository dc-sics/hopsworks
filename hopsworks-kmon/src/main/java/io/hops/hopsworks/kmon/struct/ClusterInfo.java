package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.role.RoleHostInfo;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import io.hops.hopsworks.common.util.FormatUtils;

public class ClusterInfo {

  private String name;
  private Long numberOfHosts;
  private Long totalCores;
  private Long totalMemoryCapacity;
  private Long totalDiskCapacity;
  private Set<String> services = new HashSet<>();
  private Set<String> roles = new HashSet<>();
  private Set<String> badServices = new HashSet<>();
  private Set<String> badRoles = new HashSet<>();
  private Map<String, Integer> rolesCount = new TreeMap<>();
  private Map<String, String> rolesServicesMap = new TreeMap<>();
  private Integer started, stopped, timedOut;

  public ClusterInfo(String name) {
    started = 0;
    stopped = 0;
    timedOut = 0;
    this.name = name;
  }

  public void setNumberOfHost(Long numberOfHosts) {
    this.numberOfHosts = numberOfHosts;
  }

  public Long getNumberOfMachines() {
    return numberOfHosts;
  }

  public String getName() {
    return name;
  }

  public String[] getServices() {
    return services.toArray(new String[services.size()]);
  }

  public String[] getRoles() {
    return roles.toArray(new String[roles.size()]);
  }

  public Long getTotalCores() {
    return totalCores;
  }

  public void setTotalCores(Long totalCores) {
    this.totalCores = totalCores;
  }

  public Integer roleCount(String role) {
    return rolesCount.get(role);
  }

  public String roleService(String role) {
    return rolesServicesMap.get(role);
  }

  public Health getClusterHealth() {
    if (badRoles.isEmpty()) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public Health serviceHealth(String service) {
    if (badServices.contains(service)) {
      return Health.Bad;
    }
//      return Health.None;
    return Health.Good;
  }

  public Health roleHealth(String role) {
    if (badRoles.contains(role)) {
      return Health.Bad;
    }
//      return Health.None;
    return Health.Good;
  }

  public Map getStatus() {

    Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
    if (started > 0) {
      statusMap.put(Status.Started, started);
    }
    if (stopped > 0) {
      statusMap.put(Status.Stopped, stopped);
    }
    if (timedOut > 0) {
      statusMap.put(Status.TimedOut, timedOut);
    }
    return statusMap;
  }

  public void addRoles(List<RoleHostInfo> roleHostList) {
    for (RoleHostInfo roleHost : roleHostList) {
      services.add(roleHost.getRole().getService());
      if (roleHost.getRole().getRole().toString().equals("")) {
        continue;
      }
      roles.add(roleHost.getRole().getRole());
      rolesServicesMap.put(roleHost.getRole().getRole(), roleHost.getRole().
              getService());
      if (roleHost.getStatus() == Status.Started) {
        started += 1;
      } else {
        badServices.add(roleHost.getRole().getService());
        badRoles.add(roleHost.getRole().getRole());
        if (roleHost.getStatus() == Status.Stopped) {
          stopped += 1;
        } else if (roleHost.getStatus() == Status.TimedOut) {
          timedOut += 1;
        }
      }
      addRole(roleHost.getRole().getRole());
    }
  }

  private void addRole(String role) {
    if (rolesCount.containsKey(role)) {
      Integer current = rolesCount.get(role);
      rolesCount.put(role, current + 1);
    } else {
      rolesCount.put(role, 1);
    }
  }

  public String getTotalMemoryCapacity() {
    if (totalMemoryCapacity == null) {
      return "N/A";
    }
    return FormatUtils.storage(totalMemoryCapacity);
  }

  public void setTotalMemoryCapacity(Long totalMemoryCapacity) {
    this.totalMemoryCapacity = totalMemoryCapacity;
  }

  public String getTotalDiskCapacity() {
    if (totalDiskCapacity == null) {
      return "N/A";
    }
    return FormatUtils.storage(totalDiskCapacity);
  }

  public void setTotalDiskCapacity(Long totalDiskCapacity) {
    this.totalDiskCapacity = totalDiskCapacity;
  }
}
