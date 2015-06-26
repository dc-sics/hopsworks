package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.project.Project;
import se.kth.bbc.security.ua.SecurityQuestion;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "users")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "User.findAll",
          query = "SELECT u FROM User u"),
  @NamedQuery(name = "User.findByUid",
          query = "SELECT u FROM User u WHERE u.uid = :uid"),
  @NamedQuery(name = "User.findByUsername",
          query = "SELECT u FROM User u WHERE u.username = :username"),
  @NamedQuery(name = "User.findByPassword",
          query = "SELECT u FROM User u WHERE u.password = :password"),
  @NamedQuery(name = "User.findByEmail",
          query = "SELECT u FROM User u WHERE u.email = :email"),
  @NamedQuery(name = "User.findByFname",
          query = "SELECT u FROM User u WHERE u.fname = :fname"),
  @NamedQuery(name = "User.findByLname",
          query = "SELECT u FROM User u WHERE u.lname = :lname"),
  @NamedQuery(name = "User.findByActivated",
          query = "SELECT u FROM User u WHERE u.activated = :activated"),
  @NamedQuery(name = "User.findByTitle",
          query = "SELECT u FROM User u WHERE u.title = :title"),
  @NamedQuery(name = "User.findByMobile",
          query = "SELECT u FROM User u WHERE u.mobile = :mobile"),
  @NamedQuery(name = "User.findByOrcid",
          query = "SELECT u FROM User u WHERE u.orcid = :orcid"),
  @NamedQuery(name = "User.findByFalseLogin",
          query = "SELECT u FROM User u WHERE u.falseLogin = :falseLogin"),
  @NamedQuery(name = "User.findByStatus",
          query = "SELECT u FROM User u WHERE u.status = :status"),
  @NamedQuery(name = "User.findByIsonline",
          query = "SELECT u FROM User u WHERE u.isonline = :isonline"),
  @NamedQuery(name = "User.findBySecret",
          query = "SELECT u FROM User u WHERE u.secret = :secret"),
  @NamedQuery(name = "User.findByValidationKey",
          query = "SELECT u FROM User u WHERE u.validationKey = :validationKey"),
  @NamedQuery(name = "User.findBySecurityQuestion",
          query
          = "SELECT u FROM User u WHERE u.securityQuestion = :securityQuestion"),
  @NamedQuery(name = "User.findBySecurityAnswer",
          query
          = "SELECT u FROM User u WHERE u.securityAnswer = :securityAnswer"),
  @NamedQuery(name = "User.findByYubikeyUser",
          query = "SELECT u FROM User u WHERE u.yubikeyUser = :yubikeyUser"),
  @NamedQuery(name = "User.findByPasswordChanged",
          query
          = "SELECT u FROM User u WHERE u.passwordChanged = :passwordChanged"),
  @NamedQuery(name = "User.findByNotes",
          query = "SELECT u FROM User u WHERE u.notes = :notes"),
  @NamedQuery(name = "User.findMaxUid",
          query = "SELECT MAX(u.uid) FROM User u")})
public class User implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "uid")
  private Integer uid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 10)
  @Column(name = "username")
  private String username;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "password")
  private String password;
  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
  @Size(max = 45)
  @Column(name = "email")
  private String email;
  @Size(max = 30)
  @Column(name = "fname")
  private String fname;
  @Size(max = 30)
  @Column(name = "lname")
  private String lname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "activated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date activated;
  @Size(max = 10)
  @Column(name = "title")
  private String title;
  @Size(max = 20)
  @Column(name = "mobile")
  private String mobile;
  @Size(max = 20)
  @Column(name = "orcid")
  private String orcid;
  @Basic(optional = false)
  @NotNull
  @Column(name = "false_login")
  private int falseLogin;
  @Basic(optional = false)
  @NotNull
  @Column(name = "status")
  private int status;
  @Basic(optional = false)
  @NotNull
  @Column(name = "isonline")
  private int isonline;
  @Size(max = 20)
  @Column(name = "secret")
  private String secret;
  @Size(max = 128)
  @Column(name = "validation_key")
  private String validationKey;
  @Size(max = 20)
  @Enumerated(EnumType.STRING)
  @Column(name = "security_question")
  private SecurityQuestion securityQuestion;
  @Size(max = 128)
  @Column(name = "security_answer")
  private String securityAnswer;
  @Column(name = "yubikey_user")
  private int yubikeyUser;
  @Column(name = "password_changed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date passwordChanged;
  @Size(max = 500)
  @Column(name = "notes")
  private String notes;
  @OneToOne(cascade = CascadeType.ALL,
          mappedBy = "uid")
  private Yubikey yubikey;
  @OneToOne(cascade = CascadeType.ALL,
          mappedBy = "uid")
  private Address address;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "user")
  private Collection<PeopleGroup> peopleGroupCollection;
  @OneToOne(cascade = CascadeType.ALL,
          mappedBy = "uid")
  private Organization organization;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "owner")
  private Collection<Project> projectCollection;

  public User() {
  }

  public User(Integer uid) {
    this.uid = uid;
  }

  public User(Integer uid, String username, String password, Date activated,
          int falseLogin, int status, int isonline) {
    this.uid = uid;
    this.username = username;
    this.password = password;
    this.activated = activated;
    this.falseLogin = falseLogin;
    this.status = status;
    this.isonline = isonline;
  }

  @XmlTransient
  @JsonIgnore
  public String getValidationKey() {
    return validationKey;
  }

  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @XmlTransient
  @JsonIgnore
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFname() {
    return fname;
  }

  public void setFname(String fname) {
    this.fname = fname;
  }

  public String getLname() {
    return lname;
  }

  public void setLname(String lname) {
    this.lname = lname;
  }

  @XmlTransient
  @JsonIgnore
  public Date getActivated() {
    return activated;
  }

  public void setActivated(Date activated) {
    this.activated = activated;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  @XmlTransient
  @JsonIgnore
  public String getOrcid() {
    return orcid;
  }

  public void setOrcid(String orcid) {
    this.orcid = orcid;
  }

  @XmlTransient
  @JsonIgnore
  public int getFalseLogin() {
    return falseLogin;
  }

  public void setFalseLogin(int falseLogin) {
    this.falseLogin = falseLogin;
  }

  @XmlTransient
  @JsonIgnore
  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public int getIsonline() {
    return isonline;
  }

  public void setIsonline(int isonline) {
    this.isonline = isonline;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @XmlTransient
  @JsonIgnore
  public SecurityQuestion getSecurityQuestion() {
    return securityQuestion;
  }

  public void setSecurityQuestion(SecurityQuestion securityQuestion) {
    this.securityQuestion = securityQuestion;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  @XmlTransient
  @JsonIgnore
  public int getYubikeyUser() {
    return yubikeyUser;
  }

  public void setYubikeyUser(int yubikeyUser) {
    this.yubikeyUser = yubikeyUser;
  }

  public Date getPasswordChanged() {
    return passwordChanged;
  }

  public void setPasswordChanged(Date passwordChanged) {
    this.passwordChanged = passwordChanged;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @XmlTransient
  @JsonIgnore
  public Yubikey getYubikey() {
    return yubikey;
  }

  public void setYubikey(Yubikey yubikey) {
    this.yubikey = yubikey;
  }

  @XmlTransient
  @JsonIgnore
  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  @XmlTransient
  @JsonIgnore
  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<PeopleGroup> getPeopleGroupCollection() {
    return peopleGroupCollection;
  }

  public void setPeopleGroupCollection(
          Collection<PeopleGroup> peopleGroupCollection) {
    this.peopleGroupCollection = peopleGroupCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (uid != null ? uid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof User)) {
      return false;
    }
    User other = (User) object;
    if ((this.uid == null && other.uid != null) || (this.uid != null
            && !this.uid.equals(other.uid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.User[ uid=" + uid + " ]";
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Project> getProjectCollection() {
    return projectCollection;
  }

  public void setProjectCollection(Collection<Project> projectCollection) {
    this.projectCollection = projectCollection;
  }
}
