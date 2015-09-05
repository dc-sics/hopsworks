package se.kth.bbc.jobs.model.description;

import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.configuration.JsonReduceableConverter;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 * Description of work to be executed. If the work is executed, this
 * results in an Execution. Every type of Job needs to subclass this Entity and
 * declare the @DiscriminatorValue annotation.
 * <p>
 * @author stig
 */
@Entity
@Table(name = "hopsworks.jobs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobDescription.findAll",
          query = "SELECT j FROM JobDescription j"),
  @NamedQuery(name = "JobDescription.findById",
          query
          = "SELECT j FROM JobDescription j WHERE j.id = :id"),
  @NamedQuery(name = "JobDescription.findByName",
          query
          = "SELECT j FROM JobDescription j WHERE j.name = :name"),
  @NamedQuery(name = "JobDescription.findByCreationTime",
          query
          = "SELECT j FROM JobDescription j WHERE j.creationTime = :creationTime"),
  @NamedQuery(name = "JobDescription.findByProject",
          query
          = "SELECT j FROM JobDescription j WHERE j.project = :project"),
  @NamedQuery(name = "JobDescription.findByProjectAndType",
          query
          = "SELECT j FROM JobDescription j WHERE j.project = :project AND j.type = :type")})
public class JobDescription implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Size(max = 128)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "creation_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationTime;

  @Column(name = "json_config")
  @Convert(converter = JsonReduceableConverter.class)
  private JobConfiguration jobConfig;

  @Size(max = 128)
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private JobType type;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "creator",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users creator;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "job")
  private Collection<Execution> executionCollection;

  protected JobDescription() {
    this.name = "Hopsworks job";
  }

  public JobDescription(JobConfiguration config, Project project,
          Users creator) {
    this(config, project, creator, new Date());
  }

  public JobDescription(JobConfiguration config, Project project,
          Users creator, Date creationTime) {
    this(config, project, creator, null, creationTime);
  }

  public JobDescription(JobConfiguration config, Project project,
          Users creator, String jobname) {
    this(config, project, creator, jobname, new Date());
  }

  protected JobDescription(JobConfiguration config, Project project,
          Users creator, String jobname, Date creationTime) {
    if (Strings.isNullOrEmpty(jobname)) {
      this.name = "Hopsworks job";
    } else {
      this.name = jobname;
    }
    this.creationTime = creationTime;
    this.jobConfig = config;
    this.project = project;
    this.creator = creator;
    this.type = config.getType();
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

  /**
   * Set the name of the application. Default value: "Hopsworks job".
   * <p>
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }

  public JobConfiguration getJobConfig() {
    return jobConfig;
  }

  public void setJobConfig(JobConfiguration jobConfig) {
    this.jobConfig = jobConfig;
  }

  @XmlElement
  public JobType getJobType() {
    return type;
  }

  @JsonIgnore
  @XmlTransient
  public Collection<Execution> getExecutionCollection() {
    return executionCollection;
  }

  public void setExecutionCollection(
          Collection<Execution> executionCollection) {
    this.executionCollection = executionCollection;
  }

  @Override
  public final int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public final boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobDescription)) {
      return false;
    }
    JobDescription other = (JobDescription) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return type.toString() + "Job [" + name + ", " + id + "]";
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

}
