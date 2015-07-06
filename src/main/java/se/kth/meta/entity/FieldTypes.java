package se.kth.meta.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "vangelis_kthfs.meta_field_types")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FieldTypes.findAll",
          query = "SELECT f FROM FieldTypes f"),
  @NamedQuery(name = "FieldTypes.findById",
          query = "SELECT f FROM FieldTypes f WHERE f.id = :id"),
  @NamedQuery(name = "FieldTypes.findByDescription",
          query
          = "SELECT f FROM FieldTypes f WHERE f.description = :description")})
public class FieldTypes implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "description")
  private String description;

  @OneToMany(mappedBy = "fieldTypes",
          targetEntity = Fields.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL) //cascade type all updates the child entities
  private List<Fields> fields;

  public FieldTypes() {
  }

  public FieldTypes(Integer id) {
    this.id = id;
    this.fields = new LinkedList<>();
  }

  public FieldTypes(Integer id, String description) {
    this.id = id;
    this.description = description;
    this.fields = new LinkedList<>();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Fields> getFields() {
    return this.fields;
  }

  public void setFields(List<Fields> fields) {
    this.fields = fields;
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
    if (!(object instanceof FieldTypes)) {
      return false;
    }
    FieldTypes other = (FieldTypes) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.FieldTypes[ id=" + id + " ]";
  }

  @Override
  public void copy(EntityIntf entity) {
    FieldTypes ft = (FieldTypes) entity;

    this.id = ft.getId();
    this.description = ft.getDescription();
  }

}
