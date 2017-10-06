/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.users.UserView;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.activity.Activity;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ProjectView {
  
  private List<ProjectTeam> team;
  private List<ProjectServices> services;
  private Integer projectId;
  private String description;
  private Date created;
  private String ethicalStatus;
  private boolean archived;
  private String name;
  private UserView owner;
  private List<Activity> activity;
  private List<Dataset> datasets;
  
  public ProjectView(){}
  
  public ProjectView(Project project){
    this.projectId = project.getId();
    this.name = project.getName();
    this.ethicalStatus = project.getEthicalStatus();
    this.created = project.getCreated();
    this.description = project.getDescription();
    this.owner = new UserView(project.getOwner());
    this.activity = new ArrayList<>(project.getActivityCollection());
    this.datasets = new ArrayList<>(project.getDatasetCollection());
    this.team = new ArrayList<>(project.getProjectTeamCollection());
    this.services = new ArrayList<>(project.getProjectServicesCollection());
    this.archived = project.getArchived();
  }
  
  public List<ProjectTeam> getTeam() {
    return team;
  }
  
  public List<ProjectServices> getServices() {
    return services;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Date getCreated() {
    return created;
  }
  
  public void setCreated(Date created) {
    this.created = created;
  }
  
  public String getEthicalStatus() {
    return ethicalStatus;
  }
  
  public void setEthicalStatus(String ethicalStatus) {
    this.ethicalStatus = ethicalStatus;
  }
  
  public boolean isArchived() {
    return archived;
  }
  
  public void setArchived(boolean archived) {
    this.archived = archived;
  }
  
  public String getName() {
    return name;
  }
  
  public UserView getOwner() {
    return owner;
  }
  
  public List<Activity> getActivity() {
    return activity;
  }
  
  public List<Dataset> getDatasets() {
    return datasets;
  }
}
