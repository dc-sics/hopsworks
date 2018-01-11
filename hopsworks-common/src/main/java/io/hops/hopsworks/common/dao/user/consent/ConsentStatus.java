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

public enum ConsentStatus {

  APPROVED("Approved"),
  PENDING("Pending"),
  REJECTED("Rejected"),
  UNDEFINED("Undefined");

  private final String readable;

  private ConsentStatus(String readable) {
    this.readable = readable;
  }

  public static ConsentStatus create(String str) {
    if (str.compareTo(APPROVED.toString()) == 0) {
      return APPROVED;
    }
    if (str.compareTo(PENDING.toString()) == 0) {
      return PENDING;
    }
    if (str.compareTo(REJECTED.toString()) == 0) {
      return REJECTED;
    }
    return UNDEFINED;
  }

  @Override
  public String toString() {
    return readable;
  }

}
