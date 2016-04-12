package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class RootNode extends Node {
    public RootNode(){}

    public Element getWorkflowElement(Document doc ,Element root) throws ProcessingException{

        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");

        Element element = doc.createElement("start");
        root.appendChild(element);

        Node child = this.getChildren().iterator().next();

        element.setAttribute("to", child.getOozieId());
        child.getWorkflowElement(doc, root);

        return element;
    }

    public String getOozieId() {
        return this.getId();
    }
}
