package io.hops.hopsworks.admin.job;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import io.hops.hopsworks.common.util.Settings;

/**
 * AdminUI for administering yarn jobs.
 * <p>
 */
@ManagedBean
@ViewScoped
public class JobAdministration implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(JobAdministration.class.
          getName());
  @EJB
  private Settings settings;
  @EJB
  private YarnClientService ycs;

  private Configuration conf;
  private YarnClientWrapper yarnClientWrapper;
  private List<YarnApplicationReport> jobs = new ArrayList<>();

  private List<YarnApplicationReport> filteredJobs = new ArrayList<>();

  private Map<String, String> error = new HashMap<>();
  private boolean initial = true;

  @PostConstruct
  public void init() {
    fetchJobs(filteredJobs);
  }

  public List<YarnApplicationReport> getAllJobs() {
    if (initial) {
      jobs.addAll(filteredJobs);
      initial = false;
    } else {
      jobs.clear();
      fetchJobs(jobs);
    }

    return jobs;
  }

  public void setFilteredJobs(List<YarnApplicationReport> filteredJobs) {
    this.filteredJobs = filteredJobs;
  }

  public List<YarnApplicationReport> getFilteredJobs() {
    return filteredJobs;
  }

  public String getNumberOfJobs() {
    if (yarnClientWrapper == null) {
      conf = settings.getConfiguration();
      yarnClientWrapper = ycs.getYarnClientSuper(conf);
    }
    try {
      return String.valueOf(yarnClientWrapper.getYarnClient().getApplications()
          .size());
    } catch (YarnException | IOException ex) {
      Logger.getLogger(JobAdministration.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return "N/A";
  }

  public void killJob(final String appId) {
    error.put(appId, "Trying to kill job");
    if (yarnClientWrapper == null) {
      conf = settings.getConfiguration();
      yarnClientWrapper = ycs.getYarnClientSuper(conf);
    }
    //Find applicationId and kill it
    error.put(appId, "Application was not found");
    ApplicationId appIdToKill = null;
    try {
      for (YarnApplicationReport report : jobs) {
        if (report.getAppId().equals(appId)) {
          //Get state
          appIdToKill = ApplicationId.newInstance(report.
                  getClusterTimestamp(), report.getId());

          ApplicationReport appReport = yarnClientWrapper.getYarnClient()
              .getApplicationReport(appIdToKill);
          if (appReport.getYarnApplicationState()
                  == YarnApplicationState.FINISHED || appReport.
                  getYarnApplicationState() == YarnApplicationState.KILLED) {
            error.put(appId, "Job is already " + appReport.
                    getYarnApplicationState());
            break;
          } else {
            yarnClientWrapper.getYarnClient().killApplication(appIdToKill);
            error.put(appId, "Job killed successfully");
            break;
          }
        }
      }

      jobs.clear();
      try {
        //Create our custom YarnApplicationReport Pojo
        for (ApplicationReport appReport : yarnClientWrapper.getYarnClient()
            .getApplications()) {
          jobs.add(new YarnApplicationReport(appReport.getApplicationId().
                  toString(),
                  appReport.getName(), appReport.getUser(), appReport.
                  getStartTime(), appReport.getFinishTime(), appReport.
                  getApplicationId().getClusterTimestamp(),
                  appReport.getApplicationId().getId(), appReport.
                  getYarnApplicationState().name()));
        }
      } catch (YarnException | IOException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
      //Update filtered jobs
      if (filteredJobs != null && appIdToKill != null) {
        ListIterator<YarnApplicationReport> iter = filteredJobs.listIterator();
        while (iter.hasNext()) {
          YarnApplicationReport next = iter.next();
          if (next.getAppId().equals(appId)) {
            //Updated AppReport
            ApplicationReport appReport = yarnClientWrapper.getYarnClient()
                .getApplicationReport(appIdToKill);

            iter.set(new YarnApplicationReport(appReport.getApplicationId().
                    toString(),
                    appReport.getName(), appReport.getUser(), appReport.
                    getStartTime(), appReport.getFinishTime(), appReport.
                    getApplicationId().getClusterTimestamp(),
                    appReport.getApplicationId().getId(), appReport.
                    getYarnApplicationState().name()));
            break;
          }
        }
      }
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, "Error while trying to kill job with appId:"
              + appId, ex.getMessage());
    }

  }

  private void fetchJobs(List<YarnApplicationReport> reports) {
    if (yarnClientWrapper == null) {
      conf = settings.getConfiguration();
      yarnClientWrapper = ycs.getYarnClientSuper(conf);
    }
    try {
      //Create our custom YarnApplicationReport Pojo
      for (ApplicationReport appReport : yarnClientWrapper.getYarnClient()
          .getApplications()) {
        reports.add(new YarnApplicationReport(appReport.getApplicationId().
                toString(),
                appReport.getName(), appReport.getUser(), appReport.
                getStartTime(), appReport.getFinishTime(), appReport.
                getApplicationId().getClusterTimestamp(),
                appReport.getApplicationId().getId(), appReport.
                getYarnApplicationState().name()));
      }
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  @PreDestroy
  public void preDestroy() {
    if (yarnClientWrapper != null) {
      ycs.closeYarnClient(yarnClientWrapper);
    }
  }

  public String getError(String appId) {
    if (error.containsKey(appId)) {
      return error.get(appId);
    }
    return null;
  }

  public void setError(Map<String, String> error) {
    this.error = error;
  }

  public class YarnApplicationReport {

    private String appId;
    private String name;
    private String user;
    private Date startTime;
    private Date finishTime;
    private long clusterTimestamp;
    private int id;
    private String state;

    public YarnApplicationReport(String appId, String name, String user,
            long startTime, long finishTime, long clusterTimestamp, int id,
            String state) {
      this.appId = appId;
      this.name = name;
      this.user = user;
      this.startTime = new Date(startTime);
      this.finishTime = finishTime == 0 ? null : new Date(finishTime);
      this.clusterTimestamp = clusterTimestamp;
      this.id = id;
      this.state = state;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public Date getStartTime() {
      return startTime;
    }

    public void setStartTime(long startTime) {
      this.startTime = new Date(startTime);
    }

    public Date getFinishTime() {
      return finishTime;
    }

    public void setFinishTime(long finishTime) {
      if (finishTime == 0) {
        this.finishTime = null;
      } else {
        this.finishTime = new Date(finishTime);
      }
    }

    public long getClusterTimestamp() {
      return clusterTimestamp;
    }

    public void setClusterTimestamp(long clusterTimestamp) {
      this.clusterTimestamp = clusterTimestamp;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final YarnApplicationReport other = (YarnApplicationReport) obj;
      if (!Objects.equals(this.appId, other.appId)) {
        return false;
      }
      return true;
    }

  }

}
