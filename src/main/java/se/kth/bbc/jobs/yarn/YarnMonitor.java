package se.kth.bbc.jobs.yarn;

import java.io.Closeable;
import java.io.IOException;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;

/**
 *
 * @author stig
 */
public final class YarnMonitor implements Closeable {

  private final YarnClient yarnClient;
  private final ApplicationId appId;

  public YarnMonitor(ApplicationId id, YarnClient yarnClient) {
    if (id == null) {
      throw new IllegalArgumentException(
              "ApplicationId cannot be null for Yarn monitor!");
    }
    this.appId = id;
    this.yarnClient = yarnClient;
  }

  public YarnMonitor start() {
    yarnClient.start();
    return this;
  }

  public void stop() {
    yarnClient.stop();
  }

  public boolean isStarted() {
    return yarnClient.isInState(Service.STATE.STARTED);
  }

  public boolean isStopped() {
    return yarnClient.isInState(Service.STATE.STOPPED);
  }

  //---------------------------------------------------------------------------        
  //--------------------------- STATUS QUERIES --------------------------------
  //---------------------------------------------------------------------------
  public YarnApplicationState getApplicationState() throws YarnException,
          IOException {
    return yarnClient.getApplicationReport(appId).getYarnApplicationState();
  }

  public FinalApplicationStatus getFinalApplicationStatus() throws YarnException,
      IOException {
    return yarnClient.getApplicationReport(appId).getFinalApplicationStatus();
  }

  public float getProgress() throws YarnException,
      IOException {
    return yarnClient.getApplicationReport(appId).getProgress();
  }

  public ApplicationId getApplicationId() {
    return appId;
  }

  //---------------------------------------------------------------------------        
  //------------------------- YARNCLIENT UTILS --------------------------------
  //---------------------------------------------------------------------------
  @Override
  public void close() {
    stop();
  }

  public void cancelJob(String appid) throws YarnException, IOException {
    ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
    yarnClient.killApplication(applicationId);
  }
}
