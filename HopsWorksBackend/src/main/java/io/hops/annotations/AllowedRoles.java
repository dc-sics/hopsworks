/**
 * 
 */
package io.hops.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author André & Ermias
 * 
 * Annotations that can be used to restrict users from accessing project methods
 * based on the role they have for that project. 
 * 
 * if no role is specified the default will be OWNER only access
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedRoles {
    public static final String ALL = "ALL";
    public static final String OWNER = "OWNER";
    public static final String CONTRIBUTOR = "CONTRIBUTOR";
    public static final String GUEST = "GUEST";
    
    /**
     *  Used to annotate methods that work with project resources 
     * @return allowed roles
     */
    public String[] roles() default {AllowedRoles.OWNER};
}
