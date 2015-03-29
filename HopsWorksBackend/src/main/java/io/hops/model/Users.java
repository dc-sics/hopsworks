/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.model;

import io.hops.integration.UserDTO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Ermias
 */
@Entity
@Table(name = "users")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Users.findAll", query = "SELECT u FROM Users u"),
    @NamedQuery(name = "Users.findByEmail", query = "SELECT u FROM Users u WHERE u.email = :email"),
    @NamedQuery(name = "Users.findByMobilenum", query = "SELECT u FROM Users u WHERE u.mobilenum = :mobilenum"),
    @NamedQuery(name = "Users.findByName", query = "SELECT u FROM Users u WHERE u.name = :name"),
    @NamedQuery(name = "Users.findAllByName", query = "SELECT r.name FROM Users r"),
    @NamedQuery(name = "Users.findByRegisteredon", query = "SELECT u FROM Users u WHERE u.registeredon = :registeredon"),
    @NamedQuery(name = "Users.findByStatus", query = "SELECT u FROM Users u WHERE u.status = :status")})
public class Users implements Serializable, UsersInterface {
    private static final long serialVersionUID = 1L;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "email")
    private String email;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "mobilenum")
    private String mobilenum;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "password")
    private String password;
    @Basic(optional = false)
    @NotNull
    @Column(name = "registeredon")
    @Temporal(TemporalType.TIMESTAMP)
    private Date registeredon;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "salt")
    private byte[] salt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private int status;
    @ManyToMany(mappedBy = "usersCollection")
    private Collection<Groups> groupsCollection;

    public Users() {
    }

    public Users(String email) {
        this.email = email;
    }

    public Users(UserDTO dto) {
        if (dto.getPassword1() == null || dto.getPassword1().length() == 0
                || dto.getPassword1().compareTo(dto.getPassword2()) != 0) {
            throw new IllegalArgumentException("Password 1 and Password 2 have to be equal (typo?)");
        }
        this.email = dto.getEmail();
        this.name = dto.getEmail();
        this.mobilenum = dto.getMobileNum();
        setPassword(dto.getPassword1());
        this.registeredon = new Date();
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getMobilenum() {
        return mobilenum;
    }

    public void setMobilenum(String mobilenum) {
        this.mobilenum = mobilenum;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if(password != null){
            this.password = DigestUtils.sha256Hex(password);
        } else {
            this.password = "";
        }
    }

    @Override
    public Date getRegisteredon() {
        return registeredon;
    }

    public void setRegisteredon(Date registeredon) {
        this.registeredon = registeredon;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    @Override
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<Groups> getGroupsCollection() {
        return groupsCollection;
    }

    public void setGroupsCollection(Collection<Groups> groupsCollection) {
        this.groupsCollection = groupsCollection;
    }
    
    public void addGroup(Groups g) {
        if (this.groupsCollection == null || this.groupsCollection.isEmpty()) {
            this.groupsCollection = new ArrayList<>();
        }
        if (!this.groupsCollection.contains(g)) {
            this.groupsCollection.add(g);
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (email != null ? email.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Users)) {
            return false;
        }
        Users other = (Users) object;
        if ((this.email == null && other.email != null) || (this.email != null && !this.email.equals(other.email))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.model.Users[ email=" + email + " ]";
    }
    
}
