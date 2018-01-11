package io.hops.hopsworks.api.admin.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class ProjectDeletionLog implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String successLog;
  private String errorLog;
  
  public ProjectDeletionLog() {
  }
  
  public ProjectDeletionLog(String successLog, String errorLog) {
    this.successLog = successLog;
    this.errorLog = errorLog;
  }
  
  public String getSuccessLog() {
    return successLog;
  }
  
  public void setSuccessLog(String successLog) {
    this.successLog = successLog;
  }
  
  public String getErrorLog() {
    return errorLog;
  }
  
  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }
}
