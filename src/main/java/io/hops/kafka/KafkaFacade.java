package io.hops.kafka;

import io.hops.metadata.hdfs.entity.User;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import se.kth.bbc.project.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.util.LocalhostServices;
import se.kth.hopsworks.util.Settings;

@Stateless
public class KafkaFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;

  protected EntityManager getEntityManager() {
    return em;
  }

  public KafkaFacade() {
  }

  /**
   * Get all the Topics for the given project.
   * <p/>
   * @param project
   * @return
   */
  public List<TopicDTO> findTopicsByProject(Project project) {
    TypedQuery<ProjectTopics> query = em.createNamedQuery(
        "ProjectTopics.findByProject",
        ProjectTopics.class);
    query.setParameter("project", project);
    List<ProjectTopics> res = query.getResultList();
    List<TopicDTO> topics = new ArrayList<>();
    for (ProjectTopics pt : res) {
      topics.add(new TopicDTO(pt.getTopicName()));
    }
    return topics;
  }

  public TopicDTO getTopicDetails(Project project, String topicName) throws AppException {
    List<TopicDTO> topics = findTopicsByProject(project);
    if (topics.isEmpty()) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No Kafka topics found in this project.");
    }
    for (TopicDTO t : topics) {
      if (t.getTopic().compareToIgnoreCase(topicName) == 0) {
        // TODO - Go to Kafka and get the details for this topic
        // Set all the attribute fields in the DTO, then return it.
        // t.setXX()
        String zkIpPort = settings.getZkIp();
        return t;
      }
    }

    throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No Kafka topics found in this project.");
  }

  private int getPort(String zkIp) {
            // TODO parse the zkIp from "ip:port" to a "ip", "port" pair.
            return 0;
  }
  private InetAddress getIp(String zkIp) throws AppException {
    // TODO
    String ip = "";
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException ex) {
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "Zookeeper service is not available right now...");
    }
  }
  
  public Project findProjectforTopic(String topicName) throws AppException {
    ProjectTopics pt = em.find(ProjectTopics.class, topicName);
    if (pt == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No project found for this Kafka topic.");
    }
    Project proj = pt.getProject();
    if (proj == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No project found for this Kafka topic.");
    }
    return proj;
  }

  public void createTopicInProject(Project project, String topicName) throws AppException {
    ProjectTopics pt = em.find(ProjectTopics.class, topicName);
    if (pt != null) {
      throw new AppException(Response.Status.FOUND.getStatusCode(), "Kafka topic already exists. Pick a different topic name.");
    }
    
    // TODO - create the topic in kafka
    
    pt = new ProjectTopics(topicName, project);
    em.merge(pt);
    em.persist(pt);
    em.flush();
  }

  @Asynchronous
  public void createHopsUserSslCert(User user, Project project) throws IOException {

    String stdout = LocalhostServices.createSslUserCert(user.getName(), project.getName(), settings.getGlassfishDir());

  }

}
