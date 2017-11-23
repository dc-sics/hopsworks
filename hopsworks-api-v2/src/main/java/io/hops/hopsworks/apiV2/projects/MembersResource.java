package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.MembersDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Api("Members")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MembersResource {
  
  @EJB
  ProjectTeamFacade projectTeamFacade;
  @EJB
  ProjectFacade projectFacade;
  @EJB
  UserFacade userFacade;
  
  private Integer projectId;
  
  private final static Logger logger = Logger.getLogger(
      MembersResource.class.getName());
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  @ApiOperation("Get a list of project members")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAll(@Context SecurityContext sc){
    List<ProjectTeam> membersByProject = projectTeamFacade.findMembersByProject(projectFacade.find(projectId));
    List<MemberDTO> result = new ArrayList<>();
    for (ProjectTeam projectTeam : membersByProject) {
      result.add(new MemberDTO(projectTeam));
    }
    GenericEntity<List<MemberDTO>> entity = new GenericEntity<List<MemberDTO>>(result){};
    return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation("Remove member from project")
  @DELETE
  @Path("/{userId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteMember(@PathParam("id") Integer userId, @Context SecurityContext sc, @Context
      HttpServletRequest req) throws Exception {
    Users user = userFacade.find(userId);
    
    //projectMembersService.removeMembersByID(user.getEmail(),sc, req);
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Endpoint not implemented");
  
    //return Response.noContent().build();
  }
  
  @ApiOperation("Get member information")
  @GET
  @Path("/{userId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getMember(@PathParam("id") Integer userId, @Context SecurityContext sc) throws AppException {
    for (ProjectTeam member : projectTeamFacade.findMembersByProject(projectFacade.find(projectId))){
      if (userId.equals(member.getUser().getUid())){
        GenericEntity<MemberDTO> result = new GenericEntity<MemberDTO>(new MemberDTO(member)){};
        Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
      }
    }
    throw new AppException(Response.Status.NOT_FOUND, "No such member");
  }
  
  @ApiOperation("Add member to project")
  @PUT
  @Path("/{userId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response addMember(@PathParam("id") Integer userId, @Context SecurityContext sc, @Context HttpServletRequest
      req) throws AppException {
    MembersDTO toAdd = new MembersDTO();
    ProjectTeam teamOfOne = new ProjectTeam();
    teamOfOne.setUser(userFacade.find(userId));
    toAdd.setProjectTeam(Collections.singletonList(teamOfOne));
    //projectMembersService.addMembers(toAdd, sc, req);
    throw new AppException(Response.Status.NOT_IMPLEMENTED, "Endpoint not implemented");
    //return Response.noContent().build();
  }
  
  
  
}
