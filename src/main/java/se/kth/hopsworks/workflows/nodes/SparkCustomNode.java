package se.kth.hopsworks.workflows.nodes;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.node.ArrayNode;
import org.w3c.dom.Element;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.util.Iterator;


@Entity
@XmlRootElement
public class SparkCustomNode extends Node {

    public SparkCustomNode(){
        super();
    }

    @XmlElement(name = "jar")
    public String getJar() {
        if(this.getData().get("jar") == null) return null;
        return this.getData().get("jar").getValueAsText();
    }

    @XmlElement(name = "mainClass")
    public String getMainClass() {
        if(this.getData().get("mainClass") == null) return null;
        return this.getData().get("mainClass").getValueAsText();
    }

    @XmlElement(name = "prepare")
    public Boolean getPrepare() {
        if(this.getData().get("prepare") == null) return null;
        return this.getData().get("prepare").getValueAsBoolean();
    }

    @XmlElement(name = "name")
    public String getName() {
        if(this.getData().get("name") == null) return this.getId();
        return this.getData().get("name").getValueAsText();
    }

    @JsonIgnore
    @XmlTransient
    public Iterator<JsonNode> getArguments() {
        if(this.getData().get("arguments") == null) return null;
        return this.getData().get("arguments").getElements();
    }

    @JsonIgnore
    @XmlTransient
    public Iterator<JsonNode> getJobXmls() {
        if(this.getData().get("jobXmls") == null) return null;
        return this.getData().get("jobXmls").getElements();
    }

    @JsonIgnore
    @XmlTransient
    public Iterator<JsonNode> getConfigurations() {
        if(this.getData().get("configurations") == null) return null;
        return this.getData().get("configurations").getElements();
    }

    @JsonIgnore
    @XmlTransient
    public Iterator<JsonNode> getMkDirs() {
        if(this.getData().get("mkDirs") == null) return null;
        return this.getData().get("mkDirs").getElements();
    }

    @JsonIgnore
    @XmlTransient
    public Iterator<JsonNode> getRmDirs() {
        if(this.getData().get("rmDirs") == null) return null;
        return this.getData().get("rmDirs").getElements();
    }

    @JsonIgnore
    @XmlTransient
    public String getOpts() {
        if(this.getData().get("sparkOptions") == null) return null;
        return this.getData().get("sparkOptions").getValueAsText();
    }

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException{
        /* Add  prepare job-xml configuration spark-opts arg*/

        if(this.getJar().isEmpty() || this.getMainClass().isEmpty()) throw new ProcessingException("Missing arguments for Spark Job");
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");
        if(execution.hasNodeId(this.getOozieId())) return null;

        Element action = execution.getDoc().createElement("action");
        action.setAttribute("name", this.getOozieId());


        Element spark = execution.getDoc().createElement("spark");
        Node child = this.getChildren().iterator().next();
        spark.setAttribute("xmlns", "uri:oozie:spark-action:0.1");

        Element jobTracker = execution.getDoc().createElement("job-tracker");
        jobTracker.setTextContent("${jobTracker}");
        spark.appendChild(jobTracker);

        Element nameNode = execution.getDoc().createElement("name-node");
        nameNode.setTextContent("${nameNode}");
        spark.appendChild(nameNode);

        Iterator<JsonNode> rmDirs = this.getRmDirs();
        Iterator<JsonNode> mkDirs = this.getMkDirs();
        Element prepare = execution.getDoc().createElement("prepare");
        if(rmDirs != null){
            Element del;
            while(rmDirs.hasNext()){
                del = execution.getDoc().createElement("delete");
                del.setAttribute("path", rmDirs.next().getValueAsText());
                prepare.appendChild(del);
            }
        }

        if(mkDirs != null){
            Element make;
            while(mkDirs.hasNext()){
                make = execution.getDoc().createElement("mkdir");
                make.setAttribute("path", mkDirs.next().getValueAsText());
                prepare.appendChild(make);
            }
        }

        spark.appendChild(prepare);

        Iterator<JsonNode> xmls = this.getJobXmls();
        if(xmls != null){
            Element xml;
            while(xmls.hasNext()){
                xml = execution.getDoc().createElement("job-xml");
                xml.setTextContent(xmls.next().getTextValue());
                spark.appendChild(xml);
            }
        }

        Iterator<JsonNode> confs = this.getConfigurations();
        if(confs != null){
            Element conf, confName, confVal;
            ArrayNode arrayNode;
            while(confs.hasNext()){
                conf = execution.getDoc().createElement("configuration");
                confName = execution.getDoc().createElement("name");
                confVal = execution.getDoc().createElement("value");
                arrayNode = (ArrayNode) confs.next();
                confName.setTextContent(arrayNode.get(0).getTextValue());
                confVal.setTextContent(arrayNode.get(1).getTextValue());
                conf.appendChild(confName);
                conf.appendChild(confVal);
                spark.appendChild(conf);
            }
        }

        Element master = execution.getDoc().createElement("master");
        master.setTextContent("${sparkMaster}");
        spark.appendChild(master);

        Element mode = execution.getDoc().createElement("mode");
        mode.setTextContent("${sparkMode}");
        spark.appendChild(mode);

        Element name = execution.getDoc().createElement("name");
        name.setTextContent(this.getName());
        spark.appendChild(name);

        Element mainClass = execution.getDoc().createElement("class");
        mainClass.setTextContent(this.getMainClass());
        spark.appendChild(mainClass);

        Element jar = execution.getDoc().createElement("jar");
        String jarPath = this.getJar();
        String jarName = jarPath.split("/")[jarPath.split("/").length - 1];
        try {
            DistributedFileSystemOps dfsops = execution.getDfs().getDfsOps();
            dfsops.copyInHdfs(new Path(jarPath), new Path(execution.getPath().concat("lib/")));
        }catch(IOException e){
            throw new ProcessingException(e.getMessage());
        }
        jar.setTextContent("${nameNode}" + execution.getPath().concat("lib/").concat(jarName));
        spark.appendChild(jar);

        Element opts = execution.getDoc().createElement("spark-opts");
        opts.setTextContent(this.getOpts());
        spark.appendChild(opts);

        Iterator<JsonNode> args = this.getArguments();
        if(args != null){
            Element arg;
            while(args.hasNext()){
                arg = execution.getDoc().createElement("arg");
                arg.setTextContent(args.next().getTextValue());
                spark.appendChild(arg);
            }
        }

        action.appendChild(spark);

        Element ok = execution.getDoc().createElement("ok");
        ok.setAttribute("to", child.getOozieId());
        action.appendChild(ok);

        Element error = execution.getDoc().createElement("error");
        error.setAttribute("to", "kill");
        action.appendChild(error);

        root.appendChild(action);
        if(child.getClass() != JoinNode.class) child.getWorkflowElement(execution, root);

        execution.addNodeId(this.getOozieId());
        return action;
    }

}
