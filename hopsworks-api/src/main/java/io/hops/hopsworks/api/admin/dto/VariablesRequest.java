package io.hops.hopsworks.api.admin.dto;

import io.hops.hopsworks.common.dao.util.Variables;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class VariablesRequest {
  
  @XmlElement(name = "variables", type = Variables.class)
  private List<Variables> variables;
  
  public VariablesRequest() {
  
  }
  
  public VariablesRequest(List<Variables> variables) {
    this.variables = variables;
  }
  
  public List<Variables> getVariables() {
    return variables;
  }
  
  public void setVariables(List<Variables> variables) {
    this.variables = variables;
  }
}
