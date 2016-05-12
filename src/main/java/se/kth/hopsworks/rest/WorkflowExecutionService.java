package se.kth.hopsworks.rest;

import org.apache.oozie.client.OozieClientException;
import org.codehaus.jackson.map.ObjectMapper;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.workflows.*;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;


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

    @EJB
    private WorkflowJobFacade workflowJobFacade;

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    private Workflow workflow;

    public WorkflowExecutionService() {

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response index() throws AppException {
        Collection<WorkflowExecution> executions = workflow.getWorkflowExecutions();
        GenericEntity<Collection<WorkflowExecution>> executionsList = new GenericEntity<Collection<WorkflowExecution>>(executions) {
        };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(executionsList).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response show(
            @PathParam("id") Integer id) throws AppException {
        WorkflowExecution execution = workflowExecutionFacade.find(id, workflow);
        if (execution == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WOKFLOW_EXECUTION_NOT_FOUND);
        }
        WorkflowJob job = execution.getJob();
        if (job != null) {
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(job).build();
        } else {
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(execution).build();
        }

    }

    @GET
    @Path("{id}/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response logs(
            @PathParam("id") Integer id) throws AppException {
        WorkflowExecution execution = workflowExecutionFacade.find(id, workflow);
        WorkflowJob job = execution.getJob();
        if (execution == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WOKFLOW_EXECUTION_NOT_FOUND);
        }
        if (job == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WOKFLOW_JOB_NOT_FOUND);
        }
        String log;
        try {
            Map<String, String> logs = oozieFacade.getLogs(execution);
            log = new ObjectMapper().writeValueAsString(logs).toString();
        } catch (OozieClientException | IOException e) {
            //ADD MESSAGE
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "");
        }

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(log).build();

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response create(
            @Context HttpServletRequest req) throws AppException {
        Users user = userBean.findByEmail(req.getRemoteUser());
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
