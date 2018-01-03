package io.hops.hopsworks.common.dao.user.ldap;

import java.util.List;

public class LdapUserDTO {
  private String uidNumber;
  private String uid;
  private String givenName;
  private String sn;
  private List<String> email;

  public LdapUserDTO() {
  }

  public LdapUserDTO(String uidNumber, String uid, String givenName, String sn, List<String> email) {
    this.uidNumber = uidNumber;
    this.uid = uid;
    this.givenName = givenName;
    this.sn = sn;
    this.email = email;
  }

  public String getUidNumber() {
    return uidNumber;
  }

  public void setUidNumber(String uidNumber) {
    this.uidNumber = uidNumber;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getSn() {
    return sn;
  }

  public void setSn(String sn) {
    this.sn = sn;
  }

  public List<String> getEmail() {
    return email;
  }

  public void setEmail(List<String> email) {
    this.email = email;
  }

  @Override
  public String toString() {
    return "LdapUserDTO{" + "uidNumber=" + uidNumber + ", uid=" + uid + ", givenName=" + givenName + ", sn=" + sn +
        ", email=" + email + '}';
  }  
  
}
