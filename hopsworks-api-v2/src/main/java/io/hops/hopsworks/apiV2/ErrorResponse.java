package io.hops.hopsworks.apiV2;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ErrorResponse {
  
  private String description;
  
  public ErrorResponse(){}
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
}
