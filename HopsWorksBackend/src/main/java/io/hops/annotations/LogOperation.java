/*
 */
package io.hops.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 * 
 * This annotation can be used on project related methods that needs to be logged 
 * in the project history table.
 * The annotation type(operation performed on the project) and the description of the 
 * operation performed will be saved together with the project id and the email of the 
 * person that performed it.
 * For this annotation to work the method annotated should be a web service with 
 * a path project/{id}/*.
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface LogOperation {
    public static final String ACCESS = "ACCESS";
    public static final String UPDATE = "UPDATE";
    public static final String CREATE = "CREATE";

    public @Nonbinding String type() default "";
    public @Nonbinding String description() default "";
    
}
