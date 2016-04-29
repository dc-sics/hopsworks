package se.kth.hopsworks.workflows;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.WorkflowJob;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class OozieFacade {

    private static final Logger logger = Logger.getLogger(OozieFacade.class.
            getName());

    private static String OOZIE_URL = "http://10.0.2.15:11000/oozie/";
    private static String JOB_TRACKER = "hdfs://10.0.2.15:8032";
    private static String NAME_NODE = "hdfs://10.0.2.15:8020";

    @EJB
    private WorkflowExecutionFacade workflowExecutionFacade;

    @EJB
    private DistributedFsService dfs;

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public String getPath() {
        return path;
    }

    public DistributedFsService getDfs() {
        return dfs;
    }

    public EntityManager getEm() {

        return em;
    }

    private String path;

    private Document doc;

    public Document getDoc() {
        return doc;
    }

    private Set<String> nodeIds;

    public Boolean hasNodeId(String id) {
        return nodeIds.contains(id);
    }

    public void addNodeId(String id){
        nodeIds.add(id);
    }

    public void removeNodeId(String id){
        nodeIds.remove(id);
    }

    public OozieFacade(){
        nodeIds = new HashSet<String>();
    }

    public Map<String, String> getLogs(WorkflowExecution execution) throws IOException, OozieClientException{
        OozieClient client = new OozieClient(OOZIE_URL);
        String path = "/Workflows/" + execution.getUser().getUsername() + "/" + execution.getWorkflow().getName() + "/" + execution.getWorkflow().getUpdatedAt().getTime() + "/";
        DistributedFileSystemOps dfsOps = dfs.getDfsOps();
        PrintStream ps;

        Map<String, String> logs = new HashMap<String, String>();

        String defaultLog = client.getJobLog(execution.getJobId());


        if(!dfsOps.exists(path.concat("/logs/default.log")) || !execution.getJob().isDone()){
            ps = new PrintStream(dfsOps.create(path.concat("/logs/default.log")));
            ps.print(defaultLog);
            ps.flush();
            ps.close();
        }

        if(!dfsOps.exists(path.concat("/logs/error.log")) || !execution.getJob().isDone()){
            ps = new PrintStream(dfsOps.create(path.concat("/logs/error.log")));
            client.getJobErrorLog(execution.getJobId(), ps);
            ps.flush();
            ps.close();
        }
        String errorLog = dfsOps.cat(path.concat("/logs/error.log"));

        if(!dfsOps.exists(path.concat("/logs/audit.log")) || !execution.getJob().isDone()){
            ps = new PrintStream(dfsOps.create(path.concat("/logs/audit.log")));
            client.getJobAuditLog(execution.getJobId(), ps);
            ps.flush();
            ps.close();
        }

        String auditLog = dfsOps.cat(path.concat("/logs/audit.log"));

        logs.put("default", defaultLog);
        logs.put("error", errorLog);
        logs.put("audit", auditLog);
        return logs;
    }
    @Asynchronous
    public void run(WorkflowExecution workflowExecution){
        try {
            this.path = "/Workflows/" + workflowExecution.getUser().getUsername() + "/" + workflowExecution.getWorkflow().getName() + "/" + workflowExecution.getWorkflow().getUpdatedAt().getTime() + "/";
            OozieClient client = new OozieClient(OOZIE_URL);
            Properties conf = client.createConfiguration();
            conf.setProperty(OozieClient.APP_PATH, "${nameNode}" + path);
            conf.setProperty(OozieClient.LIBPATH, "/user/glassfish/workflows/lib");
            conf.setProperty(OozieClient.USE_SYSTEM_LIBPATH, String.valueOf(Boolean.TRUE));
            conf.setProperty("jobTracker", JOB_TRACKER);
            conf.setProperty("nameNode", NAME_NODE);
            conf.setProperty("sparkMaster", "yarn-client");
            conf.setProperty("sparkMode", "client");
            Workflow workflow = workflowExecution.getWorkflow();


            if (workflow.getWorkflowExecutions().size() == 0 || !workflow.getUpdatedAt().equals(workflow.getWorkflowExecutions().get(0).getWorkflowTimestamp())) {

                DistributedFileSystemOps dfsOps = dfs.getDfsOps();
                dfsOps.create(path.concat("lib/init"));
                dfsOps.close();
                this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                workflow.makeWorkflowFile(this);
                FSDataOutputStream fsStream = dfs.getDfsOps().create(path.concat("workflow.xml"));
                DOMImplementationLS domImplementation = (DOMImplementationLS) this.doc.getImplementation();
                LSSerializer lsSerializer = domImplementation.createLSSerializer();
                PrintStream ps = new PrintStream(fsStream);
                lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
                lsSerializer.getDomConfig().setParameter("xml-declaration", false); // Set this to true if the declaration is needed to be outputted.
                ps.print(lsSerializer.writeToString(this.doc));
                ps.flush();
                ps.close();


                String jobId = client.run(conf);
                workflowExecution.setJobId(jobId);

            }
        }catch (Exception e) {
            workflowExecution.setError(e.getMessage());
        }
        finally {
            if(workflowExecution.getError() == null && workflowExecution.getJobId() != null) {
                workflowExecution.setWorkflowTimestamp(workflowExecution.getWorkflow().getUpdatedAt());
            }
            workflowExecutionFacade.edit(workflowExecution);

        }

    }

    public WorkflowJob getJob(String id) throws OozieClientException{
        OozieClient client = new OozieClient(OOZIE_URL);
        WorkflowJob job = client.getJobInfo(id);

        return job;
    }
}
