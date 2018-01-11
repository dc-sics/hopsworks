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

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import io.hops.hopsworks.common.dao.role.RoleEJB;

@ManagedBean
@RequestScoped
public class ServiceController {

  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.role}")
  private String role;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.status}")
  private String status;
  @EJB
  private RoleEJB roleEjb;

  private static final Logger logger = Logger.getLogger(ServiceController.class.
          getName());

  public ServiceController() {

  }

  @PostConstruct
  public void init() {
    logger.info("init ServiceController");
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getHostId() {
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

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public boolean isServiceFound() {
    return roleEjb.countRoles(cluster, service) > 0;
  }

  public void addMessage(String summary) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary,
            null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public void startService() {
    addMessage("Start not implemented!");
  }

  public void stopService() {
    addMessage("Stop not implemented!");
  }

  public void restartService() {
    addMessage("Restart not implemented!");
  }

  public void deleteService() {
    addMessage("Delete not implemented!");
  }

}
