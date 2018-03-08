package io.hops.hopsworks.common.dao.project.pia;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import javax.annotation.Nullable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "pia",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Pia.findAll",
      query = "SELECT p FROM Pia p")
  ,
    @NamedQuery(name = "Pia.findById",
      query = "SELECT p FROM Pia p WHERE p.id = :id")
  ,
    @NamedQuery(name = "Pia.findByPersonalData",
      query = "SELECT p FROM Pia p WHERE p.personalData = :personalData")
  ,
    @NamedQuery(name = "Pia.findByHowDataCollected",
      query
      = "SELECT p FROM Pia p WHERE p.howDataCollected = :howDataCollected")
  ,
    @NamedQuery(name = "Pia.findBySpecifiedExplicitLegitimate",
      query
      = "SELECT p FROM Pia p WHERE p.specifiedExplicitLegitimate = :specifiedExplicitLegitimate")
  ,
    @NamedQuery(name = "Pia.findByConsentProcess",
      query
      = "SELECT p FROM Pia p WHERE p.consentProcess = :consentProcess")
  ,
    @NamedQuery(name = "Pia.findByConsentBasis",
      query = "SELECT p FROM Pia p WHERE p.consentBasis = :consentBasis")
  ,
    @NamedQuery(name = "Pia.findByDataMinimized",
      query = "SELECT p FROM Pia p WHERE p.dataMinimized = :dataMinimized")
  ,
    @NamedQuery(name = "Pia.findByDataUptodate",
      query = "SELECT p FROM Pia p WHERE p.dataUptodate = :dataUptodate")
  ,
    @NamedQuery(name = "Pia.findByUsersInformedHow",
      query
      = "SELECT p FROM Pia p WHERE p.usersInformedHow = :usersInformedHow")
  ,
    @NamedQuery(name = "Pia.findByUserControlsDataCollectionRetention",
      query
      = "SELECT p FROM Pia p WHERE p.userControlsDataCollectionRetention = :userControlsDataCollectionRetention")
  ,
    @NamedQuery(name = "Pia.findByDataEncrypted",
      query = "SELECT p FROM Pia p WHERE p.dataEncrypted = :dataEncrypted")
  ,
    @NamedQuery(name = "Pia.findByDataAnonymized",
      query
      = "SELECT p FROM Pia p WHERE p.dataAnonymized = :dataAnonymized")
  ,
    @NamedQuery(name = "Pia.findByDataPseudonymized",
      query
      = "SELECT p FROM Pia p WHERE p.dataPseudonymized = :dataPseudonymized")
  ,
    @NamedQuery(name = "Pia.findByDataBackedup",
      query = "SELECT p FROM Pia p WHERE p.dataBackedup = :dataBackedup")
  ,
    @NamedQuery(name = "Pia.findByDataSecurityMeasures",
      query
      = "SELECT p FROM Pia p WHERE p.dataSecurityMeasures = :dataSecurityMeasures")
  ,
    @NamedQuery(name = "Pia.findByDataPortabilityMeasure",
      query
      = "SELECT p FROM Pia p WHERE p.dataPortabilityMeasure = :dataPortabilityMeasure")
  ,
    @NamedQuery(name = "Pia.findBySubjectAccessRights",
      query
      = "SELECT p FROM Pia p WHERE p.subjectAccessRights = :subjectAccessRights")
  ,
    @NamedQuery(name = "Pia.findByProjectId",
      query
      = "SELECT p FROM Pia p WHERE p.projectId = :projectId")
  ,
    @NamedQuery(name = "Pia.findByRisks",
      query = "SELECT p FROM Pia p WHERE p.risks = :risks")})
public class Pia implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 4000)
  @Column(name = "personal_data")
  private String personalData;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 2000)
  @Column(name = "how_data_collected")
  private String howDataCollected;
  @Basic(optional = false)
  @NotNull
  @Column(name = "specified_explicit_legitimate")
  private int specifiedExplicitLegitimate;
  @Size(max = 1000)
  @Column(name = "consent_process")
  private String consentProcess;
  @Size(max = 1000)
  @Column(name = "consent_basis")
  private String consentBasis;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_minimized")
  private int dataMinimized;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_uptodate")
  private int dataUptodate;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 500)
  @Column(name = "users_informed_how")
  private String usersInformedHow;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 500)
  @Column(name = "user_controls_data_collection_retention")
  private String userControlsDataCollectionRetention;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_encrypted")
  private int dataEncrypted;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_anonymized")
  private int dataAnonymized;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_pseudonymized")
  private int dataPseudonymized;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_backedup")
  private int dataBackedup;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 500)
  @Column(name = "data_security_measures")
  private String dataSecurityMeasures;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 500)
  @Column(name = "data_portability_measure")
  private String dataPortabilityMeasure;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 500)
  @Column(name = "subject_access_rights")
  private String subjectAccessRights;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 2000)
  @Column(name = "risks")
  private String risks;
//  @JoinColumn(name = "project_id",
//      referencedColumnName = "id")
//  @ManyToOne(optional = false)
  @Nullable
  private int projectId;
//  private Project projectId;

  public Pia() {
  }

  public Pia(Long id) {
    this.id = id;
  }

  public Pia(Long id, String personalData, String howDataCollected, int specifiedExplicitLegitimate, int dataMinimized,
      int dataUptodate, String usersInformedHow, String userControlsDataCollectionRetention, int dataEncrypted,
      int dataAnonymized, int dataPseudonymized, int dataBackedup, String dataSecurityMeasures,
      String dataPortabilityMeasure, String subjectAccessRights, String risks) {
    this.id = id;
    this.personalData = personalData;
    this.howDataCollected = howDataCollected;
    this.specifiedExplicitLegitimate = specifiedExplicitLegitimate;
    this.dataMinimized = dataMinimized;
    this.dataUptodate = dataUptodate;
    this.usersInformedHow = usersInformedHow;
    this.userControlsDataCollectionRetention = userControlsDataCollectionRetention;
    this.dataEncrypted = dataEncrypted;
    this.dataAnonymized = dataAnonymized;
    this.dataPseudonymized = dataPseudonymized;
    this.dataBackedup = dataBackedup;
    this.dataSecurityMeasures = dataSecurityMeasures;
    this.dataPortabilityMeasure = dataPortabilityMeasure;
    this.subjectAccessRights = subjectAccessRights;
    this.risks = risks;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPersonalData() {
    return personalData;
  }

  public void setPersonalData(String personalData) {
    this.personalData = personalData;
  }

  public String getHowDataCollected() {
    return howDataCollected;
  }

  public void setHowDataCollected(String howDataCollected) {
    this.howDataCollected = howDataCollected;
  }

  public int getSpecifiedExplicitLegitimate() {
    return specifiedExplicitLegitimate;
  }

  public void setSpecifiedExplicitLegitimate(int specifiedExplicitLegitimate) {
    this.specifiedExplicitLegitimate = specifiedExplicitLegitimate;
  }

  public String getConsentProcess() {
    return consentProcess;
  }

  public void setConsentProcess(String consentProcess) {
    this.consentProcess = consentProcess;
  }

  public String getConsentBasis() {
    return consentBasis;
  }

  public void setConsentBasis(String consentBasis) {
    this.consentBasis = consentBasis;
  }

  public int getDataMinimized() {
    return dataMinimized;
  }

  public void setDataMinimized(int dataMinimized) {
    this.dataMinimized = dataMinimized;
  }

  public int getDataUptodate() {
    return dataUptodate;
  }

  public void setDataUptodate(int dataUptodate) {
    this.dataUptodate = dataUptodate;
  }

  public String getUsersInformedHow() {
    return usersInformedHow;
  }

  public void setUsersInformedHow(String usersInformedHow) {
    this.usersInformedHow = usersInformedHow;
  }

  public String getUserControlsDataCollectionRetention() {
    return userControlsDataCollectionRetention;
  }

  public void setUserControlsDataCollectionRetention(String userControlsDataCollectionRetention) {
    this.userControlsDataCollectionRetention = userControlsDataCollectionRetention;
  }

  public int getDataEncrypted() {
    return dataEncrypted;
  }

  public void setDataEncrypted(int dataEncrypted) {
    this.dataEncrypted = dataEncrypted;
  }

  public int getDataAnonymized() {
    return dataAnonymized;
  }

  public void setDataAnonymized(int dataAnonymized) {
    this.dataAnonymized = dataAnonymized;
  }

  public int getDataPseudonymized() {
    return dataPseudonymized;
  }

  public void setDataPseudonymized(int dataPseudonymized) {
    this.dataPseudonymized = dataPseudonymized;
  }

  public int getDataBackedup() {
    return dataBackedup;
  }

  public void setDataBackedup(int dataBackedup) {
    this.dataBackedup = dataBackedup;
  }

  public String getDataSecurityMeasures() {
    return dataSecurityMeasures;
  }

  public void setDataSecurityMeasures(String dataSecurityMeasures) {
    this.dataSecurityMeasures = dataSecurityMeasures;
  }

  public String getDataPortabilityMeasure() {
    return dataPortabilityMeasure;
  }

  public void setDataPortabilityMeasure(String dataPortabilityMeasure) {
    this.dataPortabilityMeasure = dataPortabilityMeasure;
  }

  public String getSubjectAccessRights() {
    return subjectAccessRights;
  }

  public void setSubjectAccessRights(String subjectAccessRights) {
    this.subjectAccessRights = subjectAccessRights;
  }

  public String getRisks() {
    return risks;
  }

  public void setRisks(String risks) {
    this.risks = risks;
  }

//  public Project getProjectId() {
//    return projectId;
//  }
//
//  public void setProjectId(Project projectId) {
//    this.projectId = projectId;
//  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
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
    if (!(object instanceof Pia)) {
      return false;
    }
    Pia other = (Pia) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.project.pia.Pia[ id=" + id + " ]";
  }

}
