package io.hops.hopsworks.common.jobs.jobhistory;

import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;

public enum JobFinalStatus {

  UNDEFINED("Undefined"),
  SUCCEEDED("Succeeded"),
  FAILED("Failed"),
  KILLED("Killed");

  private final String readable;

  private JobFinalStatus(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

  public static JobFinalStatus getJobFinalStatus(
          FinalApplicationStatus yarnFinalStatus) {
    switch (yarnFinalStatus) {
      case UNDEFINED:
        return JobFinalStatus.UNDEFINED;
      case SUCCEEDED:
        return JobFinalStatus.SUCCEEDED;
      case FAILED:
        return JobFinalStatus.FAILED;
      case KILLED:
        return JobFinalStatus.KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant"); // can never happen
    }
  }

}
