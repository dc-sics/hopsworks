package se.kth.hopsworks.rest;


import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.OozieClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.workflows.*;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    @Inject
    private WorkflowExecutionService workflowExecutionService;

    public WorkflowService(){

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public Response index() throws AppException {
        List<Workflow> workflows = workflowFacade.findAll();
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflows).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public Response create(
            Workflow workflow,
            @Context HttpServletRequest req) throws AppException {
        workflowFacade.persist(workflow);

        JsonNode json = new ObjectMapper().valueToTree(workflow);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json.toString()).build();

    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public Response show(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        JsonNode json = new ObjectMapper().valueToTree(workflow);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json.toString()).build();
    }

//    @PUT
//    @Path("{id}/run")
//    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
//    public Response run(
//            @PathParam("id") Integer id,
//            @Context HttpServletRequest req) throws AppException {
//        Workflow workflow = workflowFacade.findById(id);
//        if (workflow == null) {
//            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
//                    ResponseMessages.WORKFLOW_NOT_FOUND);
//        }
//
//
//    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
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
        workflow = workflowFacade.merge(workflow);
        JsonNode json = new ObjectMapper().valueToTree(workflow);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json.toString()).build();
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
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
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
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
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
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

    @Path("{id}/executions")
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public WorkflowExecutionService executions(
            @PathParam("id") Integer id) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        this.workflowExecutionService.setWorkflow(workflow);

        return this.workflowExecutionService;
    }
}
