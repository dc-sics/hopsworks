package se.kth.hopsworks.rest;


import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.workflows.NodeFacade;
import se.kth.hopsworks.workflows.WorkflowFacade;
import se.kth.hopsworks.workflows.Workflow;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/workflows")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class WorkflowService {
    private final static Logger logger = Logger.getLogger(WorkflowService.class.
            getName());

    @EJB
    private WorkflowFacade workflowFacade;

    @EJB
    private NodeFacade nodeFacade;

    @EJB
    private NoCacheResponse noCacheResponse;

    @Inject
    private NodeService nodeService;

    @Inject
    private EdgeService edgeService;

    public WorkflowService(){

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response index() throws AppException {
        List<Workflow> workflows = workflowFacade.findAll();
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflows).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response create(
            Workflow workflow,
            @Context HttpServletRequest req) throws AppException {
        workflowFacade.persist(workflow);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflow).build();

    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response show(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflow).build();
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response update(
            String stringParams,
            @PathParam("id") Integer id) throws AppException, IllegalAccessException, InvocationTargetException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        Map<String, Object> paramsMap = new ObjectMapper().convertValue(stringParams, Map.class);
        BeanUtils.populate(workflow, paramsMap);
        workflowFacade.merge(workflow);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflowFacade.refresh(workflow)).build();
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response delete(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        workflowFacade.remove(workflow);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    }

    @Path("{id}/nodes")
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public NodeService nodes(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        this.nodeService.setWorkflow(workflow);

        return this.nodeService;
    }

    @Path("{id}/edges")
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public EdgeService edges(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        this.edgeService.setWorkflow(workflow);

        return this.edgeService;
    }
}
