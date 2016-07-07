package se.kth.hopsworks.meta.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vangelis
 */
@Entity
@Table(name = "hopsworks.meta_data")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Metadata.findAll",
          query
          = "SELECT m FROM Metadata m"),
  @NamedQuery(name = "Metadata.findByPrimaryKey",
          query
          = "SELECT m FROM Metadata m WHERE m.metadataPK = :metadataPK"),
  @NamedQuery(name = "Metadata.findById",
          query = "SELECT m FROM Metadata m WHERE m.metadataPK.id = :id")
})
public class Metadata implements EntityIntf, Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private MetadataPK metadataPK;

  @Basic(optional = false)
  @NotNull
  @Lob
  @Size(min = 1,
          max = 12000)
  @Column(name = "data")
  private String data;

  @ManyToOne(optional = false,
          fetch = FetchType.EAGER)
  @JoinColumns(value = {
    @JoinColumn(name = "fieldid",
            referencedColumnName = "fieldid",
            insertable = false,
            updatable = false),
    @JoinColumn(name = "tupleid",
            referencedColumnName = "tupleid",
            insertable = false,
            updatable = false)})
  private RawData rawdata;

  public Metadata() {
    this.metadataPK = new MetadataPK(-1, -1, -1);
  }

  public Metadata(MetadataPK metadataPK) {
    this.metadataPK = metadataPK;
  }

  public Metadata(MetadataPK metadataPK, String data) {
    this.metadataPK = metadataPK;
    this.data = data;
  }

  public void setMetadataPK(MetadataPK metadataPK) {
    this.metadataPK = metadataPK;
  }

  public MetadataPK getMetadataPK() {
    return this.metadataPK;
  }

  public void setRawData(RawData rawdata) {
    this.rawdata = rawdata;
  }

  public RawData getRawData() {
    return this.rawdata;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getData() {
    return this.data;
  }

  @Override
  public void copy(EntityIntf entity) {
    Metadata metadata = (Metadata) entity;

    this.metadataPK.copy(metadata.getMetadataPK());
    this.data = metadata.getData();
  }

  @Override
  public Integer getId() {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void setId(Integer id) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (this.metadataPK != null ? this.metadataPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Metadata)) {
      return false;
    }
    Metadata other = (Metadata) object;

    return !((this.metadataPK == null && other.getMetadataPK() != null)
            || (this.metadataPK != null
            && !this.metadataPK.equals(other.getMetadataPK())));
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.MetaData[ metadataPK= " + metadataPK + " ]";
  }
}