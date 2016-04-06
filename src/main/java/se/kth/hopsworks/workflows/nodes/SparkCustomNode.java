package se.kth.hopsworks.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONObject;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
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
        return this.getData().get("jar").getValueAsText();
    }

    @XmlElement(name = "mainClass")
    public String getMainClass() {
        return this.getData().get("mainClass").getValueAsText();
    }

}
