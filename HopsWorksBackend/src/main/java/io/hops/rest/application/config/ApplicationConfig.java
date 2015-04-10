package io.hops.rest.application.config;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author André & Ermias
 */
@javax.ws.rs.ApplicationPath("api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
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
        resources.add(io.hops.filters.RequestAuthFilter.class);
        resources.add(io.hops.integration.AppExceptionMapper.class);
        resources.add(io.hops.integration.AuthExceptionMapper.class);
        resources.add(io.hops.integration.ThrowableExceptionMapper.class);
        resources.add(io.hops.integration.TransactionExceptionMapper.class);
        resources.add(io.hops.services.rest.AuthService.class);
        resources.add(io.hops.services.rest.ProjectService.class);
        resources.add(io.hops.services.rest.UserService.class);
    }
    
}
