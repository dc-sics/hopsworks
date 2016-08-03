package se.kth.bbc.jobs;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.JobsHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;

import java.io.IOException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.bbc.jobs.jobhistory.ExecutionInputfilesFacade;
import se.kth.hopsworks.certificates.UserCertsFacade;

/**
 * Utility class for executing a HopsJob asynchronously. Passing the Hopsjob to
 * the method startExecution() will start the HopsJob asynchronously. The
 * HobsJob is supposed to take care of all aspects of execution, such as
 * creating a JobHistory object or processing output.
 * <p/>
 * @author stig
 */
@Stateless
@LocalBean
public class AsynchronousJobExecutor {

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JobOutputFileFacade jobOutputFileFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private JobsHistoryFacade jhf;
  @EJB
  private ExecutionInputfilesFacade executionInputFileFacade;
  @EJB
  private UserCertsFacade userCerts;


  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void startExecution(HopsJob job) {
    job.execute();
  }

  public void stopExecution(HopsJob job, String appid) {
//    job.stop(appid);
  }

  public ExecutionFacade getExecutionFacade() {
    return executionFacade;
  }

  public JobOutputFileFacade getJobOutputFileFacade() {
    return jobOutputFileFacade;
  }
  
  public DistributedFsService getFsService() {
    return dfs;
  }

  public DistributedFileSystemOps getFileOperations(String hdfsUser) throws
          IOException {
    return dfs.getDfsOps(hdfsUser);
  }
  
  public JobsHistoryFacade getJobsHistoryFacade(){
      return jhf;
  }
  
  public ExecutionInputfilesFacade getExecutionInputfilesFacade(){
      return executionInputFileFacade;
  }

  public UserCertsFacade getUserCerts() {
    return userCerts;
  }
  
}
