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

package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.host.Hosts;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.context.RequestContext;
import io.hops.hopsworks.common.dao.command.CommandEJB;

@ManagedBean
@RequestScoped
public class AddHostController implements Serializable {

  @EJB
  private HostEJB hostEJB;
  @EJB
  private CommandEJB commandEJB;
  private String hostId;
  private String privateIp;
  private String publicIp;
  private static final Logger logger = Logger.getLogger(AddHostController.class.
          getName());

  public AddHostController() {
    logger.info("AddHostController");
  }

  public void addHost(ActionEvent actionEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage msg;
    if (hostEJB.hostExists(hostId)) {
      logger.log(Level.INFO, "Host with id {0} already exists.", hostId);
      msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Host Exists",
              "A host with id " + hostId + " already exists.");
      context.addMessage(null, msg);
    } else {
      Hosts host = new Hosts();
      host.setHostname(hostId);
      host.setPrivateIp(privateIp);
      host.setPublicIp(publicIp);
      host.setHostIp("");
      hostEJB.storeHost(host, true);
      RequestContext reqInstace = RequestContext.getCurrentInstance();
      reqInstace.addCallbackParam("hostadded", true);
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Host Added",
              "Host " + hostId + " added successfully.");
      context.addMessage(null, msg);
      resetValues();
    }
  }

  private void resetValues() {
    hostId = "";
    privateIp = "";
    publicIp = "";
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public String getPrivateIp() {
    return privateIp;
  }

  public void setPrivateIp(String privateIp) {
    this.privateIp = privateIp;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public void setPublicIp(String publicIp) {
    this.publicIp = publicIp;
  }
}
