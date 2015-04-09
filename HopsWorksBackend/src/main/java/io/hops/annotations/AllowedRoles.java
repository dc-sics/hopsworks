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
    public static final String ALL = "ALL";
    public static final String OWNER = "OWNER";
    public static final String CONTRIBUTOR = "CONTRIBUTOR";
    public static final String GUEST = "GUEST";
    
    /**
     *
     * @return
     */
    public String[] roles() default {AllowedRoles.OWNER};
}
