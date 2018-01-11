package io.hops.hopsworks.common.jobs.flink;

import org.apache.flink.yarn.YarnApplicationMasterRunner;

/**
 * Default implementation of {@link AbstractYarnClusterDescriptor} which starts
 * an {@link YarnApplicationMasterRunner}.
 */
public class YarnClusterDescriptor extends AbstractYarnClusterDescriptor {

  @Override
  protected Class<?> getApplicationMasterClass() {
    return YarnApplicationMasterRunner.class;
  }
}
