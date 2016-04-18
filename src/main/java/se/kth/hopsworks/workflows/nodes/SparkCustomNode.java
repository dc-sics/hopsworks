package se.kth.hopsworks.workflows.nodes;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.workflows.Node;

import javax.ejb.EJB;
import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


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

    public Element getWorkflowElement(Document doc , Element root) throws ProcessingException{
        /* Add  prepare job-xml configuration spark-opts arg*/

        if(this.getJar().isEmpty() || this.getMainClass().isEmpty()) throw new ProcessingException("Missing arguments for Spark Job");
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");

        Element action = doc.createElement("action");
        action.setAttribute("name", this.getOozieId());


        Element spark = doc.createElement("spark");
        Node child = this.getChildren().iterator().next();
        spark.setAttribute("xmlns", "uri:oozie:spark-action:0.1");

        Element jobTracker = doc.createElement("job-tracker");
        jobTracker.setTextContent("${jobTracker}");
        spark.appendChild(jobTracker);

        Element nameNode = doc.createElement("name-node");
        nameNode.setTextContent("${nameNode}");
        spark.appendChild(nameNode);

        Iterator<JsonNode> rmDirs = this.getRmDirs();
        Iterator<JsonNode> mkDirs = this.getMkDirs();
        Element prepare = doc.createElement("prepare");
        if(rmDirs != null){
            Element del;
            while(rmDirs.hasNext()){
                del = doc.createElement("delete");
                del.setAttribute("path", rmDirs.next().getValueAsText());
                prepare.appendChild(del);
            }
        }

        if(mkDirs != null){
            Element make;
            while(mkDirs.hasNext()){
                make = doc.createElement("mkdir");
                make.setAttribute("path", mkDirs.next().getValueAsText());
                prepare.appendChild(make);
            }
        }

        spark.appendChild(prepare);

        Iterator<JsonNode> xmls = this.getJobXmls();
        if(xmls != null){
            Element xml;
            while(xmls.hasNext()){
                xml = doc.createElement("job-xml");
                xml.setTextContent(xmls.next().getTextValue());
                spark.appendChild(xml);
            }
        }

//        Iterator<JsonNode> confs = this.getConfigurations();
//        if(confs != null){
//            Element conf, confName, confVal;
//            while(xmls.hasNext()){
//                conf = doc.createElement("configuration");
//                confName = doc.createElement("name");
//                confVal = doc.createElement("value");
//                conf.setTextContent(confs.next().getTextValue());
//                spark.appendChild(conf);
//            }
//        }

        Element master = doc.createElement("master");
        master.setTextContent("${sparkMaster}");
        spark.appendChild(master);

        Element mode = doc.createElement("mode");
        mode.setTextContent("${sparkMode}");
        spark.appendChild(mode);

        Element name = doc.createElement("name");
        name.setTextContent(this.getName());
        spark.appendChild(name);

        Element mainClass = doc.createElement("class");
        mainClass.setTextContent(this.getMainClass());
        spark.appendChild(mainClass);

        Element jar = doc.createElement("jar");
        String jarPath = this.getJar();
        String jarName = jarPath.split("/")[jarPath.split("/").length - 1];
        try {
            DistributedFileSystemOps dfsops = this.getWorkflow().getDfs().getDfsOps();
            dfsops.copyInHdfs(new Path(jarPath), new Path(this.getWorkflow().getPath().concat("lib/")));
        }catch(IOException e){
            throw new ProcessingException(e.getMessage());
        }
        jar.setTextContent("${nameNode}" + this.getWorkflow().getPath().concat("lib/").concat(jarName));
        spark.appendChild(jar);

        Element opts = doc.createElement("spark-opts");
        opts.setTextContent(this.getOpts());
        spark.appendChild(opts);

        Iterator<JsonNode> args = this.getArguments();
        if(args != null){
            Element arg;
            while(args.hasNext()){
                arg = doc.createElement("arg");
                arg.setTextContent(args.next().getTextValue());
                spark.appendChild(arg);
            }
        }

        action.appendChild(spark);

        Element ok = doc.createElement("ok");
        ok.setAttribute("to", child.getOozieId());
        action.appendChild(ok);

        Element error = doc.createElement("error");
        error.setAttribute("to", "end");
        action.appendChild(error);

        root.appendChild(action);
        child.getWorkflowElement(doc, root);

        return action;
    }

}
