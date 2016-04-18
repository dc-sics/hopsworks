package se.kth.hopsworks.rest;

import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.workflows.AsynchronousWorkflowExecutor;
import se.kth.hopsworks.workflows.WorkflowExecution;
import se.kth.hopsworks.workflows.WorkflowExecutionFacade;
import se.kth.hopsworks.workflows.Workflow;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


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
    private AsynchronousWorkflowExecutor async;

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    private Workflow workflow;

    public WorkflowExecutionService(){

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
        workflowExecutionFacade.save(workflowExecution);
        workflowExecutionFacade.flush();
        this.async.run(workflowExecution, path, dfs);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflowExecution).build();
    }
}
