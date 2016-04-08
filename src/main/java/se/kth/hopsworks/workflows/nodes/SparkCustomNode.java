package se.kth.hopsworks.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


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

    public Element getWorkflowElement(Document doc , Element root) throws ProcessingException{
        /* Add  prepare job-xml configuration spark-opts arg*/

        if(this.getJar().isEmpty() || this.getMainClass().isEmpty()) throw new ProcessingException("Missing arguments for Spark Job");
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");

        Element action = doc.createElement("action");
        action.setAttribute("name", this.getId());


        Element spark = doc.createElement("spark");
        Node child = this.getChildren().iterator().next();
        spark.setAttribute("xmlns", "uri:oozie:spark-action:0.1");

        Element jobTracker = doc.createElement("job-tracker");
        jobTracker.setTextContent("${jobTracker}");
        spark.appendChild(jobTracker);

        Element nameNode = doc.createElement("name-node");
        nameNode.setTextContent("${nameNode}");
        spark.appendChild(nameNode);

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
        jar.setTextContent(this.getJar());
        spark.appendChild(jar);

        action.appendChild(spark);

        Element ok = doc.createElement("ok");
        ok.setAttribute("to", child.getId());
        action.appendChild(ok);

        Element error = doc.createElement("error");
        error.setAttribute("to", "end");
        action.appendChild(error);

        root.appendChild(action);
        child.getWorkflowElement(doc, root);

        return action;
    }

}
