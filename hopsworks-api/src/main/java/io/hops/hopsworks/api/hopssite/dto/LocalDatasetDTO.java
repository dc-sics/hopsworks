package io.hops.hopsworks.api.hopssite.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LocalDatasetDTO {
  private int InodeId;
  private String name;
  private String description;
  private String projectName;

  public LocalDatasetDTO() {
  }

  public LocalDatasetDTO(int InodeId, String projectName) {
    this.InodeId = InodeId;
    this.projectName = projectName;
  }

  public LocalDatasetDTO(int InodeId, String name, String description, String projectName) {
    this.InodeId = InodeId;
    this.name = name;
    this.description = description;
    this.projectName = projectName;
  }

  public int getInodeId() {
    return InodeId;
  }

  public void setInodeId(int InodeId) {
    this.InodeId = InodeId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }  
  
}
