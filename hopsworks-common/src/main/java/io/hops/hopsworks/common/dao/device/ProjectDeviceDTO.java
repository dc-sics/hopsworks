package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement
public class ProjectDeviceDTO implements Serializable{

  private Integer projectId;

  private String deviceUuid;

  private String alias;

  private Date createdAt;

  private Integer state;

  public ProjectDeviceDTO(){}

  public ProjectDeviceDTO(
    Integer projectId, String deviceUuid, String alias, Date createdAt, Integer state, Date lastProduced) {
    this.projectId = projectId;
    this.deviceUuid = deviceUuid;
    this.alias = alias;
    this.createdAt = createdAt;
    this.state = state;
    this.lastProduced = lastProduced;
  }

  private Date lastProduced;

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

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Integer getState() {
    return state;
  }

  public void setState(Integer state) {
    this.state = state;
  }

  public Date getLastProduced() {
    return lastProduced;
  }

  public void setLastProduced(Date lastProduced) {
    this.lastProduced = lastProduced;
  }
}
