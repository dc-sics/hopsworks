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
	@Column(name = "project_name")
	private String projectName;

	@Basic(optional = false)
	@NotNull
	@Size(min = 1, max = 10)
	@Column(name = "username")
	private String username;

	public ProjectUserUuidPK() {
	}

	public ProjectUserUuidPK(String projectName, String username) {
		this.projectName = projectName;
		this.username = username;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (projectName != null ? projectName.hashCode() : 0);
		hash += (username != null ? username.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ProjectUserUuidPK)) {
			return false;
		}
		ProjectUserUuidPK other = (ProjectUserUuidPK) object;
		if (!this.projectName.equals(other.projectName)) {
			return false;
		}

		return this.username.equals(other.username);

	}

	@Override
	public String toString() {
		return "io.hops.hopsworks.common.dao.device.ProjectUserUuidPK[ projectName=" + 
				this.projectName +  ", username=" + this.username + " ]";

	}
}
