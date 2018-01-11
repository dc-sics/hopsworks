package io.hops.hopsworks.common.yarn;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YarnClientWrapper {
  private final Logger LOG = Logger.getLogger(YarnClientWrapper.class.getName());
  private final String projectName;
  private final String username;
  private final Configuration conf;
  private YarnClient yarnClient;
  
  public YarnClientWrapper(String projectName, String username,
      Configuration conf) {
    this.projectName = projectName;
    this.username = username;
    this.conf = conf;
  }
  
  public YarnClientWrapper get() {
    if (yarnClient == null) {
      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(conf);
      yarnClient.start();
    }
    
    return this;
  }
  
  public YarnClient getYarnClient() {
    if (yarnClient == null) {
      throw new RuntimeException("YarnClient has not been initialized");
    }
    
    return yarnClient;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void close() {
    if (null != yarnClient) {
      try {
        yarnClient.close();
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Error while closing YarnClient", ex);
      }
    }
  }
}
