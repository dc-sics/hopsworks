package io.hops.hopsworks.common.dao.device;

import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

public class DeviceFacade {

  private final static Logger LOGGER = Logger.getLogger(DeviceFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void addProjectDevice(Integer projectId, Integer userId,
      String deviceUuid, String passUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    ProjectDevice pd = new ProjectDevice(pdKey, passUuid, userId);
    em.persist(pd);
  }

  public ProjectDevice getProjectDevice(Integer projectId, String deviceUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
        "ProjectDevices.findByProjectDevicePK",
        ProjectDevice.class);
    query.setParameter("projectDevicePK", pdKey);
    return query.getSingleResult();
  }

  public void addProjectSecret(Integer projectId, String projectSecret,
      Integer projectTokenDurationInHours) {
    em.persist(new ProjectSecret(projectId, projectSecret,
        projectTokenDurationInHours));
  }

  public ProjectSecret getProjectSecret(Integer projectId) {
    TypedQuery<ProjectSecret> query = em.createNamedQuery(
        "ProjectSecrets.findByProjectId",
        ProjectSecret.class);
    query.setParameter("projectId", projectId);
    return query.getSingleResult();
  }

}
