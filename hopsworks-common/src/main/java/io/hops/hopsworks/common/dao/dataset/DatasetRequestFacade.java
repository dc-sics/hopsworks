package io.hops.hopsworks.common.dao.dataset;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class DatasetRequestFacade extends AbstractFacade<DatasetRequest> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetRequestFacade() {
    super(DatasetRequest.class);
  }

  @Override
  public List<DatasetRequest> findAll() {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findAll",
            DatasetRequest.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p/>
   * @param id
   * @return
   */
  public DatasetRequest find(Integer id) {
    return em.find(DatasetRequest.class, id);
  }

  /**
   * Finds all requests made on a dataset
   * <p/>
   * @param ds
   * @return
   */
  public List<DatasetRequest> findByDataset(Dataset ds) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByDataset",
            DatasetRequest.class).setParameter(
                    "dataset", ds);
    return query.getResultList();
  }

  /**
   * Find by project member and dataset
   * <p/>
   * @param projectTeam
   * @param ds
   * @return
   */
  public DatasetRequest findByProjectTeamAndDataset(ProjectTeam projectTeam,
          Dataset ds) {
    try {
      return em.createNamedQuery("DatasetRequest.findByProjectTeamAndDataset",
              DatasetRequest.class)
              .setParameter("projectTeam", projectTeam).setParameter(
              "dataset", ds).getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Find by project and dataset
   * <p/>
   * @param project
   * @param ds
   * @return
   */
  public DatasetRequest findByProjectAndDataset(Project project, Dataset ds) {
    try {
      return em.createNamedQuery("DatasetRequest.findByProjectAndDataset",
              DatasetRequest.class)
              .setParameter("project", project).setParameter(
              "dataset", ds).getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Finds all Requests by a project member.
   * <p/>
   * @param projectTeam
   * @return
   */
  public List<DatasetRequest> findByProjectTeam(ProjectTeam projectTeam) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByProjectTeam",
            DatasetRequest.class).setParameter(
                    "projectTeam", projectTeam);
    return query.getResultList();
  }

  /**
   * Finds all Requests by a project member.
   * <p/>
   * @param message
   * @return
   */
  public DatasetRequest findByMessageId(Message message) {
    TypedQuery<DatasetRequest> query = em.createNamedQuery(
            "DatasetRequest.findByMessageId",
            DatasetRequest.class).setParameter(
                    "message_id", message.getId());
    List<DatasetRequest> results = query.getResultList();
    if (results != null && results.size() == 1) {
      return results.get(0);
    } else {
      return null;
    }
  }

  public void persistDataset(DatasetRequest datasetRequest) {
    em.persist(datasetRequest);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(DatasetRequest dataset) {
    em.merge(dataset);
  }

}
