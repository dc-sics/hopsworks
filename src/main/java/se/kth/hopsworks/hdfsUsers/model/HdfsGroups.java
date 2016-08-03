
package se.kth.hopsworks.hdfsUsers.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hops.hdfs_groups")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsGroups.findAll",
          query
          = "SELECT h FROM HdfsGroups h"),
  @NamedQuery(name = "HdfsGroups.delete",
          query
          = "DELETE FROM HdfsGroups h WHERE h.id =:id"),
  @NamedQuery(name = "HdfsGroups.findByName",
          query
          = "SELECT h FROM HdfsGroups h WHERE h.name = :name")})
public class HdfsGroups implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 100)
  @Column(name = "name")
  private String name;
  @JoinTable(name = "hops.hdfs_users_groups",
          joinColumns
          = {
            @JoinColumn(name = "group_id",
                    referencedColumnName = "id")},
          inverseJoinColumns
          = {
            @JoinColumn(name = "user_id",
                    referencedColumnName = "id")})
  @ManyToMany
  private Collection<HdfsUsers> hdfsUsersCollection;

  public HdfsGroups() {
  }

  public HdfsGroups(Integer id) {
    this.id = id;
  }

  public HdfsGroups(String name) {
    this.name = name;
  }

  public HdfsGroups(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HdfsUsers> getHdfsUsersCollection() {
    return hdfsUsersCollection;
  }

  public void setHdfsUsersCollection(Collection<HdfsUsers> hdfsUsersCollection) {
    this.hdfsUsersCollection = hdfsUsersCollection;
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
    if (!(object instanceof HdfsGroups)) {
      return false;
    }
    HdfsGroups other = (HdfsGroups) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.hdfsUsers.HdfsGroups[ id=" + id + " ]";
  }
  
}
