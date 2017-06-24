package io.hops.hopsworks.common.dao.device;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.project_user_uuids")
@XmlRootElement
@NamedQueries({
	@NamedQuery(
			name = "ProjectUserUuid.findAll", 
			query = "SELECT pu FROM ProjectUserUuid pu"),
	@NamedQuery(
			name = "ProjectUserUuid.findByPrimaryKey",
			query= "SELECT pu FROM ProjectUserUuid pu WHERE pu.projectUserUuidPK = :projectUserUuidPK"),
	@NamedQuery(
			name = "ProjectUserUuid.findByProjectUserUuuid",
			query= "SELECT pu FROM ProjectUserUuid pu WHERE pu.projectUserUuid = :projectUserUuid")})
public class ProjectUserUuid implements Serializable{

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private ProjectUserUuidPK projectUserUuidPK;

	@Basic(optional = false)
	@NotNull
	@Size(min = 1, max = 36)
	@Column(name = "project_user_uuid")
	private String projectUserUuid;

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (this.projectUserUuidPK != null ? this.projectUserUuidPK.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ProjectUserUuid)) {
			return false;
		}
		ProjectUserUuid other = (ProjectUserUuid) object;

		return !((this.projectUserUuidPK == null && other.projectUserUuidPK != null)
				|| (this.projectUserUuidPK != null
				&& !this.projectUserUuidPK.equals(other.projectUserUuidPK)));
	}

	@Override
	public String toString() {
		return "io.hops.hopsworks.common.dao.device.ProjectUserUuid[ projectUserUuidPK= " + this.projectUserUuidPK + " ]";
	}

}
