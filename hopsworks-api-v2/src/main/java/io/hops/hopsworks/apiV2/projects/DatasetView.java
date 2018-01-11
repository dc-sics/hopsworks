package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.common.dao.dataset.Dataset;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DatasetView {
  private Integer id;
  private String name;
  private String description;
  private boolean isShared;
  
  public DatasetView(){
  }
  
  public DatasetView(Dataset ds){
    this.id = ds.getId();
    this.name = ds.getName();
    this.description = ds.getDescription();
    this.isShared = ds.isShared();
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
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
  
  public boolean isShared() {
    return isShared;
  }
  
  public void setShared(boolean shared) {
    isShared = shared;
  }
}
