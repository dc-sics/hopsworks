/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.kmon.struct.ClusterInfo;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.ServiceInfo;

@ManagedBean
@RequestScoped
public class ClusterStatusController {

  @EJB
  private RoleEJB roleEjb;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private static final Logger logger = Logger.getLogger(
          ClusterStatusController.class.getName());
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
    List<String> servicesList = roleEjb.findServices(cluster);
    if (!servicesList.isEmpty()) {
      found = true;
    }
    for (String s : servicesList) {
      ServiceInfo serviceInfo = new ServiceInfo(s);
      Health health = serviceInfo.addRoles(roleEjb.findRoleHost(cluster, s));
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
    clusterInfo.setNumberOfHost(roleEjb.countHosts(cluster));
    clusterInfo.setTotalCores(roleEjb.totalCores(cluster));
    clusterInfo.setTotalMemoryCapacity(roleEjb.totalMemoryCapacity(cluster));
    clusterInfo.setTotalDiskCapacity(roleEjb.totalDiskCapacity(cluster));
    clusterInfo.addRoles(roleEjb.findRoleHost(cluster));
    found = true;
  }

  public ClusterInfo getClusterInfo() {
    loadCluster();
    return clusterInfo;
  }
}
