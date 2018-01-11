package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.users.UserView;
import io.hops.hopsworks.common.dao.project.Project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LimitedProjectView {
  private Integer projectId;
  private String description;
  private String name;
  private UserView owner;
  
  public LimitedProjectView(){}
  
  public LimitedProjectView(Project project){
    this.projectId = project.getId();
    this.description = project.getDescription();
    this.name = project.getName();
    this.owner = new UserView(project.getOwner());
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
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public UserView getOwner() {
    return owner;
  }
  
  public void setOwner(UserView owner) {
    this.owner = owner;
  }
}
