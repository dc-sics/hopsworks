package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO.TimeUnit;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * Allow jobs to be scheduled and take care of their execution.
 */
@Stateless
public class JobScheduler {

  private static final Logger logger = Logger.getLogger(JobScheduler.class.
          getName());

  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private ExecutionController executionController;
  @Resource
  private TimerService timerService;

  /**
   * Execute the job given as extra info to the timer object.
   * <p/>
   * @param timer
   */
  @Timeout
  public void timeout(Timer timer) {
    Serializable jobId = timer.getInfo();
    //Valid id?
    if (!(jobId instanceof Integer)) {
      logger.log(Level.WARNING,
              "Trying to run a scheduled execution, but info is not of integer class.");
      return;
    }
    //Valid job?
    JobDescription job = jobFacade.findById((Integer) jobId);
    if (job == null) {
      logger.log(Level.WARNING, "Trying to run a job with non-existing id.");
      return;
    }
    try {
      //Yes! Now execute!
      executionController.start(job, job.getCreator(), null);
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Exception while starting scheduled job " + job,
              ex);
    }
  }

  /**
   * Schedule a job for periodic execution. The first execution will occur after
   * the specified interval.
   * <p/>
   * @param job
   * @param numberOfUnits
   * @param timeUnit
   */
  public void scheduleJobPeriodic(JobDescription job, int numberOfUnits,
          TimeUnit timeUnit) {
    long interval = numberOfUnits * timeUnit.getDuration();
    timerService.createTimer(new Date().getTime() + interval, interval, job.
            getId());
  }

  /**
   * Schedule a job for a single execution, at the given date.
   * <p/>
   * @param job
   * @param when
   */
  public void scheduleJobOnce(JobDescription job, Date when) {
    TimerConfig config = new TimerConfig();
    config.setInfo(job.getId());
    timerService.createSingleActionTimer(when, config);
  }

  /**
   * Schedule a job for regular execution.
   * <p/>
   * @param job
   * @param when The ScheduleExpression dictating when the job should be run.
   */
  public void scheduleJobOnCalendar(JobDescription job, ScheduleExpression when) {
    TimerConfig config = new TimerConfig();
    config.setInfo(job.getId());
    timerService.createCalendarTimer(when, config);
  }

  /**
   * Schedule the given job according to the JobSchedule contained in its
   * configuration.
   * <p/>
   * @param job
   * @throws NullPointerException If the job or its contained schedule are null.
   */
  public void scheduleJobPeriodic(JobDescription job) {
    //First: parameter checking
    if (job == null) {
      throw new NullPointerException("Cannot schedule null job.");
    } else if (job.getJobConfig().getSchedule() == null) {
      throw new NullPointerException(
              "Trying to schedule a job with null schedule: " + job);
    }
    //Then: set up interval timer
    ScheduleDTO schedule = job.getJobConfig().getSchedule();
    timerService.createTimer(new Date(schedule.getStart()), schedule.getNumber()
            * schedule.getUnit().getDuration(), job.getId());
  }

}
