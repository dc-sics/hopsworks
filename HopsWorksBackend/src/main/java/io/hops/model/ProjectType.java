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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author AMore
 */
@Entity
@Table(name = "project_type")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectType.findAll", query = "SELECT p FROM ProjectType p"),
    @NamedQuery(name = "ProjectType.findById", query = "SELECT p FROM ProjectType p WHERE p.id = :id"),
    @NamedQuery(name = "ProjectType.findByType", query = "SELECT p FROM ProjectType p WHERE p.type = :type")})
public class ProjectType implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 12)
    @Column(name = "type")
    private String type;
    @JoinColumn(name = "projectID", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Project projectID;

    public ProjectType() {
    }

    public ProjectType(Integer id) {
        this.id = id;
    }

    public ProjectType(Integer id, String type) {
        this.id = id;
        this.type = type;
    }
    
    @XmlTransient
    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    @XmlTransient
    public Project getProjectID() {
        return projectID;
    }

    public void setProjectID(Project projectID) {
        this.projectID = projectID;
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
        if (!(object instanceof ProjectType)) {
            return false;
        }
        ProjectType other = (ProjectType) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.model.ProjectType[ id=" + id + " ]";
    }
    
}
