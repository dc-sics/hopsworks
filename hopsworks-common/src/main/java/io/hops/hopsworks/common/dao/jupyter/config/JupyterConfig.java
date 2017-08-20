package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.ConfigFileGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

public class JupyterConfig {

  private static final Logger LOGGGER = Logger.getLogger(JupyterConfig.class.
          getName());
  private static final String LOG4J_PROPS = "/log4j.properties";
  private static final String JUPYTER_NOTEBOOK_CONFIG
          = "/jupyter_notebook_config.py";
  private static final String JUPYTER_CUSTOM_JS = "/custom/custom.js";
  private static final String SPARKMAGIC_CONFIG = "/config.json";
  private static final int DELETE_RETRY = 10;

  public static JupyterConfig COMMON_CONF;

  /**
   * A configuration that is common for all projects.
   */
  private final Settings settings;
  private final String projectName;
  private final String hdfsUser;
  private final String projectPath;
  private final String projectUserDirPath;
  private final String confDirPath;
  private final String runDirPath;
  private final String logDirPath;
  private final int port;
  private long pid;
  private String secret;
  private String token;
  private String nameNodeEndpoint;

  JupyterConfig(String projectName, String secretConfig, String hdfsUser,
          String nameNodeEndpoint, Settings settings, int port, String token,
          JupyterSettings js)
          throws AppException {
    this.projectName = projectName;
    this.hdfsUser = hdfsUser;
    this.nameNodeEndpoint = nameNodeEndpoint;
    boolean newDir = false;
    boolean newFile = false;
    this.settings = settings;
    this.port = port;
    projectPath = settings.getJupyterDir() + File.separator
            + Settings.DIR_ROOT + File.separator + this.projectName
            + File.separator + hdfsUser;
    projectUserDirPath = projectPath + File.separator + secretConfig;
    confDirPath = projectUserDirPath + File.separator + "conf";
    logDirPath = projectUserDirPath + File.separator + "logs";
    runDirPath = projectUserDirPath + File.separator + "r";
    this.token = token;
    try {
      newDir = createJupyterDirs();
      createConfigFiles(nameNodeEndpoint, port, js);
    } catch (Exception e) {
      if (newDir) { // if the folder was newly created delete it
        removeProjectDirRecursive();
      } else if (newFile) { // if the conf files were newly created delete them
        removeProjectConfFiles();
      }
      LOGGGER.log(Level.SEVERE,
              "Error in initializing JupyterConfig for project: {0}. {1}",
              new Object[]{this.projectName, e});
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not configure Jupyter. Report a bug.");

    }
  }

  public String getProjectPath() {
    return projectPath;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getHdfsUser() {
    return hdfsUser;
  }

  public Settings getSettings() {
    return settings;
  }

  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }

  public int getPort() {
    return port;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  /**
   * No synchronization here, as one slow notebook server could kill all clients
   *
   * @param hdfsUsername
   * @param port
   * @return
   * @throws IOException
   */
  public StringBuffer getConsoleOutput(String hdfsUsername, int port) throws
          IOException {

    // Read the whole log file and pass it as a StringBuffer. 
    String fname = this.getLogDirPath() + "/" + hdfsUsername + "-" + port
            + ".log";
    BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(fname), Charset.forName("UTF8")));
    String line;
    StringBuffer sb = new StringBuffer();
    while (((line = br.readLine()) != null)) {
      sb.append(line);
    }
    return sb;

  }

  public void clean() {
    cleanAndRemoveConfDirs();
  }

  public String getProjectName() {
    return projectName;
  }

  public String getProjectDirPath() {
    return projectUserDirPath;
  }

  public String getConfDirPath() {
    return confDirPath;
  }

  public String getRunDirPath() {
    return runDirPath;
  }

  public String getLogDirPath() {
    return logDirPath;
  }

  //returns true if the project dir was created 
  private boolean createJupyterDirs() throws IOException {
    File projectDir = new File(projectPath);
    projectDir.mkdirs();
    File baseDir = new File(projectUserDirPath);
    baseDir.mkdirs();
    // Set owner persmissions
    Set<PosixFilePermission> perms = new HashSet<>();
    //add owners permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    //add group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_WRITE);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    //add others permissions
    perms.add(PosixFilePermission.OTHERS_READ);
//    perms.add(PosixFilePermission.OTHERS_WRITE);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);

    Files.setPosixFilePermissions(Paths.get(projectUserDirPath), perms);
    Files.setPosixFilePermissions(Paths.get(projectPath), perms);

    new File(confDirPath + "/custom").mkdirs();
    new File(runDirPath).mkdirs();
    new File(logDirPath).mkdirs();
    return true;
  }

  // returns true if one of the conf files were created anew 
  private boolean createConfigFiles(String nameNodeEndpoint, Integer port,
          JupyterSettings js)
          throws
          IOException {
    File jupyter_config_file = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    File sparkmagic_config_file = new File(confDirPath + SPARKMAGIC_CONFIG);
    File custom_js = new File(confDirPath + JUPYTER_CUSTOM_JS);
    boolean createdJupyter = false;
    boolean createdSparkmagic = false;
    boolean createdCustomJs = false;

    if (!jupyter_config_file.exists()) {

      String ldLibraryPath = "";
      if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
      }
      String[] nn = nameNodeEndpoint.split(":");
      String nameNodeIp = nn[0];
      String nameNodePort = nn[1];

      StringBuilder jupyter_notebook_config = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.JUPYTER_NOTEBOOK_CONFIG_TEMPLATE,
                      "project", this.projectName,
                      "namenode_ip", nameNodeIp,
                      "namenode_port", nameNodePort,
                      "hopsworks_ip", settings.getHopsworksIp(),
                      "hdfs_user", this.hdfsUser,
                      "port", port.toString(),
                      "hadoop_home", this.settings.getHadoopDir(),
                      "hdfs_home", this.settings.getHadoopDir(),
                      "secret_dir", this.settings.getStagingDir()
                      + "/private_dirs/" + js.getSecret()
              );
      createdJupyter = ConfigFileGenerator.createConfigFile(jupyter_config_file,
              jupyter_notebook_config.toString());
    }
    if (!sparkmagic_config_file.exists()) {

      StringBuilder sparkmagic_sb
              = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.SPARKMAGIC_CONFIG_TEMPLATE,
                      "livy_ip", settings.getLivyIp(),
                      "hdfs_user", this.hdfsUser,
                      "driver_cores", Integer.toString(js.getAppmasterCores()),
                      "driver_memory", Integer.toString(js.getAppmasterMemory()),
                      "num_executors", Integer.toString(js.getNumExecutors()),
                      "executor_cores", Integer.toString(js.
                              getNumExecutorCores()),
                      "executor_memory", Integer.
                      toString(js.getExecutorMemory()),
                      "dynamic_executors", Boolean.toString(
                              js.getMode().compareToIgnoreCase("sparkDynamic")
                              == 0),
                      "min_executors", Integer.toString(js.
                              getDynamicMinExecutors()),
                      "initial_executors", Integer.toString(js.
                              getDynamicInitialExecutors()),
                      "max_executors", Integer.toString(js.
                              getDynamicMaxExecutors()),
                      "archives", js.getArchives(),
                      "jars", js.getJars(),
                      "files", js.getFiles(),
                      "pyFiles", js.getPyFiles(),
                      "num_ps", Integer.toString(js.getNumTfPs()),
                      "num_gpus", Integer.toString(js.getNumTfGpus()),
                      "tensorflow", Boolean.toString(js.getMode().
                              compareToIgnoreCase("tensorflow") == 0),
                      "jupyter_home", this.confDirPath,
                      "project", this.projectName,
                      "nn_endpoint", this.nameNodeEndpoint,
                      "spark_user", this.settings.getSparkUser(),
                      "java_home", this.settings.getJavaHome(),
                      "hadoop_home", this.settings.getHadoopDir(),
                      "pyspark_bin", this.settings.getAnacondaProjectDir(
                              projectName) + "/bin/python",
                      "anaconda_dir", this.settings.getAnacondaDir(),
                      "cuda_dir", this.settings.getCudaDir(),
                      "anaconda_env", this.settings.getAnacondaProjectDir(
                              projectName),
                      "sparkhistoryserver_ip", this.settings.
                      getSparkHistoryServerIp()
              );
      createdSparkmagic = ConfigFileGenerator.createConfigFile(
              sparkmagic_config_file,
              sparkmagic_sb.toString());
    }
    if (!custom_js.exists()) {

      StringBuilder custom_js_sb = ConfigFileGenerator.
              instantiateFromTemplate(
                      ConfigFileGenerator.JUPYTER_CUSTOM_TEMPLATE,
                      "hadoop_home", this.settings.getHadoopDir()
              );
      createdCustomJs = ConfigFileGenerator.createConfigFile(
              custom_js, custom_js_sb.toString());
    }

    return createdJupyter || createdSparkmagic || createdCustomJs;
  }

  /**
   * Closes all resources and deletes project dir
   * /srv/zeppelin/Projects/this.projectName recursive.
   *
   * @return true if the dir is deleted
   */
  public boolean cleanAndRemoveConfDirs() {
    return removeProjectDirRecursive();
  }

  private boolean removeProjectDirRecursive() {
    File projectDir = new File(projectPath);
    if (!projectDir.exists()) {
      return true;
    }
    boolean ret = false;
    try {
      ret = ConfigFileGenerator.deleteRecursive(projectDir);
    } catch (FileNotFoundException ex) {
      // do nothing
    }
    return ret;
  }

  private boolean removeProjectConfFiles() {
//    File jupyter_js_file = new File(confDirPath + JUPYTER_CUSTOM_JS);
    File jupyter_config_file
            = new File(confDirPath + JUPYTER_NOTEBOOK_CONFIG);
    boolean ret = false;
//    if (jupyter_js_file.exists()) {
//      ret = jupyter_js_file.delete();
//    }
    if (jupyter_config_file.exists()) {
      ret = jupyter_config_file.delete();
    }
    return ret;
  }

  public String getProjectUserDirPath() {
    return projectUserDirPath;
  }


}
