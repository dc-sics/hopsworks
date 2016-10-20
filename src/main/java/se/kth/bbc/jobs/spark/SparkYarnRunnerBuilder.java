package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.util.Settings;

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 *
 * @author stig
 */
public class SparkYarnRunnerBuilder {

  //Necessary parameters
  private final String appJarPath, mainClass;

  //Optional parameters
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled Spark Job";
  private List<LocalResourceDTO> extraFiles = new ArrayList<>();
  private int numberOfExecutors = 1;
  private int numberOfExecutorsMin = Settings.SPARK_MIN_EXECS;
  private int numberOfExecutorsMax = Settings.SPARK_MAX_EXECS;
  private int numberOfExecutorsInit = Settings.SPARK_INIT_EXECS;
  private int executorCores = 1;
  private boolean dynamicExecutors;
  private String executorMemory = "512m";
  private int driverMemory = 1024; // in MB
  private int driverCores = 1;
  private String driverQueue;
  private final Map<String, String> envVars = new HashMap<>();
  private final Map<String, String> sysProps = new HashMap<>();
  private String classPath;
  private String sessionId;//used by Kafka
  private String kafkaAddress;
  private boolean kafkaJob;
  private String kafkaTopics;

  private String restEndpoint;
  private JobType jobType;

  public SparkYarnRunnerBuilder(String appJarPath, String mainClass,
          JobType jobType) {
    if (appJarPath == null || appJarPath.isEmpty()) {
      throw new IllegalArgumentException(
              "Path to application jar cannot be empty!");
    }
    if (mainClass == null || mainClass.isEmpty()) {
      throw new IllegalArgumentException(
              "Name of the main class cannot be empty!");
    }
    this.appJarPath = appJarPath;
    this.mainClass = mainClass;
    this.jobType = jobType;
  }

  /**
   * Get a YarnRunner instance that will launch a Spark job.
   * <p/>
   * @param project name of the project
   * @param sparkUser
   * @param jobUser
   * @param hadoopDir
   * @param sparkDir
   * @param nameNodeIpPort
   * @return The YarnRunner instance to launch the Spark job on Yarn.
   * @throws IOException If creation failed.
   */
  public YarnRunner getYarnRunner(String project, String sparkUser,
          String jobUser,
          final String hadoopDir, final String sparkDir,
          final String nameNodeIpPort)
          throws IOException {

    String hdfsSparkJarPath = Settings.getHdfsSparkJarPath(sparkUser);
    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    builder.setJobType(jobType);

    String stagingPath = File.separator + "Projects" + File.separator + project
            + File.separator
            + Settings.PROJECT_STAGING_DIR + File.separator
            + YarnRunner.APPID_PLACEHOLDER;
    builder.localResourcesBasePath(stagingPath);

    builder.addLocalResource(new LocalResourceDTO(
            "__spark_libs__", hdfsSparkJarPath,
            LocalResourceVisibility.PRIVATE.toString(),
            LocalResourceType.ARCHIVE.toString(), null), false);

    //Add app jar  
    builder.addLocalResource(new LocalResourceDTO(
            Settings.SPARK_LOCRSC_APP_JAR, appJarPath,
            LocalResourceVisibility.APPLICATION.toString(),
            LocalResourceType.FILE.toString(), null),
            !appJarPath.startsWith("hdfs:"));
    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH, "$PWD");
    StringBuilder extraClassPathFiles = new StringBuilder();

    //Add extra files to local resources, use filename as key
    for (LocalResourceDTO dto : extraFiles) {
      if (dto.getName().equals(Settings.KAFKA_K_CERTIFICATE) || dto.getName().
              equals(Settings.KAFKA_T_CERTIFICATE)) {
        //Set deletion to true so that certs are removed
        builder.addLocalResource(dto, true);
      } else {
        builder.addLocalResource(dto, !appJarPath.startsWith("hdfs:"));
      }
      builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
              dto.getName());
      extraClassPathFiles.append(dto.getName()).append(File.pathSeparator);

    }
    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
            "$PWD/__spark_conf__:__spark_conf__:__spark_libs__/*"
            + ":" + Settings.SPARK_LOCRSC_APP_JAR
    );
    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", jobUser);
    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    if (extraClassPathFiles.length() > 0) {
      addSystemProperty(Settings.SPARK_EXECUTOR_EXTRACLASSPATH,
              extraClassPathFiles.
              toString().substring(0, extraClassPathFiles.length() - 1));
    }
    addSystemProperty(Settings.KAFKA_SESSIONID_ENV_VAR, sessionId);
    addSystemProperty(Settings.KAFKA_BROKERADDR_ENV_VAR, kafkaAddress);
    addSystemProperty(Settings.KAFKA_JOB_ENV_VAR, Boolean.toString(kafkaJob));
    addSystemProperty(Settings.KAFKA_JOB_TOPICS_ENV_VAR, kafkaTopics);

    addSystemProperty(Settings.KAFKA_REST_ENDPOINT_ENV_VAR, restEndpoint);
    //If DynamicExecutors are not enabled, set the user defined number 
    //of executors
    if (dynamicExecutors) {
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_ENV, String.valueOf(
              dynamicExecutors));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
              String.valueOf(numberOfExecutorsMin));
      //TODO: Fill in the init and max number of executors. Should it be a per job
      //or global setting?
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
              String.valueOf(numberOfExecutorsMax));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
              String.valueOf(numberOfExecutorsInit));
      //Dynamic executors requires the shuffle service to be enabled
      addSystemProperty(Settings.SPARK_SHUFFLE_SERVICE, "true");
      //spark.shuffle.service.enabled
    } else {
      addSystemProperty(Settings.SPARK_NUMBER_EXECUTORS_ENV, Integer.toString(
              numberOfExecutors));
    }

    List<String> jobSpecificProperties = new ArrayList<>();
    jobSpecificProperties.add(Settings.KAFKA_SESSIONID_ENV_VAR);
    jobSpecificProperties.add(Settings.KAFKA_BROKERADDR_ENV_VAR);
    jobSpecificProperties.add(Settings.SPARK_NUMBER_EXECUTORS_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_CORES_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_CORES_ENV);

    //These properties are set sot that spark history server picks them up
    addSystemProperty(Settings.SPARK_DRIVER_MEMORY_ENV, Integer.toString(
            driverMemory) + "m");
    addSystemProperty(Settings.SPARK_DRIVER_CORES_ENV, Integer.toString(
            driverCores));
    addSystemProperty(Settings.SPARK_EXECUTOR_MEMORY_ENV, executorMemory);
    addSystemProperty(Settings.SPARK_EXECUTOR_CORES_ENV, Integer.toString(
            executorCores));

    //Set executor extraJavaOptions to make parameters available to executors
    builder.addJavaOption("'-Dspark.executor.extraJavaOptions="
            + "-Dlog4j.configuration=/srv/spark/conf/executor-log4j.properties "
            + "-XX:+PrintReferenceGC -verbose:gc -XX:+PrintGCDetails "
            + "-XX:+PrintGCTimeStamps -XX:+PrintAdaptiveSizePolicy "
            + "-Djava.library.path=/srv/hadoop/lib/native/ -D"
            + Settings.KAFKA_SESSIONID_ENV_VAR + "=" + sessionId + " -D"
            + Settings.KAFKA_BROKERADDR_ENV_VAR + "=" + kafkaAddress + " -D"
            + Settings.KAFKA_JOB_TOPICS_ENV_VAR + "=" + kafkaTopics + " -D"
            + Settings.KAFKA_REST_ENDPOINT_ENV_VAR + "=" + restEndpoint + " -D"
            + Settings.KAFKA_PROJECTID_ENV_VAR + "=" + sysProps.get(
                    Settings.KAFKA_PROJECTID_ENV_VAR) + "'");
    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(mainClass);

    Properties sparkProperties = new Properties();
    InputStream is = null;
    try {
      is = new FileInputStream(sparkDir + "/" + Settings.SPARK_CONFIG_FILE);
      sparkProperties.load(is);
      //For every property that is in the spark configuration file but is not
      //already set, create a java system property.
      for (String property : sparkProperties.stringPropertyNames()) {
        if (!jobSpecificProperties.contains(property) && sparkProperties.
                getProperty(property) != null && !sparkProperties.getProperty(
                property).isEmpty()) {
          addSystemProperty(property,
                  sparkProperties.getProperty(property).trim());
        }
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
    for (String s : sysProps.keySet()) {
      String option = escapeForShell("-D" + s + "=" + sysProps.get(s));
      builder.addJavaOption(option);
    }

    //Add local resources to spark environment too
    for (String s : jobArgs) {
      amargs.append(" --arg ").append(s);
    }
    //amargs.append(" --properties-file __spark_conf__/spark-defaults.conf");
    builder.amArgs(amargs.toString());

    //Set up Yarn properties
    builder.amMemory(driverMemory);
    builder.amVCores(driverCores);
    builder.amQueue(driverQueue);

    //Set app name
    builder.appName(jobName);

    return builder.build(hadoopDir, sparkDir, nameNodeIpPort, JobType.SPARK);
  }

  public SparkYarnRunnerBuilder setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(List<String> jobArgs) {
    this.jobArgs.addAll(jobArgs);
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }

  public SparkYarnRunnerBuilder addJobArg(String jobArg) {
    jobArgs.add(jobArg);
    return this;
  }

  public SparkYarnRunnerBuilder setExtraFiles(List<LocalResourceDTO> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFile(LocalResourceDTO dto) {
    if (dto.getName() == null || dto.getName().isEmpty()) {
      throw new IllegalArgumentException(
              "Filename in extra file mapping cannot be null or empty.");
    }
    if (dto.getPath() == null || dto.getPath().isEmpty()) {
      throw new IllegalArgumentException(
              "Location in extra file mapping cannot be null or empty.");
    }
    this.extraFiles.add(dto);
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFiles(
          List<LocalResourceDTO> projectLocalResources) {
    if (projectLocalResources != null && !projectLocalResources.isEmpty()) {
      this.extraFiles.addAll(projectLocalResources);
    }
    return this;
  }

  public SparkYarnRunnerBuilder setNumberOfExecutors(int numberOfExecutors) {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
              "Number of executors cannot be less than 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorCores(int executorCores) {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
              "Number of executor cores cannot be less than 1.");
    }
    this.executorCores = executorCores;
    return this;
  }

  public boolean isDynamicExecutors() {
    return dynamicExecutors;
  }

  public void setDynamicExecutors(boolean dynamicExecutors) {
    this.dynamicExecutors = dynamicExecutors;
  }

  public int getNumberOfExecutorsMin() {
    return numberOfExecutorsMin;
  }

  public void setNumberOfExecutorsMin(int numberOfExecutorsMin) {
    this.numberOfExecutorsMin = numberOfExecutorsMin;
  }

  public int getNumberOfExecutorsMax() {
    return numberOfExecutorsMax;
  }

  public void setNumberOfExecutorsMax(int numberOfExecutorsMax) {
    if (numberOfExecutorsMax > Settings.SPARK_MAX_EXECS) {
      throw new IllegalArgumentException(
              "Maximum number of  executors cannot be greate than:"
              + Settings.SPARK_MAX_EXECS);
    }
    this.numberOfExecutorsMax = numberOfExecutorsMax;
  }

  public int getNumberOfExecutorsInit() {
    return numberOfExecutorsInit;
  }

  public void setNumberOfExecutorsInit(int numberOfExecutorsInit) {
    this.numberOfExecutorsInit = numberOfExecutorsInit;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryMB(int executorMemoryMB) {
    if (executorMemoryMB < 1) {
      throw new IllegalArgumentException(
              "Executor memory bust be greater than zero.");
    }
    this.executorMemory = "" + executorMemoryMB + "m";
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryGB(float executorMemoryGB) {
    if (executorMemoryGB <= 0) {
      throw new IllegalArgumentException(
              "Executor memory must be greater than zero.");
    }
    int mem = (int) (executorMemoryGB * 1024);
    this.executorMemory = "" + mem + "m";
    return this;
  }

  /**
   * Set the memory requested for each executor. The given string should have
   * the form of a number followed by a 'm' or 'g' signifying the metric.
   * <p/>
   * @param memory
   * @return
   */
  public SparkYarnRunnerBuilder setExecutorMemory(String memory) {
    memory = memory.toLowerCase();
    if (!memory.endsWith("m") && !memory.endsWith("g")) {
      throw new IllegalArgumentException(
              "Memory string does not follow the necessary format.");
    } else {
      String memnum = memory.substring(0, memory.length() - 1);
      try {
        Integer.parseInt(memnum);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
                "Memory string does not follow the necessary format.", e);
      }
    }
    this.executorMemory = memory;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryMB(int driverMemoryMB) {
    if (driverMemoryMB < 1) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    this.driverMemory = driverMemoryMB;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryGB(int driverMemoryGB) {
    if (driverMemoryGB <= 0) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    int mem = driverMemoryGB * 1024;
    this.driverMemory = mem;
    return this;
  }

  public void setDriverCores(int driverCores) {
    this.driverCores = driverCores;
  }

  public void setDriverQueue(String driverQueue) {
    this.driverQueue = driverQueue;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setKafkaAddress(String kafkaAddress) {
    this.kafkaAddress = kafkaAddress;
  }

  public void setKafkaJob(boolean isKafkaJob) {
    this.kafkaJob = isKafkaJob;
  }

  public void setKafkaTopics(String kafkaTopics) {
    this.kafkaTopics = kafkaTopics;
  }

  public void setRestEndpoint(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }

  public SparkYarnRunnerBuilder addEnvironmentVariable(String name, String value) {
    envVars.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addSystemProperty(String name, String value) {
    sysProps.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addToClassPath(String s) {
    if (classPath == null || classPath.isEmpty()) {
      classPath = s;
    } else {
      classPath = classPath + ":" + s;
    }
    return this;
  }

  /**
   * Taken from Apache Spark code: Escapes a string for inclusion in a command
   * line executed by Yarn. Yarn executes commands
   * using `bash -c "command arg1 arg2"` and that means plain quoting doesn't
   * really work. The
   * argument is enclosed in single quotes and some key characters are escaped.
   * <p/>
   * @param s A single argument.
   * @return Argument quoted for execution via Yarn's generated shell script.
   */
  public static String escapeForShell(String s) {
    if (s != null) {
      StringBuilder escaped = new StringBuilder("'");
      for (int i = 0; i < s.length(); i++) {
        switch (s.charAt(i)) {
          case '$':
            escaped.append("\\$");
            break;
          case '"':
            escaped.append("\\\"");
            break;
          case '\'':
            escaped.append("'\\''");
            break;
          default:
            escaped.append(s.charAt(i));
            break;
        }
      }
      return escaped.append("'").toString();
    } else {
      return s;
    }
  }

}
