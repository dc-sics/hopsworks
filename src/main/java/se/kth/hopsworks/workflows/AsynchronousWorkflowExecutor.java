package se.kth.hopsworks.workflows;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.OozieClientException;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.util.Settings;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.ProcessingException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class AsynchronousWorkflowExecutor {

    private static final Logger logger = Logger.getLogger(AsynchronousWorkflowExecutor.class.
            getName());

    private static String OOZIE_URL = "http://10.0.2.15:11000/oozie/";
    private static String JOB_TRACKER = "hdfs://10.0.2.15:8032";
    private static String NAME_NODE = "hdfs://10.0.2.15:8020";

    @EJB
    private WorkflowExecutionFacade workflowExecutionFacade;

    @Asynchronous
    public void run(WorkflowExecution workflowExecution, String path, DistributedFsService dfs){
        OozieClient client = new OozieClient(OOZIE_URL);
        Properties conf = client.createConfiguration();
        conf.setProperty(OozieClient.APP_PATH, "${nameNode}" + path);
        conf.setProperty(OozieClient.LIBPATH , "/user/glassfish/workflows/lib");
        conf.setProperty(OozieClient.USE_SYSTEM_LIBPATH , String.valueOf(Boolean.TRUE));
        conf.setProperty("jobTracker", JOB_TRACKER);
        conf.setProperty("nameNode", NAME_NODE);
        conf.setProperty("sparkMaster", "yarn-client");
        conf.setProperty("sparkMode", "client");
        Workflow workflow = workflowExecution.getWorkflow();


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

            }catch (ProcessingException | IOException | UnsupportedOperationException | ParserConfigurationException e) {
                workflowExecution.setError(e.getMessage());
            }
        }

        try{
            String jobId = client.run(conf);
            workflowExecution.setJobId(jobId);
        }catch (OozieClientException e){
            workflowExecution.setError(e.getMessage());
        }
        if(workflowExecution.getError() == null) workflowExecution.setWorkflowTimestamp(workflow.getUpdatedAt());
        workflowExecutionFacade.edit(workflowExecution);
    }
}
