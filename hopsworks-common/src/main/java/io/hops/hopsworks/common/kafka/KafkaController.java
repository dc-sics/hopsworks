package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileOutputStream;
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
  private ProjectServiceFacade projectServicesFacade;
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
    topics.addAll(kafkaFacade.findSharedTopicsByProject(project.getId()));

    //Iterate over topics and add user to ACLs 
    for (TopicDTO topic : topics) {
      kafkaFacade.addAclsToTopic(topic.getName(), project.getId(), new AclDTO(project.getName(), member, "allow",
          Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD, Settings.KAFKA_ACL_WILDCARD));
    }
  }

}
