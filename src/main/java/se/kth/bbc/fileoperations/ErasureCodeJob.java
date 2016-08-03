package se.kth.bbc.fileoperations;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.user.model.Users;

/**
 *
 */
public class ErasureCodeJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(ErasureCodeJob.class.
          getName());
  private ErasureCodeJobConfiguration jobConfig;

  public ErasureCodeJob(JobDescription job, AsynchronousJobExecutor services,
          Users user,
          String hadoopDir, String nameNodeIpPort) {

    super(job, services, user, hadoopDir, nameNodeIpPort);

    if (!(job.getJobConfig() instanceof ErasureCodeJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain an ErasureCodeJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }

    this.jobConfig = (ErasureCodeJobConfiguration) job.getJobConfig();
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    if (jobConfig.getAppName() == null || jobConfig.getAppName().isEmpty()) {
      jobConfig.setAppName("Untitled Erasure coding Job");
    }

    return true;
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso) {
    boolean jobSucceeded = false;
    try {
      //do compress the file
      jobSucceeded = dfso.compress(this.jobConfig.
              getFilePath());
    } catch (IOException | NotFoundException e) {
      jobSucceeded = false;
    }
    if (jobSucceeded) {
      //TODO: push a message to the messaging service
      logger.log(Level.INFO, "File compression was successful");
      return;
    }
    //push message to the messaging service
    logger.log(Level.INFO, "File compression was not successful");
  }

  @Override
  protected void stopJob(String appid) {

  }

  @Override
  protected void cleanup() {
  }

}
