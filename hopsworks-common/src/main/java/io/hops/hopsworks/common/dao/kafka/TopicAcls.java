/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.kafka;

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
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "topic_acls",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TopicAcls.findAll",
      query = "SELECT t FROM TopicAcls t")
  ,
  @NamedQuery(name = "TopicAcls.findById",
      query = "SELECT t FROM TopicAcls t WHERE t.id = :id")
  ,
  @NamedQuery(name = "TopicAcls.findByTopicName",
      query
      = "SELECT t FROM TopicAcls t WHERE t.projectTopics.projectTopicsPK.topicName = :topicName")
  ,
  @NamedQuery(name = "TopicAcls.findByPermissionType",
      query
      = "SELECT t FROM TopicAcls t WHERE t.permissionType = :permissionType")
  ,
  @NamedQuery(name = "TopicAcls.findByOperationType",
      query
      = "SELECT t FROM TopicAcls t WHERE t.operationType = :operationType")
  ,
  @NamedQuery(name = "TopicAcls.findByHost",
      query = "SELECT t FROM TopicAcls t WHERE t.host = :host")
  ,
  @NamedQuery(name = "TopicAcls.findByRole",
      query = "SELECT t FROM TopicAcls t WHERE t.role = :role")
  ,
  @NamedQuery(name = "TopicAcls.findByPrincipal",
      query = "SELECT t FROM TopicAcls t WHERE t.principal = :principal")
  ,
    @NamedQuery(name = "TopicAcls.findAcl",
      query
      = "SELECT t FROM TopicAcls t WHERE t.principal = :principal AND t.role = :role AND t.host = :host AND "
          + "t.operationType = :operationType AND t.permissionType = :permissionType AND "
          + "t.projectTopics.projectTopicsPK.topicName = :topicName"),
  @NamedQuery(name = "TopicAcls.deleteByUser",
      query = "DELETE FROM TopicAcls t WHERE t.user = :user AND t.projectTopics.projectTopicsPK.projectId = " +
          ":projectId")})
public class TopicAcls implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "permission_type")
  private String permissionType;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "operation_type")
  private String operationType;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "host")
  private String host;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "role")
  private String role;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 100)
  @Column(name = "principal")
  private String principal;
  @JoinColumns({
    @JoinColumn(name = "topic_name",
        referencedColumnName = "topic_name")
    ,
    @JoinColumn(name = "project_id",
        referencedColumnName = "project_id")})
  @ManyToOne(optional = false)
  private ProjectTopics projectTopics;
  @JoinColumn(name = "username",
      referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users user;

  public TopicAcls() {
  }

  public TopicAcls(Integer id) {
    this.id = id;
  }

  public TopicAcls(ProjectTopics pt, Users user, String permissionType,
      String operationType, String host, String role, String principal) {
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
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
        equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.TopicAcls[ id=" + id + " ]";
  }

}
