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
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.workflows.NodeFacade;
import se.kth.hopsworks.workflows.WorkflowFacade;
import se.kth.hopsworks.workflows.Workflow;

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

    @EJB
    private UserFacade userBean;

    @EJB
    private DistributedFsService dfs;

    @Inject
    private NodeService nodeService;

    @Inject
    private EdgeService edgeService;

    private static String OOZIE_URL = "http://10.0.2.15:11000/oozie/";
    private static String JOB_TRACKER = "hdfs://10.0.2.15:8032";
    private static String NAME_NODE = "hdfs://10.0.2.15:8020";


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

    @PUT
    @Path("{id}/run")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public Response run(
            @PathParam("id") Integer id,
            @Context HttpServletRequest req) throws AppException {
        Workflow workflow = workflowFacade.findById(id);
        if (workflow == null) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    ResponseMessages.WORKFLOW_NOT_FOUND);
        }
        Users user = userBean.findByEmail(req.getRemoteUser());
        String path = "/Workflows/" + user.getUsername() + "/" + workflow.getName() + "/" + workflow.getUpdatedAt().getTime() + "/";
        OozieClient client = new OozieClient(OOZIE_URL);
        Properties conf = client.createConfiguration();
        conf.setProperty(OozieClient.APP_PATH, "${nameNode}" + path);
        conf.setProperty(OozieClient.LIBPATH , "/user/glassfish/workflows/lib");
        conf.setProperty(OozieClient.USE_SYSTEM_LIBPATH , String.valueOf(Boolean.TRUE));
        conf.setProperty("jobTracker", JOB_TRACKER);
        conf.setProperty("nameNode", NAME_NODE);
        conf.setProperty("sparkMaster", "yarn-client");
        conf.setProperty("sparkMode", "client");

        if(!workflow.getUpdatedAt().equals(workflow.getXmlCreatedAt())){

            try{
                DistributedFileSystemOps dfsOps = dfs.getDfsOps();
                dfsOps.create(path.concat("lib/init"));
                dfsOps.close();

                Document workflowFile = workflow.makeWorkflowFile(path, dfs);
                FSDataOutputStream fsStream = dfs.getDfsOps().create(path.concat("workflow.xml"));
                DOMImplementationLS domImplementation = (DOMImplementationLS) workflowFile.getImplementation();
                LSSerializer lsSerializer = domImplementation.createLSSerializer();
                PrintStream ps = new PrintStream(fsStream);
                ps.print(lsSerializer.writeToString(workflowFile));
                ps.flush();
                ps.close();

                workflow.setXmlCreatedAt(workflow.getUpdatedAt());

            }catch (ProcessingException e){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch (IOException e){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch(UnsupportedOperationException e){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch (ParserConfigurationException e){
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }
        }

        try{
            String jobId = client.run(conf);
            workflowFacade.merge(workflow);
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity("{'job': " + jobId +"}").build();
        }catch (OozieClientException e){
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
        }

    }

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
}
