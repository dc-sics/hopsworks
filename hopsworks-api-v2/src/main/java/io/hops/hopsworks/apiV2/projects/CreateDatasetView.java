package io.hops.hopsworks.apiV2.projects;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CreateDatasetView {
  private String description;
  private int template;
  private String name;
  private boolean searchable;
  private boolean generateReadme;
  
  public CreateDatasetView(){}
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public int getTemplate() {
    return template;
  }
  
  public void setTemplate(int template) {
    this.template = template;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public boolean isSearchable() {
    return searchable;
  }
  
  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }
  
  public boolean isGenerateReadme() {
    return generateReadme;
  }
  
  public void setGenerateReadme(boolean generateReadme) {
    this.generateReadme = generateReadme;
  }
}
