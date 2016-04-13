package se.kth.hopsworks.rest;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.OozieClientException;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
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
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

@RequestScoped
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

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    private Workflow workflow;

    private static String OOZIE_URL = "http://10.0.2.15:11000/oozie/";
    private static String JOB_TRACKER = "hdfs://10.0.2.15:8032";
    private static String NAME_NODE = "hdfs://10.0.2.15:8020";

    public WorkflowExecutionService(){

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"SYS_ADMIN", "BBC_USER"})
    public Response create(
            @Context HttpServletRequest req) throws AppException {
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

        WorkflowExecution workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowId(workflow.getId());
        if(workflow.getWorkflowExecutions().size() == 0 || !workflow.getUpdatedAt().equals(workflow.getWorkflowExecutions().get(0).getWorkflowTimestamp())){

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

            }catch (ProcessingException e){
                workflowExecution.setError(e.getMessage());
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch (IOException e){
                workflowExecution.setError(e.getMessage());
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch(UnsupportedOperationException e){
                workflowExecution.setError(e.getMessage());
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }catch (ParserConfigurationException e){
                workflowExecution.setError(e.getMessage());
                throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
            }
        }

        try{
            String jobId = client.run(conf);
            workflowExecution.setJobId(jobId);
        }catch (OozieClientException e){
            workflowExecution.setError(e.getMessage());
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage());
        }
        if(workflowExecution.getError() == null) workflowExecution.setWorkflowTimestamp(workflow.getUpdatedAt());

        workflowExecutionFacade.save(workflowExecution);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(workflowExecution).build();
    }
}
