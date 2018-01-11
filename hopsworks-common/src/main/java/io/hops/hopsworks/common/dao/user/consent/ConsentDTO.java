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

package io.hops.hopsworks.common.dao.user.consent;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConsentDTO implements Serializable {

  private String path;
  private String consentType;
  private String consentStatus;

  public ConsentDTO() {
  }

  public ConsentDTO(String path) {
    this.path = path;
    this.consentType = ConsentType.UNDEFINED.toString();
    this.consentStatus = ConsentStatus.UNDEFINED.toString();
  }

  public ConsentDTO(String name, ConsentType consentType,
          ConsentStatus consentStatus) {
    this.path = name;
    this.consentType = consentType.toString();
    this.consentStatus = consentStatus.toString();
  }

  public String getConsentStatus() {
    return consentStatus;
  }

  public void setConsentStatus(String consentStatus) {
    this.consentStatus = consentStatus;
  }

  public String getConsentType() {
    return consentType;
  }

  public void setConsentType(String consentType) {
    this.consentType = consentType;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String name) {
    this.path = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConsentDTO)) {
      return false;
    }
    ConsentDTO other = (ConsentDTO) obj;
    return !((this.path == null && other.path != null) || (this.path != null
            && (this.path.compareToIgnoreCase(other.path) != 0)));
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.path);
    return hash;
  }

  @Override
  public String toString() {
    return "Path: " + path + " ; type: " + consentType + " ; status: "
            + consentStatus;
  }

}
