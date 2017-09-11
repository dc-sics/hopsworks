package io.hops.hopsworks.common.dao.project.service;

public enum ProjectServiceEnum {

  ZEPPELIN("Zeppelin"),
  SSH("Ssh"),
  KAFKA("Kafka"),
  WORKFLOWS("Workflows"),
  HISTORY("History"),
  JUPYTER("Jupyter"),
  JOBS("Jobs");

  private final String readable;

  private ProjectServiceEnum(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

}
