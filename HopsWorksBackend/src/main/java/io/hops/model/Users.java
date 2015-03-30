/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.model;

import io.hops.integration.UserDTO;
import io.hops.model.Groups;
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
    @NamedQuery(name = "Users.findByTelephoneNum", query = "SELECT u FROM Users u WHERE u.telephoneNum = :telephoneNum"),
    @NamedQuery(name = "Users.findByFirstName", query = "SELECT u FROM Users u WHERE u.firstName = :firstName"),
    @NamedQuery(name = "Users.findByPassword", query = "SELECT u FROM Users u WHERE u.password = :password"),
    @NamedQuery(name = "Users.findByRegisteredon", query = "SELECT u FROM Users u WHERE u.registeredon = :registeredon"),
    @NamedQuery(name = "Users.findByStatus", query = "SELECT u FROM Users u WHERE u.status = :status"),
    @NamedQuery(name = "Users.findByLastName", query = "SELECT u FROM Users u WHERE u.lastName = :lastName"),
    @NamedQuery(name = "Users.findBySecurityQuestion", query = "SELECT u FROM Users u WHERE u.securityQuestion = :securityQuestion"),
    @NamedQuery(name = "Users.findBySecurityAnswer", query = "SELECT u FROM Users u WHERE u.securityAnswer = :securityAnswer"),
    @NamedQuery(name = "Users.findBySecret", query = "SELECT u FROM Users u WHERE u.secret = :secret"),
    @NamedQuery(name = "Users.findByIsonline", query = "SELECT u FROM Users u WHERE u.isonline = :isonline"),
    @NamedQuery(name = "Users.findByFalseLogin", query = "SELECT u FROM Users u WHERE u.falseLogin = :falseLogin")})
public class Users implements Serializable {
    
    public static final int STATUS_REQUEST = -1;
    public static final int STATUS_ALLOW = 0;
    
    public static final int STATUS_IS_ONLINE = 0;
    public static final int STATUS_IS_OFFLINE = -1;
    
    private static final long serialVersionUID = 1L;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "email")
    private String email;
    @Size(max = 255)
    @Column(name = "telephoneNum")
    private String telephoneNum;
    @Size(max = 255)
    @Column(name = "firstName")
    private String firstName;
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
    @Size(max = 255)
    @Column(name = "lastName")
    private String lastName;
    @Size(max = 45)
    @Column(name = "security_question")
    private String securityQuestion;
    @Size(max = 128)
    @Column(name = "security_answer")
    private String securityAnswer;
    @Size(max = 45)
    @Column(name = "secret")
    private String secret;
    @Basic(optional = false)
    @NotNull
    @Column(name = "isonline")
    private int isonline;
    @Basic(optional = false)
    @NotNull
    @Column(name = "false_login")
    private int falseLogin;
    @ManyToMany(mappedBy = "usersCollection")
    private Collection<Groups> groupsCollection;

    public Users() {
    }

    public Users(String email) {
        this.email = email;
    }

    public Users(UserDTO dto) {

        this.email = dto.getEmail();
        this.firstName = dto.getFirstName();
        this.lastName = dto.getLastName();
        this.telephoneNum = dto.getTelephoneNum();
        setPassword(dto.getChosenPassword());
        this.registeredon = new Date();
        this.secret = dto.getSecret();
        this.securityQuestion = dto.getSecurityQuestion();
        this.securityAnswer = dto.getSecurityAnswer();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephoneNum() {
        return telephoneNum;
    }

    public void setTelephoneNum(String telephoneNum) {
        this.telephoneNum = telephoneNum;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getIsonline() {
        return isonline;
    }

    public void setIsonline(int isonline) {
        this.isonline = isonline;
    }

    public int getFalseLogin() {
        return falseLogin;
    }

    public void setFalseLogin(int falseLogin) {
        this.falseLogin = falseLogin;
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
        return "io.hops.filters.Users[ email=" + email + " ]";
    }
    
}
