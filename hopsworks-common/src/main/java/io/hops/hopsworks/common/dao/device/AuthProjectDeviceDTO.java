package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class AuthProjectDeviceDTO implements Serializable{

  private Integer projectId;

  private String deviceUuid;

  private String password;

  private String alias;

  public AuthProjectDeviceDTO() {
  }

  public AuthProjectDeviceDTO(Integer projectId, String deviceUuid, String password) {
    this.projectId = projectId;
    this.deviceUuid = deviceUuid;
    this.password = password;
  }

  public AuthProjectDeviceDTO(Integer projectId, String deviceUuid, String password, String alias) {
    this.projectId = projectId;
    this.deviceUuid = deviceUuid;
    this.password = password;
    this.alias = alias;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getDeviceUuid() {
    return deviceUuid;
  }

  public void setDeviceUuid(String deviceUuid) {
    this.deviceUuid = deviceUuid;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }
}
