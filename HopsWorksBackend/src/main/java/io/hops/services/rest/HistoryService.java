/*
 */
package io.hops.services.rest;

import io.hops.integration.ProjectFacade;
import io.hops.integration.ProjectHistoryFacade;
import io.hops.integration.ProjectUserFacade;
import io.hops.integration.UserFacade;
import io.hops.model.Project;
import io.hops.model.ProjectHistory;
import io.hops.model.Users;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/history")
@RolesAllowed({"ADMIN", "USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class HistoryService {
    @EJB
    private ProjectHistoryFacade historyBean;
    
    @EJB
    private UserFacade userBean;
    
    @EJB
    private ProjectFacade projectBean;
    
    @EJB
    private ProjectUserFacade projectUserBean;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAllByUser(@Context SecurityContext sc, @Context HttpServletRequest req) {
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        List<ProjectHistory> historyList = historyBean.findAllByUser(user);
        GenericEntity<List<ProjectHistory>> projectHistories = new GenericEntity<List<ProjectHistory>>(historyList) {
        };
        return getNoCacheResponseBuilder(Response.Status.OK).entity(projectHistories).build();
    }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAllByProject(@PathParam("id") Integer id, @Context SecurityContext sc, @Context HttpServletRequest req) {
        Project project = projectBean.findByProjectID(id); 
        /*check if the user have role in the project.
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        if(projectUserBean.findRoleByID(user, project) == null){
                    
        }*/
               
        List<ProjectHistory> historyList = historyBean.findAllByProject(project); 
        GenericEntity<List<ProjectHistory>> projectHistories = new GenericEntity<List<ProjectHistory>>(historyList) {
        };
        return getNoCacheResponseBuilder(Response.Status.OK).entity(projectHistories).build();
    }
    
    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
}
