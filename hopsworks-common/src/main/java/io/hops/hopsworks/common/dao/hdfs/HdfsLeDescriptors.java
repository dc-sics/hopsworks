package io.hops.hopsworks.common.dao.hdfs;

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
@Table(name = "hops.hdfs_le_descriptors")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsLeDescriptors.findEndpoint",
          query
          = "SELECT h FROM HdfsLeDescriptors h ORDER BY h.hdfsLeDescriptorsPK.id ASC"),
  @NamedQuery(name = "HdfsLeDescriptors.findAll",
          query = "SELECT h FROM HdfsLeDescriptors h"),
  @NamedQuery(name = "HdfsLeDescriptors.findById",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.id = :id"),
  @NamedQuery(name = "HdfsLeDescriptors.findByCounter",
          query = "SELECT h FROM HdfsLeDescriptors h WHERE h.counter = :counter"),
  @NamedQuery(name
          = "HdfsLeDescriptors.findByHost.jpql.parser.IdentificationVariablname",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.rpcAddresses = :rpcAddresses"),
  @NamedQuery(name = "HdfsLeDescriptors.findByHttpAddress",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.httpAddress = :httpAddress"),
  @NamedQuery(name = "HdfsLeDescriptors.findByPartitionVal",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.partitionVal = :partitionVal")})
public class HdfsLeDescriptors implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HdfsLeDescriptorsPK hdfsLeDescriptorsPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "counter")
  private long counter;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 25)
  @Column(name = "rpc_addresses")
  private String rpcAddresses;
  @Size(max = 100)
  @Column(name = "http_address")
  private String httpAddress;

  public HdfsLeDescriptors() {
  }

  public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
  }

  public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK, long counter,
          String rpcAddresses) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
    this.counter = counter;
    this.rpcAddresses = rpcAddresses;
  }

  public HdfsLeDescriptors(long id, int partitionVal) {
    this.hdfsLeDescriptorsPK = new HdfsLeDescriptorsPK(id, partitionVal);
  }

  public HdfsLeDescriptorsPK getHdfsLeDescriptorsPK() {
    return hdfsLeDescriptorsPK;
  }

  public void setHdfsLeDescriptorsPK(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
  }

  public long getCounter() {
    return counter;
  }

  public void setCounter(long counter) {
    this.counter = counter;
  }

  public String getHostname() {
    if (rpcAddresses == null) {
      return "";
    }
    int pos = rpcAddresses.indexOf(",");
    if (pos == -1) {
      return "";
    }
    return rpcAddresses.substring(0, pos);
  }
  
  public String getRpcAddresses() {
    return rpcAddresses;
  }

  public void setRpcAddresses(String rpcAddresses) {
    this.rpcAddresses = rpcAddresses;
  }

  public String getHttpAddress() {
    return httpAddress;
  }

  public void setHttpAddress(String httpAddress) {
    this.httpAddress = httpAddress;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (hdfsLeDescriptorsPK != null ? hdfsLeDescriptorsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HdfsLeDescriptors)) {
      return false;
    }
    HdfsLeDescriptors other = (HdfsLeDescriptors) object;
    if ((this.hdfsLeDescriptorsPK == null && other.hdfsLeDescriptorsPK != null)
            || (this.hdfsLeDescriptorsPK != null && !this.hdfsLeDescriptorsPK.
            equals(other.hdfsLeDescriptorsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hdfs.HdfsLeDescriptors[ hdfsLeDescriptorsPK="
            + hdfsLeDescriptorsPK + " ]";
  }

}
