/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SharedTopics;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class KafkaController {

  private final static Logger logger = Logger.getLogger(KafkaController.class.getName());

  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private CertsFacade userCerts;
  @EJB
  private Settings settings;

  public String getKafkaCertPaths(Project project) {
    UserCerts userCert = userCerts.findUserCert(project.getName(), project.
        getOwner().getUsername());
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null
        && userCert.getUserCert().length > 0
        && userCert.getUserKey() != null
        && userCert.getUserKey().length > 0) {

      File certDir = new File(settings.getHopsworksTrueTempCertDir() + "/" + project.getName());

      if (!certDir.exists()) {
        try {
          certDir.mkdirs();
        } catch (Exception ex) {

        }
      }
      try {
        FileOutputStream fos;
        fos = new FileOutputStream(certDir.getAbsolutePath() + "/keystore.jks");
        fos.write(userCert.getUserKey());
        fos.close();

        fos = new FileOutputStream(certDir.getAbsolutePath() + "/truststore.jks");
        fos.write(userCert.getUserCert());
        fos.close();

      } catch (Exception e) {

      }
      return certDir.getAbsolutePath();
    } else {
      return null;
    }
  }

  /**
   * Add a new project member to all project's Kafka topics.
   *
   * @param project
   * @param member
   * @throws AppException
   */
  public void addProjectMemberToTopics(Project project, String member) throws AppException {
    //Get all topics (shared with project as well)
    List<TopicDTO> topics = kafkaFacade.findTopicsByProject(project.getId());
    List<SharedTopics> sharedTopics = kafkaFacade.findSharedTopicsByProject(project.getId());
    //For every topic that has been shared with the current project, add the new member to its ACLs
    for (SharedTopics sharedTopic : sharedTopics) {
      kafkaFacade.addAclsToTopic(sharedTopic.getSharedTopicsPK().getTopicName(), sharedTopic.getProjectId(),
          new AclDTO(project.getName(), member, "allow",
              Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD));
    }

    //Iterate over topics and add user to ACLs 
    for (TopicDTO topic : topics) {
      kafkaFacade.addAclsToTopic(topic.getName(), project.getId(), new AclDTO(project.getName(), member, "allow",
          Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD));
    }
  }
  
  public void removeProjectMemberFromTopics(Project project, Users user) throws AppException {
    //Get all topics (shared with project as well)
    List<SharedTopics> sharedTopics = kafkaFacade.findSharedTopicsByProject(project.getId());
    //For every topic that has been shared with the current project, add the new member to its ACLs
    List<Integer> projectSharedTopics = new ArrayList<>();

    //Get all projects from which topics have been shared
    for (SharedTopics sharedTopic : sharedTopics) {
      projectSharedTopics.add(sharedTopic.getProjectId());
    }

    if (!projectSharedTopics.isEmpty()) {
      for (Integer projectId : projectSharedTopics) {
        kafkaFacade.removeAclsForUser(user, projectId);
      }
    }

    //Remove acls for use in current project
    kafkaFacade.removeAclsForUser(user, project.getId());
  }
  
  /**
   * Get all shared Topics for the given project.
   *
   * @param projectId
   * @return
   */
  public List<TopicDTO> findSharedTopicsByProject(Integer projectId) {
    List<SharedTopics> res = kafkaFacade.findSharedTopicsByProject(projectId);
    List<TopicDTO> topics = new ArrayList<>();
    for (SharedTopics pt : res) {
      topics.add(new TopicDTO(pt.getSharedTopicsPK().getTopicName()));
    }
    return topics;
  }

}
