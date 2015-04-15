/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.model;

import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author AMore
 */
@Entity
@Table(name = "project_history")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProjectHistory.findAll", query = "SELECT p FROM ProjectHistory p"),
    @NamedQuery(name = "ProjectHistory.findById", query = "SELECT p FROM ProjectHistory p WHERE p.id = :id"),
    @NamedQuery(name = "ProjectHistory.findByOperation", query = "SELECT p FROM ProjectHistory p WHERE p.operation = :operation"),
    @NamedQuery(name = "ProjectHistory.findByDate", query = "SELECT p FROM ProjectHistory p WHERE p.date = :date")})
public class ProjectHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 7)
    @Column(name = "operation")
    private String operation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    @JoinColumn(name = "email", referencedColumnName = "email")
    @ManyToOne(optional = false)
    private Users email;
    @JoinColumn(name = "projectID", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Project projectID;

    public ProjectHistory() {
    }

    public ProjectHistory(Integer id) {
        this.id = id;
    }

    public ProjectHistory(Integer id, String operation, Date date) {
        this.id = id;
        this.operation = operation;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Users getEmail() {
        return email;
    }

    public void setEmail(Users email) {
        this.email = email;
    }

    public Project getProjectID() {
        return projectID;
    }

    public void setProjectID(Project projectID) {
        this.projectID = projectID;
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
        if (!(object instanceof ProjectHistory)) {
            return false;
        }
        ProjectHistory other = (ProjectHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.model.ProjectHistory[ id=" + id + " ]";
    }
    
}
