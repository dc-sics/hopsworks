package io.hops;

import io.hops.metadata.hdfs.entity.User;
import java.io.IOException;
import se.kth.bbc.project.*;
import java.util.List;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.hopsworks.util.LocalhostServices;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author roshan
 */
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
  public List<ProjectTopics> findTopicsByProject(Project project) {
    TypedQuery<ProjectTopics> query = em.createNamedQuery(
        "ProjectTopics.findByProject",
        ProjectTopics.class);
    query.setParameter("project", project);
    return query.getResultList();
  }

  public ProjectTopics findByTopicName(String topicName) {
    return em.find(ProjectTopics.class, topicName);
  }

  @Asynchronous
  public String createHopsUserSslCert(User user, Project project) throws IOException {

    String stdout = LocalhostServices.createSslUserCert(user.getName(), project.getName(), settings.getGlassfishDir());
    
    return stdout;
  }

}
