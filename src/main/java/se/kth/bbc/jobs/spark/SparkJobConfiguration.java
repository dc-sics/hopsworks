package se.kth.bbc.jobs.spark;

import com.google.common.base.Strings;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.hopsworks.util.Settings;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 * <p/>
 * @author stig
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration {

  private String jarPath;
  private String mainClass;
  private String args;
  private String historyServerIp;

  //Kafka properties
  private String sessionId;
  private String kStore;
  private String tStore;

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private int executorMemory = 1024;

  private boolean dynamicExecutors;
  private int minExecutors = Settings.SPARK_MIN_EXECS;
  private int maxExecutors = Settings.SPARK_MAX_EXECS;
  private int selectedMinExecutors = Settings.SPARK_INIT_EXECS;
  private int selectedMaxExecutors = Settings.SPARK_INIT_EXECS;
  private int numberOfExecutorsInit = Settings.SPARK_INIT_EXECS;

  protected static final String KEY_JARPATH = "JARPATH";
  protected static final String KEY_MAINCLASS = "MAINCLASS";
  protected static final String KEY_ARGS = "ARGS";
  protected static final String KEY_NUMEXECS = "NUMEXECS";
  //Dynamic executors properties
  protected static final String KEY_DYNEXECS = "DYNEXECS";
  protected static final String KEY_DYNEXECS_MIN = "DYNEXECSMIN";
  protected static final String KEY_DYNEXECS_MAX = "DYNEXECSMAX";
  protected static final String KEY_DYNEXECS_MIN_SELECTED
          = "DYNEXECSMINSELECTED";
  protected static final String KEY_DYNEXECS_MAX_SELECTED
          = "DYNEXECSMAXSELECTED";
  protected static final String KEY_DYNEXECS_INIT = "DYNEXECSINIT";

  protected static final String KEY_EXECCORES = "EXECCORES";
  protected static final String KEY_EXECMEM = "EXECMEM";
  protected static final String KEY_HISTORYSERVER = "HISTORYSERVER";

  public SparkJobConfiguration() {
    super();
  }

  public String getJarPath() {
    return jarPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p/>
   * @param jarPath
   */
  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }

  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the name of the main class to be executed. No default value.
   * <p/>
   * @param mainClass
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  /**
   * Set the arguments to be passed to the job. No default value.
   * <p/>
   * @param args
   */
  public void setArgs(String args) {
    this.args = args;
  }

  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  /**
   * Set the number of executors to be requested for this job. This should be
   * greater than or equal to 1.
   * <p/>
   * @param numberOfExecutors
   * @throws IllegalArgumentException If the argument is smaller than 1.
   */
  public void setNumberOfExecutors(int numberOfExecutors) throws
          IllegalArgumentException {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
              "Number of executors has to be greater than or equal to 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
  }

  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * Set the number of cores to be requested for each executor.
   * <p/>
   * @param executorCores
   * @throws IllegalArgumentException If the number of cores is smaller than 1.
   */
  public void setExecutorCores(int executorCores) throws
          IllegalArgumentException {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
              "Number of executor cores has to be greater than or equal to 1.");
    }
    this.executorCores = executorCores;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  /**
   * Set the memory requested for each executor in MB.
   * <p/>
   * @param executorMemory
   * @throws IllegalArgumentException If the given value is not strictly
   * positive.
   */
  public void setExecutorMemory(int executorMemory) throws
          IllegalArgumentException {
    if (executorMemory < 1) {
      throw new IllegalArgumentException(
              "Executor memory must be greater than 1MB.");
    }
    this.executorMemory = executorMemory;
  }

  public String getHistoryServerIp() {
    return historyServerIp;
  }

  public void setHistoryServerIp(String historyServerIp) {
    this.historyServerIp = historyServerIp;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getkStore() {
    return kStore;
  }

  public void setkStore(String kStore) {
    this.kStore = kStore;
  }

  public String gettStore() {
    return tStore;
  }

  public void settStore(String tStore) {
    this.tStore = tStore;
  }

  public boolean isDynamicExecutors() {
    return dynamicExecutors;
  }

  public void setDynamicExecutors(boolean dynamicExecutors) {
    this.dynamicExecutors = dynamicExecutors;
  }

  public int getMinExecutors() {
    return minExecutors;
  }

  public void setMinExecutors(int minExecutors) {
    this.minExecutors = minExecutors;
  }

  public int getMaxExecutors() {
    return maxExecutors;
  }

  public void setMaxExecutors(int maxExecutors) {
    this.maxExecutors = maxExecutors;
  }

  public int getSelectedMinExecutors() {
    return selectedMinExecutors;
  }

  public void setSelectedMinExecutors(int selectedMinExecutors) {
    this.selectedMinExecutors = selectedMinExecutors;
  }

  public int getSelectedMaxExecutors() {
    return selectedMaxExecutors;
  }

  public void setSelectedMaxExecutors(int selectedMaxExecutors) {
    this.selectedMaxExecutors = selectedMaxExecutors;
  }

  public int getNumberOfExecutorsInit() {
    return numberOfExecutorsInit;
  }

  public void setNumberOfExecutorsInit(int numberOfExecutorsInit) {
    this.numberOfExecutorsInit = numberOfExecutorsInit;
  }

  @Override
  public JobType getType() {
    return JobType.SPARK;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();
    //First: fields that are possibly null or empty:
    if (!Strings.isNullOrEmpty(args)) {
      obj.set(KEY_ARGS, args);
    }
    if (!Strings.isNullOrEmpty(mainClass)) {
      obj.set(KEY_MAINCLASS, mainClass);
    }
    if (!Strings.isNullOrEmpty(mainClass)) {
      obj.set(KEY_JARPATH, jarPath);
    }
    //Then: fields that can never be null or emtpy.
    obj.set(KEY_EXECCORES, "" + executorCores);
    obj.set(KEY_EXECMEM, "" + executorMemory);
    obj.set(KEY_NUMEXECS, "" + numberOfExecutors);
    obj.set(KEY_DYNEXECS, "" + dynamicExecutors);
    obj.set(KEY_DYNEXECS_MIN, "" + minExecutors);
    obj.set(KEY_DYNEXECS_MAX, "" + maxExecutors);
    obj.set(KEY_DYNEXECS_MIN_SELECTED, "" + selectedMinExecutors);
    obj.set(KEY_DYNEXECS_MAX_SELECTED, "" + selectedMaxExecutors);
    obj.set(KEY_DYNEXECS_INIT, "" + numberOfExecutorsInit);

    obj.set(KEY_TYPE, JobType.SPARK.name());
    obj.set(KEY_HISTORYSERVER, getHistoryServerIp());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    String jsonArgs, jsonJarpath, jsonMainclass, jsonNumexecs, hs, jsonExecmem,
            jsonExeccors;
    String jsonNumexecsMin = "";
    String jsonNumexecsMax = "";
    String jsonNumexecsMinSelected = "";
    String jsonNumexecsMaxSelected = "";
    String jsonNumexecsInit = "";
    String jsonDynexecs = "NOT_AVAILABLE";

    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.SPARK) {
        throw new IllegalArgumentException("JobType must be SPARK.");
      }
      //First: fields that can be null or empty
      jsonArgs = json.getString(KEY_ARGS, null);
      jsonJarpath = json.getString(KEY_JARPATH, null);
      jsonMainclass = json.getString(KEY_MAINCLASS, null);
      //Then: fields that cannot be null or emtpy.
      jsonExeccors = json.getString(KEY_EXECCORES);
      jsonExecmem = json.getString(KEY_EXECMEM);
      jsonNumexecs = json.getString(KEY_NUMEXECS);
      if (json.containsKey(KEY_DYNEXECS)) {
        jsonDynexecs = json.getString(KEY_DYNEXECS);
        jsonNumexecsMin = json.getString(KEY_DYNEXECS_MIN);
        jsonNumexecsMax = json.getString(KEY_DYNEXECS_MAX);
        jsonNumexecsMinSelected = json.getString(KEY_DYNEXECS_MIN_SELECTED);
        jsonNumexecsMaxSelected = json.getString(KEY_DYNEXECS_MAX_SELECTED);
        jsonNumexecsInit = json.getString(KEY_DYNEXECS_INIT);
      }

      hs = json.getString(KEY_HISTORYSERVER);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into SparkJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure that the type will get recognized correctly.
    json.set(KEY_TYPE, JobType.YARN.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.args = jsonArgs;
    this.executorCores = Integer.parseInt(jsonExeccors);
    this.executorMemory = Integer.parseInt(jsonExecmem);
    this.jarPath = jsonJarpath;
    this.mainClass = jsonMainclass;
    this.numberOfExecutors = Integer.parseInt(jsonNumexecs);
    if (jsonDynexecs.equals("true") || jsonDynexecs.equals("false")) {
      this.dynamicExecutors = Boolean.parseBoolean(jsonDynexecs);
      this.minExecutors = Integer.parseInt(jsonNumexecsMin);
      this.maxExecutors = Integer.parseInt(jsonNumexecsMax);
      this.selectedMinExecutors = Integer.parseInt(jsonNumexecsMinSelected);
      this.selectedMaxExecutors = Integer.parseInt(jsonNumexecsMaxSelected);
      this.numberOfExecutorsInit = Integer.parseInt(jsonNumexecsInit);
    }
    this.historyServerIp = hs;

  }

}
