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

package io.hops.hopsworks.kmon.url;

import java.util.logging.Logger;
import javax.faces.application.NavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class UrlController {

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
  @ManagedProperty("#{param.target}")
  private String target;
  private static final Logger logger = Logger.getLogger(UrlController.class.
          getName());

  public UrlController() {
    logger.info("UrlController");
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

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String host() {
    return "host?faces-redirect=true&hostid=" + hostId;
  }

  public String clustersStatus(){
    return "clusters?faces-redirect=true";
  }
  
  public String clusterStatus() {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }

  public String clusterStatus(String cluster) {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }
  
  public String serviceInstance() {
    return "services-instances-status?faces-redirect=true&hostid="
            + hostId + "&cluster=" + cluster + "&role=" + role;
  }

  public String clusterActionHistory() {
    return "cluster-actionhistory?faces-redirect=true&cluster=" + cluster;
  }

  public String serviceStatus() {
    return "service-status?faces-redirect=true&cluster=" + cluster + "&service="
            + service;
  }

  public String serviceInstances() {
    String url = "service-instances?faces-redirect=true";
    if (hostId != null) {
      url += "&hostid=" + hostId;
    }
    if (cluster != null) {
      url += "&cluster=" + cluster;
    }
    if (service != null) {
      url += "&service=" + service;
    }
    if (role != null) {
      url += "&r=" + role;
    }
    if (status != null) {
      url += "&s=" + status;
    }
    return url;
  }

  public String serviceActionHistory() {
    return "service-actionhistory?faces-redirect=true&cluster=" + cluster
            + "&service=" + service;
  }

  public String serviceTerminal() {
    return "service-terminal?faces-redirect=true&cluster=" + cluster
            + "&service=" + service;
  }

  public String roleStatus() {
    return "role-status?faces-redirect=true&hostid=" + hostId + "&cluster="
            + cluster
            + "&service=" + service + "&role=" + role;
  }

  public String roleActionHistory() {
    return "role-actionhistory?faces-redirect=true&hostid=" + hostId
            + "&cluster=" + cluster
            + "&service=" + service + "&role=" + role;
  }

  public void redirectToEditGraphs() {
    String outcome = "edit-graphs?faces-redirect=true";
    if (target != null) {
      outcome += "&target=" + target;
    }
    FacesContext context = FacesContext.getCurrentInstance();
    NavigationHandler navigationHandler = context.getApplication().
            getNavigationHandler();
    navigationHandler.handleNavigation(context, null, outcome);
  }
}
