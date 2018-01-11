package io.hops.hopsworks.apiV2.currentUser;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LimitedMemberView {
  private Integer uid;
  private Integer projectId;
  private String role;
  
  public LimitedMemberView(){}
  
  public LimitedMemberView(int uId, int projectId, String role){
    this.uid = uId;
    this.projectId = projectId;
    this.role = role;
  }
  
  public Integer getUid() {
    return uid;
  }
  
  public void setUid(Integer uid) {
    this.uid = uid;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
}
