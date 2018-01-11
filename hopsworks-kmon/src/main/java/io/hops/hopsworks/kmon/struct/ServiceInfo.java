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

package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.role.RoleHostInfo;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ServiceInfo {

  private String name;
  private Health health;
  private Set<String> roles = new HashSet<>();
  private Map<String, Integer> rolesCount = new HashMap<>();
  private Set<String> badRoles = new HashSet<>();
  private int started;
  private int stopped;
  private int timedOut;

  public ServiceInfo(String name) {
    started = 0;
    stopped = 0;
    timedOut = 0;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map getStatus() {

    Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
    if (started > 0) {
      statusMap.put(Status.Started, started);
    }
    if (stopped > 0) {
      statusMap.put(Status.Stopped, stopped);
    }
    if (timedOut > 0) {
      statusMap.put(Status.TimedOut, timedOut);
    }
    return statusMap;
  }

  public Health getHealth() {
    return health;
  }

  public Integer roleCount(String role) {
    return rolesCount.get(role);
  }

  public String[] getRoles() {
    return roles.toArray(new String[rolesCount.size()]);
  }

  public Health roleHealth(String role) {
    if (badRoles.contains(role)) {
      return Health.Bad;
    }
//      return Health.None;
    return Health.Good;
  }

  public Health addRoles(List<RoleHostInfo> roles) {
    for (RoleHostInfo roleHostInfo : roles) {
      if (roleHostInfo.getRole().getRole().equals("")) {
        continue;
      }
      this.roles.add(roleHostInfo.getRole().getRole());
      if (roleHostInfo.getStatus() == Status.Started) {
        started += 1;
      } else {
        badRoles.add(roleHostInfo.getRole().getRole());
        if (roleHostInfo.getStatus() == Status.Stopped) {
          stopped += 1;
        } else {
          timedOut += 1;
        }
      }
      addRole(roleHostInfo.getRole().getRole());
    }
    health = (stopped + timedOut > 0) ? Health.Bad : Health.Good;
    return health;
  }

  private void addRole(String role) {
    if (rolesCount.containsKey(role)) {
      Integer current = rolesCount.get(role);
      rolesCount.put(role, current + 1);
    } else {
      rolesCount.put(role, 1);
    }
  }
}
