package io.hops.hopsworks.common.dao.jobs.description;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

/**
 * Facade for management of persistent Jobs objects.
 */
@Stateless
public class JobFacade extends AbstractFacade<Jobs> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger logger = Logger.getLogger(JobFacade.class.
      getName());

  public JobFacade() {
    super(Jobs.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the jobs in this project with the given type.
   * <p/>
   * @param project
   * @param type
   * @return
   */
  public List<Jobs> findJobsForProjectAndType(
      Project project, JobType type) {
    TypedQuery<Jobs> q = em.createNamedQuery("Jobs.findByProjectAndType",
        Jobs.class);
    q.setParameter("project", project);
    q.setParameter("type", type);
    return q.getResultList();
  }

  /**
   * Find all the jobs defined in the given project.
   * <p/>
   * @param project
   * @return
   */
  public List<Jobs> findForProject(Project project) {
    TypedQuery<Jobs> q = em.createNamedQuery("Jobs.findByProject", Jobs.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Create a new Jobs instance.
   * <p/>
   * @param creator The creator of the job.
   * @param project The project in which this job is defined.
   * @param config The job configuration file.
   * @return
   * @throws IllegalArgumentException If the JobConfiguration object is not
   * parseable to a known class.
   * @throws NullPointerException If any of the arguments user, project or
   * config are null.
   */
  //This seems to ensure that the entity is actually created and can later 
  //be found using em.find().
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Jobs create(Users creator, Project project,
      JobConfiguration config) throws
      IllegalArgumentException, NullPointerException {
    //Argument checking
    if (creator == null || project == null || config == null) {
      throw new NullPointerException(
          "Owner, project and config must be non-null.");
    }
    //First: create a job object
    Jobs job = new Jobs(config, project, creator, config.
        getAppName());
    //Finally: persist it, getting the assigned id.
    em.persist(job);
    em.flush(); //To get the id.
    return job;
  }

  /**
   * Find the Jobs with given id.
   * <p/>
   * @param id
   * @return The found entity or null if no such exists.
   */
  public Jobs findById(Integer id) {
    return em.find(Jobs.class, id);
  }

  /**
   *
   * @param job
   * @throws DatabaseException
   */
  public void removeJob(Jobs job) throws DatabaseException {
    try {
      Jobs managedJob = em.find(Jobs.class, job.getId());
      em.remove(em.merge(managedJob));
      em.flush();
    } catch (SecurityException | IllegalStateException ex) {
      throw new DatabaseException("Could not delete job " + job.getName(), ex);
    }

  }

  /**
   *
   * @param jobId
   * @param schedule
   * @return
   * @throws DatabaseException
   */
  public boolean updateJobSchedule(int jobId, ScheduleDTO schedule) throws
      DatabaseException {
    boolean status = false;
    try {
      Jobs managedJob = em.find(Jobs.class, jobId);
      JobConfiguration config = managedJob.getJobConfig();
      config.setSchedule(schedule);
      TypedQuery<Jobs> q = em.createNamedQuery("Jobs.updateConfig", Jobs.class);
      q.setParameter("id", jobId);
      q.setParameter("jobconfig", config);
      int result = q.executeUpdate();
      logger.log(Level.INFO, "Updated entity count = {0}", result);
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      throw new DatabaseException("Could not update job  ", ex);
    }
    return status;
  }

  /**
   *
   * @param project
   * @return
   */
  public List<Jobs> getRunningJobs(Project project) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findJobsForExecutionInState", Jobs.class);
    q.setParameter("project", project);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

  /**
   *
   * @param project
   * @param hdfsUser
   * @return
   */
  public List<Jobs> getRunningJobs(Project project, String hdfsUser) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findUserJobsForExecutionInState", Jobs.class);
    q.setParameter("project", project);
    q.setParameter("hdfsUser", hdfsUser);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

  /**
   *
   * @param project
   * @param hdfsUser
   * @param jobIds
   * @return
   */
  public List<Jobs> getRunningJobs(Project project, String hdfsUser, List<Integer> jobIds) {
    TypedQuery<Jobs> q = em.createNamedQuery("Execution.findUserJobsIdsForExecutionInState", Jobs.class);
    q.setParameter("jobids", jobIds);
    q.setParameter("project", project);
    q.setParameter("hdfsUser", hdfsUser);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

}
