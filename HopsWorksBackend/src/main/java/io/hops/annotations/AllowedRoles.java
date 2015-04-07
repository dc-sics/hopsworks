/*
 */
package io.hops.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author André & Ermias
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedRoles {
    public static final String ALL = "All";
    public static final String OWNER = "Owner";
    public static final String DEVELOPER = "Developer";
    public static final String GUEST = "Guest";
    
    /**
     *
     * @return
     */
    public String[] roles() default {AllowedRoles.OWNER};
}
