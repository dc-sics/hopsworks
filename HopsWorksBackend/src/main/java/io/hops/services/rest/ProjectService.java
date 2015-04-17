package io.hops.services.rest;

import io.hops.annotations.AllowedRoles;
import io.hops.annotations.LogOperation;
import io.hops.integration.ProjectFacade;
import io.hops.integration.ProjectRoleFacade;
import io.hops.integration.ProjectTypeFacade;
import io.hops.integration.UserFacade;
import io.hops.model.Project;
import io.hops.model.ProjectType;
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
 * @author André & Ermias
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
    @EJB
    private ProjectTypeFacade projectTypeBean;

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
    @LogOperation(type=LogOperation.ACCESS, description = "Access project")//this is just a test!!!!!!!
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
        if (updatedProject.getName() != null) {
            proj.setName(updatedProject.getName());
        }
        if (updatedProject.getDescription() != null) {
            proj.setDescription(updatedProject.getDescription());
        }
        if (updatedProject.getStatus() != null) {
            proj.setStatus(updatedProject.getStatus());
        }
        if (updatedProject.getDateCreated() != null) {
            proj.setDateCreated(updatedProject.getDateCreated());
        }

        projectBean.updateByProject(proj);

        json.setStatus("OK");
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
        
        Users owner = userBean.findByEmail(sc.getUserPrincipal().getName());
        
        // Add owner and create project
        ProjectUser projUser = new ProjectUser();
        projUser.setEmail(owner);
        projUser.setProjectId(project);
        projUser.setRole(projectRoleBean.getUserRoleByName(AllowedRoles.OWNER));
        
        projectBean.createProject(projUser, project);

        // Add types
        for(String type : project.getTypes()){
            ProjectType projType = new ProjectType();
            projType.setProjectID(project);
            projType.setType(type);
            
            projectTypeBean.persist(projType);
        }
        
        // Add members
        for (String member : project.getMembers()) {
            Users foundMember = userBean.findByEmail(member);

            if (!foundMember.getEmail().equals(owner.getEmail())) {
                ProjectUser projMember = new ProjectUser();
                projMember.setEmail(foundMember);
                projMember.setProjectId(project);
                projMember.setRole(projectRoleBean.getUserRoleByName(AllowedRoles.GUEST));

                projectBean.createProject(projMember, project);
            }
        }

        json.setStatus("201");// Created       
        return getNoCacheResponseBuilder(Response.Status.CREATED).entity(json).build();
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

        projectBean.removeByProjectID(Integer.valueOf(id));

        json.setStatus("OK");
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
