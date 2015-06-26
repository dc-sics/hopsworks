package se.kth.bbc.jobs.yarn;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains user-setable configuration parameters for a Yarn job.
 * <p>
 * @author stig
 */
@XmlRootElement
public class YarnJobConfiguration {

  private String amQueue = "default";
  // Memory for App master (in MB)
  private int amMemory = 1024;
  //Number of cores for appMaster
  private int amVCores = 1;
  // Application name
  private String appName = "";

  public final String getAmQueue() {
    return amQueue;
  }

  /**
   * Set the queue to which the application should be submitted to the
   * ResourceManager. Default value: "".
   * <p>
   * @param amQueue
   */
  public final void setAmQueue(String amQueue) {
    this.amQueue = amQueue;
  }

  public final int getAmMemory() {
    return amMemory;
  }

  /**
   * Set the amount of memory in MB to be allocated for the Application Master
   * container. Default value: 1024.
   * <p>
   * @param amMemory
   */
  public final void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  public final int getAmVCores() {
    return amVCores;
  }

  /**
   * Set the number of virtual cores to be allocated for the Application Master
   * container. Default value: 1.
   * <p>
   * @param amVCores
   */
  public final void setAmVCores(int amVCores) {
    this.amVCores = amVCores;
  }

  public final String getAppName() {
    return appName;
  }

  /**
   * Set the name of the application. Default value: "Hops job".
   * <p>
   * @param appName
   */
  public final void setAppName(String appName) {
    this.appName = appName;
  }

}
