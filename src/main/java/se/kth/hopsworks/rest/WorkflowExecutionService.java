package se.kth.hopsworks.rest;

import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.WorkflowJob;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.workflows.*;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;


@RequestScoped
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class WorkflowExecutionService {


    @EJB
    private UserFacade userBean;

    @EJB
    private WorkflowExecutionFacade workflowExecutionFacade;

    @EJB
    private NoCacheResponse noCacheResponse;

    @EJB
    private DistributedFsService dfs;

    @EJB
    private OozieFacade oozieFacade;

    @EJB
    private NodeFacade nodeFacade;

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    private Workflow workflow;

    public WorkflowExecutionService(){

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response index() throws AppException {
        Collection<WorkflowExecution> executions = workflow.getWorkflowExecutions();
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(executions).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response show(
            @PathParam("id") Integer id) throws AppException {
        WorkflowExecution execution = workflowExecutionFacade.find(id);
        if (execution == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        try{
            WorkflowJob job = oozieFacade.getJob(execution.getJobId());
        }catch(OozieClientException e){
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
        }

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(execution).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response create(
            @Context HttpServletRequest req) throws AppException {
        Users user = userBean.findByEmail(req.getRemoteUser());
        String path = "/Workflows/" + user.getUsername() + "/" + workflow.getName() + "/" + workflow.getUpdatedAt().getTime() + "/";
        WorkflowExecution workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowId(workflow.getId());
        workflowExecution.setWorkflow(workflow);
        workflowExecution.setUser(user);
        workflowExecution.setUserId(user.getUid());
        workflowExecutionFacade.save(workflowExecution);
        workflowExecutionFacade.flush();
        this.oozieFacade.run(workflowExecution);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflowExecution).build();
    }
}
