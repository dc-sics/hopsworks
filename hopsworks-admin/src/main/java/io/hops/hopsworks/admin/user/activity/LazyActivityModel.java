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

import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import io.hops.hopsworks.common.dao.project.Project;

/**
 * Model for lazily loading activity data into index page. Some explanations:
 * The code for PF LazyDataModel as it is right now does not
 * allow for usage of commandLink or commandButton in a p:datascroller.
 * <p>
 * Hence, here most of the methods of LazyDataModel are overridden. Basically,
 * all the data that has been loaded so far is saved and kept track of. Then
 * when the RowIndex is set, instead of taking it modulo the page size, it is
 * taken literally as an index to the data list. This ensures that the same data
 * is always returned, as is necessary when using an iterating JSF component.
 * <p>
 * Because setWrappedData is possibly called several times with the same data,
 * new data is stored in the load method.
 */
public class LazyActivityModel extends LazyDataModel<Activity> implements
        Serializable {

  private static final Logger logger = Logger.getLogger(LazyActivityModel.class.
          getName());

  private transient final ActivityFacade activityFacade;
  private List<Activity> data;
  private Project filterProject;
  private int rowIndex;

  public LazyActivityModel(ActivityFacade ac) throws
          IllegalArgumentException {
    this(ac, null);
  }

  public LazyActivityModel(ActivityFacade ac, Project filterProject) throws
          IllegalArgumentException {
    super();
    if (ac == null) {
      logger.log(Level.SEVERE,
              "Constructing lazy activity model with a null ActivityDetailFacade. Aborting.");
      throw new IllegalArgumentException("ActivityDetailFacade cannot be null.");
    }
    this.activityFacade = ac;
    this.filterProject = filterProject;
    data = new ArrayList<>();
  }

  @Override
  public List<Activity> load(int first, int pageSize, String sortField,
          SortOrder sortOrder, Map<String, Object> filters) {

    List<Activity> retData;

    // UNDO later: this gives an error while accessing indexPage from profile 
    if (filterProject == null) {
      retData = activityFacade.getPaginatedActivity(first, pageSize);
      //TODO: add support for sorting, filtering
    } else {
      retData = activityFacade.getPaginatedActivityForProject(first,
              pageSize, filterProject);
    }
    if (first == 0) {
      data = new ArrayList<>(retData);
      return retData;
    } else if (first >= data.size()) {
      data.addAll(retData);
      return retData;
    } else {
      return data.subList(first, Math.min(first + pageSize, data.size()));
    }
  }

  @Override
  public void setRowIndex(int index) {
    if (index >= data.size()) {
      index = -1;
    }
    this.rowIndex = index;
  }

  @Override
  public Activity getRowData() {
    return data.get(rowIndex);
  }

  /**
   * Overriden because default implementation checks super.data against null.
   *
   * @return
   */
  @Override
  public boolean isRowAvailable() {
    if (data == null) {
      return false;
    }
    return rowIndex >= 0 && rowIndex < data.size();
  }

}
