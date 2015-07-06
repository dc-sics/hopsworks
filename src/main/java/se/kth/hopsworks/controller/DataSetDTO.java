package se.kth.hopsworks.controller;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author ermiasg
 */
@XmlRootElement
public class DataSetDTO {

  private String name;
  private String description;
  private String searchable;
  private int template;

  public DataSetDTO() {
  }

  public DataSetDTO(String name, String description, String searchable,
          int template) {
    this.name = name;
    this.description = description;
    this.searchable = searchable;
    this.template = template;
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

  public String getSearchable() {
    return searchable;
  }

  public void setSearchable(String searchable) {
    this.searchable = searchable;
  }

  public int getTemplate() {
    return this.template;
  }

  public void setTemplate(int template) {
    this.template = template;
  }

  @Override
  public String toString() {
    return "DataSetDTO{" + "template=" + this.template + "name=" + name
            + ", description=" + description + ", searchable=" + searchable
            + '}';
  }

}
