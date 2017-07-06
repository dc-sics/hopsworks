package io.hops.hopsworks.common.dao.device;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ProjectUserUuidPK implements Serializable {

  private static final long serialVersionUID = 1L;

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 150)
  @Column(name = "user_email")
  private String userEmail;

  public ProjectUserUuidPK() {
  }

  public ProjectUserUuidPK(Integer projectId, String userEmail) {
    this.projectId = projectId;
    this.userEmail = userEmail;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (userEmail != null ? userEmail.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ProjectUserUuidPK)) {
      return false;
    }
    ProjectUserUuidPK other = (ProjectUserUuidPK) object;
    if (this.projectId != other.getProjectId()) {
      return false;
    }

    return this.userEmail.equals(other.userEmail);

  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.device.ProjectUserUuidPK[ projectId=" + 
        this.projectId +  ", userEmail=" + this.userEmail + " ]";

  }
}
