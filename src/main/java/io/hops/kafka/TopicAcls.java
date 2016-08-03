/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author misdess
 */
@Entity
@Table(name = "topic_acls", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TopicAcls.findAll", query = "SELECT t FROM TopicAcls t"),
    @NamedQuery(name = "TopicAcls.findById", query = "SELECT t FROM TopicAcls t WHERE t.id = :id"),
    @NamedQuery(name = "TopicAcls.findByTopicName", query = "SELECT t FROM TopicAcls t WHERE t.projectTopics.projectTopicsPK.topicName = :topicName"),
    @NamedQuery(name = "TopicAcls.findByPermissionType", query = "SELECT t FROM TopicAcls t WHERE t.permissionType = :permissionType"),
    @NamedQuery(name = "TopicAcls.findByOperationType", query = "SELECT t FROM TopicAcls t WHERE t.operationType = :operationType"),
    @NamedQuery(name = "TopicAcls.findByHost", query = "SELECT t FROM TopicAcls t WHERE t.host = :host"),
    @NamedQuery(name = "TopicAcls.findByRole", query = "SELECT t FROM TopicAcls t WHERE t.role = :role"),
    @NamedQuery(name = "TopicAcls.findByPrincipal", query = "SELECT t FROM TopicAcls t WHERE t.principal = :principal")})
public class TopicAcls implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "permission_type")
    private String permissionType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "operation_type")
    private String operationType;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "host")
    private String host;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "role")
    private String role;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "principal")
    private String principal;
    @JoinColumns({
        @JoinColumn(name = "topic_name", referencedColumnName = "topic_name"),
        @JoinColumn(name = "project_id", referencedColumnName = "project_id")})
    @ManyToOne(optional = false)
    private ProjectTopics projectTopics;
    @JoinColumn(name = "username", referencedColumnName = "email")
    @ManyToOne(optional = false)
    private Users user;

    public TopicAcls() {
    }

    public TopicAcls(Integer id) {
        this.id = id;
    }

    public TopicAcls(ProjectTopics pt, Users user, String permissionType, String operationType, String host, String role, String principal) {
        this.projectTopics = pt;
        this.user = user;
        this.permissionType = permissionType;
        this.operationType = operationType;
        this.host = host;
        this.role = role;
        this.principal = principal;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public ProjectTopics getProjectTopics() {
        return projectTopics;
    }

    public void setProjectTopics(ProjectTopics projectTopics) {
        this.projectTopics = projectTopics;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
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
        if (!(object instanceof TopicAcls)) {
            return false;
        }
        TopicAcls other = (TopicAcls) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.kafka.TopicAcls[ id=" + id + " ]";
    }
    
}
