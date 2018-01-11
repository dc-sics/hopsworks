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

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "jupyter_interpreter",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JupyterInterpreter.findAll",
          query
          = "SELECT j FROM JupyterInterpreter j")
  ,
        @NamedQuery(name = "JupyterInterpreter.findByPort",
          query
          = "SELECT j FROM JupyterInterpreter j WHERE j.jupyterInterpreterPK.port = :port")
  ,
        @NamedQuery(name = "JupyterInterpreter.findByName",
          query
          = "SELECT j FROM JupyterInterpreter j WHERE j.jupyterInterpreterPK.name = :name")
  ,
        @NamedQuery(name = "JupyterInterpreter.findByCreated",
          query
          = "SELECT j FROM JupyterInterpreter j WHERE j.created = :created")
  ,
        @NamedQuery(name = "JupyterInterpreter.findByLastAccessed",
          query
          = "SELECT j FROM JupyterInterpreter j WHERE j.lastAccessed = :lastAccessed")})
public class JupyterInterpreter implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected JupyterInterpreterPK jupyterInterpreterPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Column(name = "last_accessed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastAccessed;
  @JoinColumn(name = "port",
          referencedColumnName = "port",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private JupyterProject jupyterProject;

  public JupyterInterpreter() {
  }

  public JupyterInterpreter(JupyterInterpreterPK jupyterInterpreterPK) {
    this.jupyterInterpreterPK = jupyterInterpreterPK;
  }

  public JupyterInterpreter(JupyterInterpreterPK jupyterInterpreterPK,
          Date created, Date lastAccessed) {
    this.jupyterInterpreterPK = jupyterInterpreterPK;
    this.created = created;
    this.lastAccessed = lastAccessed;
  }

  public JupyterInterpreter(int port, String name) {
    this.jupyterInterpreterPK = new JupyterInterpreterPK(port, name);
  }

  public JupyterInterpreterPK getJupyterInterpreterPK() {
    return jupyterInterpreterPK;
  }

  public void setJupyterInterpreterPK(JupyterInterpreterPK jupyterInterpreterPK) {
    this.jupyterInterpreterPK = jupyterInterpreterPK;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastAccessed() {
    return lastAccessed;
  }

  public void setLastAccessed(Date lastAccessed) {
    this.lastAccessed = lastAccessed;
  }

  public JupyterProject getJupyterProject() {
    return jupyterProject;
  }

  public void setJupyterProject(JupyterProject jupyterProject) {
    this.jupyterProject = jupyterProject;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jupyterInterpreterPK != null ? jupyterInterpreterPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JupyterInterpreter)) {
      return false;
    }
    JupyterInterpreter other = (JupyterInterpreter) object;
    if ((this.jupyterInterpreterPK == null && other.jupyterInterpreterPK != null) ||
            (this.jupyterInterpreterPK != null &&
            !this.jupyterInterpreterPK.equals(other.jupyterInterpreterPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.config.JupyterInterpreter[ jupyterInterpreterPK=" +
            jupyterInterpreterPK + " ]";
  }

}
