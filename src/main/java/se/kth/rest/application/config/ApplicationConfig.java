package se.kth.rest.application.config;

import java.util.Set;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 *
 * @author Ermias
 */
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> resources = new java.util.HashSet<>();
    resources.add(MultiPartFeature.class);
    addRestResourceClasses(resources);
    return resources;
  }

  /**
   * Do not modify addRestResourceClasses() method.
   * It is automatically populated with
   * all resources defined in the project.
   * If required, comment out calling this method in getClasses().
   */
  private void addRestResourceClasses(Set<Class<?>> resources) {
    resources.add(se.kth.hopsworks.filters.RequestAuthFilter.class);
    resources.add(se.kth.hopsworks.rest.ActivityService.class);
    resources.add(se.kth.hopsworks.rest.AppExceptionMapper.class);
    resources.add(se.kth.hopsworks.rest.AuthExceptionMapper.class);
    resources.add(se.kth.hopsworks.rest.AuthService.class);
    resources.add(se.kth.hopsworks.rest.CuneiformService.class);
    resources.add(se.kth.hopsworks.rest.DataSetService.class);
    resources.add(se.kth.hopsworks.rest.ProjectService.class);
    resources.add(se.kth.hopsworks.rest.ThrowableExceptionMapper.class);
    resources.add(se.kth.hopsworks.rest.TransactionExceptionMapper.class);
    resources.add(se.kth.hopsworks.rest.UploadService.class);
    resources.add(se.kth.hopsworks.rest.UserService.class);
  }
}
