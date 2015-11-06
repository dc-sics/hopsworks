/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project.privacy.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "consent")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Consent.findAll",
          query
          = "SELECT c FROM Consent c"),
  @NamedQuery(name = "Consent.findById",
          query
          = "SELECT c FROM Consent c WHERE c.id = :id"),
  @NamedQuery(name = "Consent.findByDate",
          query
          = "SELECT c FROM Consent c WHERE c.date = :date"),
  @NamedQuery(name = "Consent.findByProjectOwner",
          query
          = "SELECT c FROM Consent c WHERE c.projectOwner = :projectOwner"),
  @NamedQuery(name = "Consent.findByStatus",
          query
          = "SELECT c FROM Consent c WHERE c.status = :status"),
  @NamedQuery(name = "Consent.findByName",
          query
          = "SELECT c FROM Consent c WHERE c.name = :name"),
  @NamedQuery(name = "Consent.findByType",
          query
          = "SELECT c FROM Consent c WHERE c.type = :type")})
public class Consent implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Column(name = "date")
  @Temporal(TemporalType.DATE)
  private Date date;
  @Size(max = 245)
  @Column(name = "project_owner")
  private String projectOwner;
  @Size(max = 30)
  @Column(name = "status")
  private String status;
  @Size(max = 128)
  @Column(name = "name")
  private String name;
  @Size(max = 30)
  @Column(name = "type")
  private String type;
  @Lob
  @Column(name = "consent_form")
  private byte[] consentForm;
  @JoinColumn(name = "project_name",
          referencedColumnName = "projectname")
  @ManyToOne
  private Project projectName;

  public Consent() {
  }

  public Consent(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getProjectOwner() {
    return projectOwner;
  }

  public void setProjectOwner(String projectOwner) {
    this.projectOwner = projectOwner;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public byte[] getConsentForm() {
    return consentForm;
  }

  public void setConsentForm(byte[] consentForm) {
    this.consentForm = consentForm;
  }

  public Project getProjectName() {
    return projectName;
  }

  public void setProjectName(Project projectName) {
    this.projectName = projectName;
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
    if (!(object instanceof Consent)) {
      return false;
    }
    Consent other = (Consent) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.privacy.model.Consent[ id=" + id + " ]";
  }
 
}
