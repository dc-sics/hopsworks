package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.users.UserView;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MemberView {
  private UserView user;
  private String role;
  
  public MemberView(){}
  
  public MemberView(ProjectTeam member){
    user = new UserView(member.getUser());
    role = member.getTeamRole();
  }
  
  public UserView getUser() {
    return user;
  }
  
  public void setUser(UserView user) {
    this.user = user;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
}
