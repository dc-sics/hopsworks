package io.hops.hopsworks.api.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations that can be used to restrict users from accessing project methods
 * based on the role they have for that project.
 * For this annotation to work the method annotated should be a web service with
 * a path project/{id}/*.
 * if no role is specified the default will be OWNER only access
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ProjectPermission {
  
  /**
   * Used to annotate methods that work with project resources
   * <p/>
   * @return allowed roles
   */
  ProjectPermissionLevel value() default ProjectPermissionLevel.DATA_OWNER;
}
