package io.hops.services.rest;

import io.hops.annotations.AllowedRoles;
import io.hops.integration.ProjectFacade;
import io.hops.integration.ProjectRoleFacade;
import io.hops.integration.UserFacade;
import io.hops.model.Project;
import io.hops.model.ProjectUser;
import io.hops.model.Users;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
 *
 * @author André
 */
@Path("/project")
@RolesAllowed({"ADMIN", "USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class ProjectService {

    @EJB
    private ProjectFacade projectBean;
    @EJB
    private ProjectRoleFacade projectRoleBean;
    @EJB
    private UserFacade userBean;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.ALL})
    public Response findAllByUser(@Context SecurityContext sc, @Context HttpServletRequest req) {

        // Get the user according to current session and then get all its projects
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
        List<Project> list = projectBean.findAllByUser(user);
        GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(list) {
        };

        return getNoCacheResponseBuilder(Response.Status.OK).entity(projects).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.GUEST, AllowedRoles.CONTRIBUTOR, AllowedRoles.OWNER})
    public Response findByProjectID(
            @PathParam("id") String id,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();
        json.setStatus("OK");

        // Get a specific project based on the id, Annotated so that 
        // only the user with the allowed role is able to see it 
        return getNoCacheResponseBuilder(Response.Status.OK).entity(projectBean.findByProjectID(Integer.valueOf(id))).build();
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.CONTRIBUTOR, AllowedRoles.OWNER})
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response updateByProject(
            Project updatedProject,
            @PathParam("id") String id,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();
        
        Project proj = projectBean.findByProjectID(Integer.valueOf(id));
        projectBean.detach(proj);
        
        // Check if something changed
        if(updatedProject.getName() != null){
            proj.setName(updatedProject.getName());
        }
        if(updatedProject.getDescription() != null){
            proj.setDescription(updatedProject.getDescription());
        }
        if(updatedProject.getStatus() != null){
            proj.setStatus(updatedProject.getStatus());
        }
        if(updatedProject.getDateCreated() != null){
            proj.setDateCreated(updatedProject.getDateCreated());
        }
        if(updatedProject.getType() != null){
            proj.setType(updatedProject.getType());
        }        
        
        String status = (projectBean.updateByProject(proj) ? "OK" : "ERROR");
        
        json.setStatus(status);        
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.ALL})
    public Response createProject(
            Project project,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();
        
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        ProjectUser projUser = new ProjectUser();

        projUser.setEmail(user);
        projUser.setProjectId(project);
        projUser.setRole( projectRoleBean.getUserRoleByName(AllowedRoles.OWNER) );

        String status = (projectBean.createProject(projUser, project) ? "OK" : "ERROR");
        
        json.setStatus(status);        
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.OWNER})
    public Response removeByProjectID(
            @PathParam("id") String id,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) {

        JsonResponse json = new JsonResponse();
        
        String status = (projectBean.removeByProjectID(Integer.valueOf(id)) ? "OK" : "ERROR");
        
        json.setStatus(status);        
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    
    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
}
