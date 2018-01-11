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

package io.hops.hopsworks.common.dao.user.security;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.people_group")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "PeopleGroup.findAll",
          query = "SELECT p FROM PeopleGroup p"),
  @NamedQuery(name = "PeopleGroup.findByUid",
          query = "SELECT p FROM PeopleGroup p WHERE p.peopleGroupPK.uid = :uid"),
  @NamedQuery(name = "PeopleGroup.findByGid",
          query = "SELECT p FROM PeopleGroup p WHERE p.peopleGroupPK.gid = :gid")})
public class PeopleGroup implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected PeopleGroupPK peopleGroupPK;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Users user;

  public PeopleGroup() {
  }

  public PeopleGroup(PeopleGroupPK peopleGroupPK) {
    this.peopleGroupPK = peopleGroupPK;
  }

  public PeopleGroup(int uid, int gid) {
    this.peopleGroupPK = new PeopleGroupPK(uid, gid);
  }

  public PeopleGroupPK getPeopleGroupPK() {
    return peopleGroupPK;
  }

  public void setPeopleGroupPK(PeopleGroupPK peopleGroupPK) {
    this.peopleGroupPK = peopleGroupPK;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (peopleGroupPK != null ? peopleGroupPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PeopleGroup)) {
      return false;
    }
    PeopleGroup other = (PeopleGroup) object;
    if ((this.peopleGroupPK == null && other.peopleGroupPK != null)
            || (this.peopleGroupPK != null && !this.peopleGroupPK.equals(
                    other.peopleGroupPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.PeopleGroup[ peopleGroupPK="
            + peopleGroupPK + " ]";
  }

}
