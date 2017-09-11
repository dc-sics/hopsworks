package io.hops.hopsworks.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

/*
 * Client Code:
 *
 * Settings settings = ...
 * StringBuilder zeppelin_env = ConfigFileGenerator.instantiateFromTemplate(
 * ConfigFileGenerator.ZEPPELIN_ENV_TEMPLATE,
 * "zeppelin_dir", settings.getZeppelinDir() + projectName,
 * "spark_dir", settings.getSparkDir(),
 * "hadoop_dir", settings.getHadoopDir()
 * );
 *
 * ConfigFileGenerator.createConfigFile(settings.getZeppelinDir() + projectName
 * + "/zeppelin_env.xml", zeppelin_env.toString());
 *
 */
public class ConfigFileGenerator {

  public static final String TEMPLATE_ROOT = File.separator + "io"
          + File.separator + "hops";
  public static final String LOG4J_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "log4j_template.properties";
  public static final String ZEPPELIN_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "zeppelin_site_template.xml";
  public static final String ZEPPELIN_ENV_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "zeppelin_env_template.sh";
  public static final String INTERPRETER_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "zeppelin" + File.separator
          + "interpreter_template.json";
  public static final String JUPYTER_NOTEBOOK_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "jupyter_notebook_config_template.py";
  public static final String JUPYTER_CUSTOM_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "custom_template.js";
  public static final String SPARKMAGIC_CONFIG_TEMPLATE
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "config_template.json";  
  public static final String LOG4J_TEMPLATE_JUPYTER
          = TEMPLATE_ROOT + File.separator + "jupyter" + File.separator
          + "log4j_template.properties";  
  public static final String METRICS_TEMPLATE
          = TEMPLATE_ROOT + File.separator
          + "metrics_template.properties";

  /**
   * @param filePath
   * @param pairs
   * @return
   * @throws IOException
   */
  public static StringBuilder instantiateFromTemplate(String filePath,
          String... pairs) throws IOException {
    if (pairs.length % 2 != 0) {
      throw new IOException(
              "Odd number of parameters when instantiating a template. Are you missing a parameter?");
    }
    StringBuilder sb = new StringBuilder();
    String script = IoUtils.readContentFromClasspath(filePath);
    if (pairs.length > 0) {
      for (int i = 0; i < pairs.length; i += 2) {
        String key = pairs[i];
        String val = pairs[i + 1];
        script = script.replaceAll("%%" + key + "%%", val);
      }
    }
    return sb.append(script);
  }

  /**
   *
   * @param filePath
   * @param params
   * @return
   * @throws IOException
   */
  public static StringBuilder instantiateFromTemplate(String filePath,
          Map<String, String> params) throws IOException {
    StringBuilder sb = new StringBuilder();
    String script = IoUtils.readContentFromClasspath(filePath);
    if (params.size() > 0) {
      for (Entry<String, String> env : params.entrySet()) {
        if (env.getValue() != null) {
          script = script.replaceAll("%%" + env.getKey() + "%%", env.getValue());
        }
      }
    }
    return sb.append(script);
  }

  public static boolean mkdirs(String path) {
    File cbDir = new File(path);
    return cbDir.mkdirs();
  }
  
  public static String getZeppelinDefaultInterpreterJson() {
    String json;
    try {
      json = IoUtils.readContentFromClasspath(INTERPRETER_TEMPLATE);
    } catch (IOException ex) {
      return null;
    }
    return json;
  }

  public static boolean deleteRecursive(File path) throws FileNotFoundException {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath());
    }
    boolean ret = true;
    if (path.isDirectory()) {
      for (File f : path.listFiles()) {
        ret = ret && deleteRecursive(f);
      }
    }
    return ret && path.delete();
  }

  public static boolean createConfigFile(File path, String contents) throws
          IOException {
    // write contents to file as text, not binary data
    if (!path.exists()) {
      if (!path.createNewFile()) {
        throw new IOException("Problem creating file: " + path);
      }
    }
    PrintWriter out = new PrintWriter(path);
    out.println(contents);
    out.flush();
    out.close();
    return true;
  }

}
