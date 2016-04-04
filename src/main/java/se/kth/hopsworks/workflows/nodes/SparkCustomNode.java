package se.kth.hopsworks.workflows.nodes;

import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class SparkCustomNode extends Node {
    public SparkCustomNode(){}

    @XmlElement(name = "jar")
    public String getJar() {
        return this.getData().get("jar").toString();
    }

    @XmlElement(name = "mainClass")
    public String getMainClass() {
        return this.getData().get("mainClass").toString();
    }


}
