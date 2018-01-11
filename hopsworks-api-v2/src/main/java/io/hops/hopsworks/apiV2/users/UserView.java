package io.hops.hopsworks.apiV2.users;

import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserView {
  private String firstname;
  private String lastname;
  private Integer uid;
  
  public UserView(){}
  
  public UserView(Users user){
    firstname = user.getFname();
    lastname = user.getLname();
    uid = user.getUid();
  }
  
  public UserView(String firstname, String lastname, Integer uid) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.uid = uid;
  }
  
  public String getFirstname() {
    return firstname;
  }
  
  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }
  
  public String getLastname() {
    return lastname;
  }
  
  public void setLastname(String lastname) {
    this.lastname = lastname;
  }
  
  public Integer getUid() {
    return uid;
  }
  
  public void setUid(Integer uid) {
    this.uid = uid;
  }
}
