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

package io.hops.hopsworks.admin.user.activity;

import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.LazyDataModel;
import io.hops.hopsworks.common.dao.project.Project;

@ManagedBean(name = "activityBean")
@SessionScoped
public class ActivityMB implements Serializable {

  private static final Logger logger = Logger.getLogger(ActivityMB.class.
          getName());
  private static final long serialVersionUID = 1L;

  @EJB
  private ActivityFacade activityFacade;

  private LazyDataModel<Activity> allLazyModel;

  @PostConstruct
  public void init() {
    try {
      this.allLazyModel = new LazyActivityModel(activityFacade);
      int cnt = (int) activityFacade.getTotalCount();
      allLazyModel.setRowCount(cnt);
    } catch (IllegalArgumentException e) {
      logger.log(Level.SEVERE, "Failed to initialize LazyActivityModel.", e);
      this.allLazyModel = null;
    }
  }

  /**
   * Get a Lazy data model containing all activities.
   * <p/>
   * @return
   */
  public LazyDataModel<Activity> getAllLazyModel() {
    return allLazyModel;
  }

  public String findLastActivity(int id) {

    Iterator<Activity> itr = activityFacade.activityOnID(id).
            listIterator();
    long currentTime = new Date().getTime();
    while (itr.hasNext()) {
      long fetchedTime = itr.next().getTimestamp().getTime();
      if ((currentTime - fetchedTime) / 1000 >= 0 && (currentTime - fetchedTime)
              / 1000 <= 20) {
        return String.format("less than a minute ago.");
      } else if ((currentTime - fetchedTime) / 1000 > 20 && (currentTime
              - fetchedTime) / 1000 <= 118) {
        return String.format("about %s minute ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 118 && (currentTime
              - fetchedTime) / 1000 < 1800) {
        return String.format("%s minutes ago.", (currentTime - fetchedTime)
                / 60000);
      } else if ((currentTime - fetchedTime) / 1000 > 1800 && (currentTime
              - fetchedTime) / 1000 <= 7056) {
        return String.format("about %s hour ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 7056 && (currentTime
              - fetchedTime) / 1000 <= 45400) {
        return String.format("%s hours ago.", (currentTime - fetchedTime)
                / 3600000);
      } else if ((currentTime - fetchedTime) / 1000 > 45400 && (currentTime
              - fetchedTime) / 1000 <= 170000) {
        return String.format("about %s day ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 170000 && (currentTime
              - fetchedTime) / 1000 <= 1300000) {
        return String.format("%s days ago.", (currentTime - fetchedTime)
                / 86400000);
      } else if ((currentTime - fetchedTime) / 1000 > 1300000 && (currentTime
              - fetchedTime) / 1000 <= 2500000) {
        return String.format("about %s month ago.", 1);
      } else if ((currentTime - fetchedTime) / 1000 > 2500000 && (currentTime
              - fetchedTime) / 1000 < 25000000) {
        return String.format("%s months ago.", (currentTime - fetchedTime)
                / 1000 / 2600000);
      } else {
        return String.format("about %s year ago.", 1);
      }
    }
    return "more than a year ago"; // dummy
  }

  public String findLastActivityOnProject(Project project) {

    Activity itr = activityFacade.lastActivityOnProject(project);
    long currentTime = new Date().getTime();

    long getLastUpdate = itr.getTimestamp().getTime();
    if ((currentTime - getLastUpdate) / 1000 >= 0 && (currentTime
            - getLastUpdate) / 1000 <= 20) {
      return String.format("less than a minute ago.");
    } else if ((currentTime - getLastUpdate) / 1000 > 20 && (currentTime
            - getLastUpdate) / 1000 <= 118) {
      return String.format("about %s minute ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 118 && (currentTime
            - getLastUpdate) / 1000 < 1800) {
      return String.format("%s minutes ago.", (currentTime - getLastUpdate)
              / 60000);
    } else if ((currentTime - getLastUpdate) / 1000 > 1800 && (currentTime
            - getLastUpdate) / 1000 <= 7056) {
      return String.format("about %s hour ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 7056 && (currentTime
            - getLastUpdate) / 1000 <= 45400) {
      return String.format("%s hours ago.", (currentTime - getLastUpdate)
              / 3600000);
    } else if ((currentTime - getLastUpdate) / 1000 > 45400 && (currentTime
            - getLastUpdate) / 1000 <= 170000) {
      return String.format("about %s day ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 170000 && (currentTime
            - getLastUpdate) / 1000 <= 1300000) {
      return String.format("%s days ago.", (currentTime - getLastUpdate)
              / 86400000);
    } else if ((currentTime - getLastUpdate) / 1000 > 1300000 && (currentTime
            - getLastUpdate) / 1000 <= 2500000) {
      return String.format("about %s month ago.", 1);
    } else if ((currentTime - getLastUpdate) / 1000 > 2500000 && (currentTime
            - getLastUpdate) / 1000 < 25000000) {
      return String.format("%s months ago.", (currentTime - getLastUpdate)
              / 1000 / 2600000);
    } else {
      return String.format("about %s year ago.", 1);
    }
  }

  public String getGravatar(String email, int size) {
    return Gravatar.getUrl(email, size);
  }

}
