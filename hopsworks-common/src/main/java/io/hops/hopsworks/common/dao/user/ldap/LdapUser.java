package io.hops.hopsworks.common.dao.user.ldap;

import io.hops.hopsworks.common.dao.user.Users;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "ldap_user")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "LdapUser.findAll",
      query = "SELECT l FROM LdapUser l")
  ,
  @NamedQuery(name = "LdapUser.findByUid",
      query = "SELECT l FROM LdapUser l WHERE l.uid = :uid")
  ,
    @NamedQuery(name = "LdapUser.findByUidNumber",
      query = "SELECT l FROM LdapUser l WHERE l.uidNumber = :uidNumber")})
public class LdapUser implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "uidNumber")
  private Integer uidNumber;
  @JoinColumn(name = "uid",
      referencedColumnName = "uid")
  @OneToOne(optional = false)
  private Users uid;

  public LdapUser() {
  }

  public LdapUser(Integer uidNumber) {
    this.uidNumber = uidNumber;
  }

  public Integer getUidNumber() {
    return uidNumber;
  }

  public void setUidNumber(Integer uidNumber) {
    this.uidNumber = uidNumber;
  }

  public Users getUid() {
    return uid;
  }

  public void setUid(Users uid) {
    this.uid = uid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (uidNumber != null ? uidNumber.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof LdapUser)) {
      return false;
    }
    LdapUser other = (LdapUser) object;
    if ((this.uidNumber == null && other.uidNumber != null) ||
        (this.uidNumber != null && !this.uidNumber.equals(other.uidNumber))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.user.ldap.LdapUser[ uidNumber=" + uidNumber + " ]";
  }
  
}
