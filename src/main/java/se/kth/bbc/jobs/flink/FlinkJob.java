package se.kth.bbc.jobs.flink;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 * Orchestrates the execution of a Flink job: run job, update history object.
 *
 */
public class FlinkJob extends YarnJob {

     private static final Logger logger = Logger.getLogger(
             FlinkJob.class.getName());
    private final FlinkJobConfiguration jobconfig;
    private final String flinkDir;
    private final String flinkUser;    
    private final String JOBTYPE_STREAMING = "Streaming";
    /**
     *
     * @param job
     * @param services
     * @param user
     * @param hadoopDir
     * @param flinkDir
     * @param flinkConfDir
     * @param flinkConfFile
     * @param nameNodeIpPort
     * @param flinkUser
   * @param jobUser
     * @param kafkaAddress
     */
    public FlinkJob(JobDescription job, AsynchronousJobExecutor services,
            Users user, final String hadoopDir,
            final String flinkDir, final String flinkConfDir, 
            final String flinkConfFile,final String nameNodeIpPort, 
            String flinkUser, String jobUser, String kafkaAddress) {
        super(job, services, user, jobUser, hadoopDir, nameNodeIpPort,
                kafkaAddress);
        if (!(job.getJobConfig() instanceof FlinkJobConfiguration)) {
            throw new IllegalArgumentException(
                    "JobDescription must contain a FlinkJobConfiguration object. Received: "
                    + job.getJobConfig().getClass());
        }
        this.jobconfig = (FlinkJobConfiguration) job.getJobConfig();
        this.jobconfig.setFlinkConfDir(flinkConfDir);
        this.jobconfig.setFlinkConfFile(flinkConfFile);
        this.flinkDir = flinkDir;
        this.flinkUser = flinkUser;
    }

    @Override
    protected boolean setupJob(DistributedFileSystemOps dfso) {
        //Then: actually get to running.
        if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
            jobconfig.setAppName("Untitled Flink Job");
        }
        
        FlinkYarnRunnerBuilder flinkBuilder = new FlinkYarnRunnerBuilder(
                jobconfig.getJarPath(), jobconfig.getMainClass());
        //https://ci.apache.org/projects/flink/flink-docs-release-0.10/setup/yarn_setup.html
        /*If you do not want to keep the Flink YARN client running all the time, 
         its also possible to start a detached YARN session. The parameter for
         that is called -d or --detached. In that case, the Flink YARN client 
         will only submit Flink to the cluster and then close itself.
         */
        flinkBuilder.setDetachedMode(false);
        flinkBuilder.setName(jobconfig.getAppName());
        flinkBuilder.setConfigurationDirectory(jobconfig.getFlinkConfDir());
        flinkBuilder.setConfigurationFilePath(new Path(
                jobconfig.getFlinkConfFile()));
        //Flink specific conf object
        flinkBuilder.setFlinkLoggingConfigurationPath(new Path(
                jobconfig.getFlinkConfDir()));
//        flinkBuilder.setLocalJarPath(new Path("hdfs://"+nameNodeIpPort+
//                "/user/"+flinkUser+"/"+Settings.FLINK_LOCRSC_FLINK_JAR));
        
        flinkBuilder.setTaskManagerMemory(jobconfig.getTaskManagerMemory());
        flinkBuilder.setTaskManagerSlots(jobconfig.getSlots());
        flinkBuilder.setTaskManagerCount(jobconfig.getNumberOfTaskManagers());
        if(jobconfig.getFlinkjobtype().equals(JOBTYPE_STREAMING)){
            flinkBuilder.setStreamingMode(true);
        }
        flinkBuilder.setParallelism(jobconfig.getParallelism());
        flinkBuilder.setJobManagerMemory(jobconfig.getAmMemory());
        flinkBuilder.setJobManagerCores(jobconfig.getAmVCores());
        flinkBuilder.setJobManagerQueue(jobconfig.getAmQueue());
        flinkBuilder.setAppJarPath(jobconfig.getJarPath());
        flinkBuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
        if(jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()){
            String[] jobArgs = jobconfig.getArgs().trim().split(" ");
            flinkBuilder.addAllJobArgs(jobArgs);
        } 
        try {
            runner = flinkBuilder.
           getYarnRunner(jobDescription.getProject().getName(),
               flinkUser, jobUser, hadoopDir, flinkDir, nameNodeIpPort);

        } catch (IOException e) {
          logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
          writeToLogs(new IOException("Failed to start Yarn client.", e));
          return false;
        } 


        String stdOutFinalDestination = Utils.getHdfsRootPath(hadoopDir,
                jobDescription.
                getProject().
                getName())
                + Settings.FLINK_DEFAULT_OUTPUT_PATH + getExecution().getId()
                + File.separator + "stdout.log";
        String stdErrFinalDestination = Utils.getHdfsRootPath(hadoopDir,
                jobDescription.
                getProject().
                getName())
                + Settings.FLINK_DEFAULT_OUTPUT_PATH + getExecution().getId()
                + File.separator + "stderr.log";
        setStdOutFinalDestination(stdOutFinalDestination);
        setStdErrFinalDestination(stdErrFinalDestination);
        return true;
    }

    @Override
    protected void cleanup() {
        logger.log(Level.INFO, "Job finished performing cleanup...");
        if (monitor != null) {
          monitor.close();
          monitor = null;
        }
    }

}
