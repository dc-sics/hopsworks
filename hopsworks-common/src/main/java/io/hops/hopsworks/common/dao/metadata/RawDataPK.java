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

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class RawDataPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tupleid")
  private int tupleid;

  public RawDataPK() {
  }

  public RawDataPK(int fieldid, int tupleid) {
    this.fieldid = fieldid;
    this.tupleid = tupleid;
  }

  public void copy(RawDataPK rawdataPK) {
    this.fieldid = rawdataPK.getFieldid();
    this.tupleid = rawdataPK.getTupleid();
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
  }

  public void setTupleid(int tupleid) {
    this.tupleid = tupleid;
  }

  public int getFieldid() {
    return this.fieldid;
  }

  public int getTupleid() {
    return this.tupleid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) this.fieldid;
    hash += (int) this.tupleid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RawDataPK)) {
      return false;
    }
    RawDataPK other = (RawDataPK) object;

    if (this.fieldid != other.fieldid) {
      return false;
    }

    return this.tupleid == other.tupleid;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.RawDataPK[ fieldid="
            + this.fieldid + ", tupleid= " + this.tupleid + " ]";
  }
}
