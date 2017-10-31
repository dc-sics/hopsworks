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
public @interface AllowedProjectRoles {

  /**
   * Allowed for everyone. This does not mean both roles it means literally
   * everyone
   */
  String ANYONE = "ALL";
  /**
   * Allowed only to the owner
   */
  String DATA_OWNER = "Data owner";
  /**
   * Allowed to contributors or members of the project. There is no hierarchy if
   * only this annotation is used only members will be granted access. So to
   * allow
   * owners and members use both.
   */
  String DATA_SCIENTIST = "Data scientist";

  /**
   * Used to annotate methods that work with project resources
   * <p/>
   * @return allowed roles
   */
  String[] value() default {AllowedProjectRoles.DATA_OWNER};
}
