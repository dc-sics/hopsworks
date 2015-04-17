package io.hops.filters;

import io.hops.annotations.AllowedRoles;
import io.hops.integration.ProjectUserFacade;
import io.hops.services.rest.JsonResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * @author André & Ermias
 * 
 * Request filter that can be used to restrict users accesses to projects based 
 * on the role they have for the project and the annotation on the method being called.
 */
@Provider
public class RequestAuthFilter implements ContainerRequestFilter {

    @EJB
    private ProjectUserFacade projectUserBean;

    @Context
    private ResourceInfo resourceInfo;

    private final static Logger log = Logger.getLogger(RequestAuthFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) {

        String path = requestContext.getUriInfo().getPath();

        Method method = resourceInfo.getResourceMethod();
        log.log(Level.INFO, "Filtering request path: {0}", path);

        // Project service
        if (path.contains("project")) {

            JsonResponse json = new JsonResponse();

            log.log(Level.INFO, "Filtering project request path: {0}", path);

            AllowedRoles rolesAnnotation = method.getAnnotation(AllowedRoles.class);
            Set<String> rolesSet;
            rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.roles()));

            if (!method.isAnnotationPresent(AllowedRoles.class)) {
                //Should throw exception if there is a method that is not annotated in this path.
                requestContext.abortWith(Response.status(Response.Status.NOT_IMPLEMENTED).build());
                return;
            }

            //If the resource is allowed for all roles continue with the request. 
            if (rolesSet.contains(AllowedRoles.ALL)) {
                log.log(Level.INFO, "Accessing resource that is allowed for all");
                return;
            }

            //if the resource is only allowed for some roles check if the user have the requierd role for the resource.
            String userEmail = requestContext.getSecurityContext().getUserPrincipal().getName();
            String projectId = path.substring(path.indexOf("/") + 1, path.length());
            
            String userRole;
            try {
                userRole = projectUserBean.findRoleByID(userEmail, projectId);
            } catch (Exception e) {
                userRole = "";
            }

            if (userRole == null || userRole.isEmpty()) {
                log.log(Level.INFO, "Trying to access resource, but you dont have any role in this project");
                json.setStatusCode(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                json.setErrorMsg("You do not have access to this project.");
                requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
            } else if (!rolesSet.contains(userRole)) {
                log.log(Level.INFO, "Trying to access resource that is only allowed for: {0}, But you have: {1}", 
                        new Object[]{rolesSet, userRole});
                json.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
                json.setErrorMsg("You do not have the required role to access this resource.");
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            }
        }
    }
}
