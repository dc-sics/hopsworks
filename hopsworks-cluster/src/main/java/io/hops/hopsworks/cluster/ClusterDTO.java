package io.hops.hopsworks.cluster;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClusterDTO {
  private String email;
  private String chosenPassword;
  private String repeatedPassword;
  private boolean ToS;

  public ClusterDTO() {
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getChosenPassword() {
    return chosenPassword;
  }

  public void setChosenPassword(String chosenPassword) {
    this.chosenPassword = chosenPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  public boolean isToS() {
    return ToS;
  }

  public void setToS(boolean ToS) {
    this.ToS = ToS;
  }
  
}
