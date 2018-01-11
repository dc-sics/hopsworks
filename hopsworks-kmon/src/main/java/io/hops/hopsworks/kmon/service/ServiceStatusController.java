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

package io.hops.hopsworks.kmon.service;

import io.hops.hopsworks.kmon.struct.ServiceType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.kmon.role.ServiceRoleMapper;
import io.hops.hopsworks.kmon.struct.RoleType;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.role.RoleHostInfo;
import io.hops.hopsworks.kmon.struct.RoleInstancesInfo;

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
  private List<RoleInstancesInfo> serviceRoles
          = new ArrayList<RoleInstancesInfo>();
  private static final Logger logger = Logger.getLogger(
          ServiceStatusController.class.getName());

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

  private RoleInstancesInfo createRoleInstancesInfo(String cluster,
          String service, RoleType role) {

    RoleInstancesInfo roleInstancesInfo = new RoleInstancesInfo(
            ServiceRoleMapper.getRoleFullName(role), role);
    List<RoleHostInfo> roleHosts = roleEjb.findRoleHost(cluster, service, role.
            toString());
    for (RoleHostInfo roleHost : roleHosts) {
      roleInstancesInfo.addInstanceInfo(roleHost.getStatus(), roleHost.
              getHealth());
    }
    if (roleInstancesInfo.getOverallHealth() == Health.Bad) {
      serviceHealth = Health.Bad;
    }
    return roleInstancesInfo;
  }
}
