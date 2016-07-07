package se.kth.bbc.project;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
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
import se.kth.bbc.activity.Activity;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.services.ProjectServices;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "hopsworks.project")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Project.findAll",
          query = "SELECT t FROM Project t"),
  @NamedQuery(name = "Project.findByName",
          query = "SELECT t FROM Project t WHERE t.name = :name"),
  @NamedQuery(name = "Project.findByOwner",
          query = "SELECT t FROM Project t WHERE t.owner = :owner"),
  @NamedQuery(name = "Project.findByCreated",
          query = "SELECT t FROM Project t WHERE t.created = :created"),
  @NamedQuery(name = "Project.findByEthicalStatus",
          query
          = "SELECT t FROM Project t WHERE t.ethicalStatus = :ethicalStatus"),
  @NamedQuery(name = "Project.findByRetentionPeriod",
          query
          = "SELECT t FROM Project t WHERE t.retentionPeriod = :retentionPeriod"),
  @NamedQuery(name = "Project.countProjectByOwner",
          query
          = "SELECT count(t) FROM Project t WHERE t.owner = :owner"),
  @NamedQuery(name = "Project.findByOwnerAndName",
          query
          = "SELECT t FROM Project t WHERE t.owner = :owner AND t.name = :name"),
  @NamedQuery(name = "Project.findByInodeId",
          query
          = "SELECT t FROM Project t WHERE t.inode.inodePK.parentId = :parentid AND t.inode.inodePK.name = :name")})
public class Project implements Serializable {

  @Column(name = "archived")
  private Boolean archived = false;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "project")
  private Collection<ProjectTeam> projectTeamCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "project")
  private Collection<Activity> activityCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "project")
  private Collection<ProjectServices> projectServicesCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "projectId")
  private Collection<Dataset> datasetCollection;

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 88)
  @Column(name = "projectname")
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "username",
          referencedColumnName = "email")
  private Users owner;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "retention_period")
  @Temporal(TemporalType.DATE)
  private Date retentionPeriod;

  @NotNull
  @Size(min = 1,
          max = 30)
  @Column(name = "ethical_status")
  private String ethicalStatus;

  @Column(name = "deleted")
  private Boolean deleted;

  @Size(max = 3000)
  @Column(name = "description")
  private String description;

  @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name")
  })
  @OneToOne(optional = false)
  private Inode inode;

  public Project() {
  }

  public Project(String name) {
    this.name = name;
    this.archived = false;
  }

  public Project(String name, Inode inode) {
    this(name);
    this.inode = inode;
  }

  public Project(String name, Users owner, Date timestamp) {
    this.name = name;
    this.owner = owner;
    this.created = timestamp;
    this.archived = false;
  }



  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getEthicalStatus() {
    return ethicalStatus;
  }

  public void setEthicalStatus(String ethicalStatus) {
    this.ethicalStatus = ethicalStatus;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public Inode getInode() {
    return this.inode;
  }

  public Users getOwner() {
    return owner;
  }

  public void setOwner(Users owner) {
    this.owner = owner;
  }

  public Date getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project(Integer id) {
    this.id = id;
  }

  public Project(Integer id, String projectname) {
    this.id = id;
    this.name = projectname;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
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
    if (!(object instanceof Project)) {
      return false;
    }
    Project other = (Project) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectTeam> getProjectTeamCollection() {
    return projectTeamCollection;
  }

  public void setProjectTeamCollection(
          Collection<ProjectTeam> projectTeamCollection) {
    this.projectTeamCollection = projectTeamCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Activity> getActivityCollection() {
    return activityCollection;
  }

  public void setActivityCollection(Collection<Activity> activityCollection) {
    this.activityCollection = activityCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectServices> getProjectServicesCollection() {
    return projectServicesCollection;
  }

  public void setProjectServicesCollection(
          Collection<ProjectServices> projectServicesCollection) {
    this.projectServicesCollection = projectServicesCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Dataset> getDatasetCollection() {
    return datasetCollection;
  }

  public void setDatasetCollection(Collection<Dataset> datasetCollection) {
    this.datasetCollection = datasetCollection;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.Project[ name=" + this.name + ", id=" + this.id
            + ", parentId=" + this.inode.getInodePK().getParentId() + " ]";
  }
}
