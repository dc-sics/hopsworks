package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;

public enum TourProjectType {
  SPARK("spark", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS}),
  KAFKA("kafka", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS, ProjectServiceEnum.KAFKA}),
  DISTRIBUTED_TENSORFLOW("distributed tensorflow", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS}),
  TENSORFLOW("tensorflow", new ProjectServiceEnum[]{ProjectServiceEnum.JOBS, ProjectServiceEnum.JUPYTER,
    ProjectServiceEnum.ZEPPELIN});

  private final String tourName;
  private final ProjectServiceEnum[] activeServices;

  TourProjectType(String tourName, ProjectServiceEnum[] activeServices) {
    this.tourName = tourName;
    this.activeServices = activeServices;
  }

  public String getTourName() {
    return tourName;
  }

  public ProjectServiceEnum[] getActiveServices() {
    return activeServices;
  }
}
