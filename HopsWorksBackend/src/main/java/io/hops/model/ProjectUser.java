package io.hops.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author AMore
 */
@Entity
@Table(name = "project_user")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectUser.findRoleByEmailAndID", query = "SELECT p FROM ProjectUser p WHERE p.email = :email AND p.id = :id"),
    @NamedQuery(name = "ProjectUser.findAll", query = "SELECT p FROM ProjectUser p"),
    @NamedQuery(name = "ProjectUser.findById", query = "SELECT p FROM ProjectUser p WHERE p.id = :id")})
public class ProjectUser implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "projectId", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Project projectId;
    @JoinColumn(name = "role", referencedColumnName = "name")
    @ManyToOne(optional = false)
    private ProjectRole role;
    @JoinColumn(name = "email", referencedColumnName = "email")
    @ManyToOne(optional = false)
    private Users email;

    public ProjectUser() {
    }

    public ProjectUser(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Project getProjectId() {
        return projectId;
    }

    public void setProjectId(Project projectId) {
        this.projectId = projectId;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public Users getEmail() {
        return email;
    }

    public void setEmail(Users email) {
        this.email = email;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProjectUser)) {
            return false;
        }
        ProjectUser other = (ProjectUser) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.model.ProjectUser[ id=" + id + " ]";
    }
    
}
