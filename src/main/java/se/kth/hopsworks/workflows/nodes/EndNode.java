package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class EndNode extends Node {
    public EndNode(){}

    public Element getWorkflowElement(Document doc , Element root) throws ProcessingException{

        if(this.getChildren().size() != 0) throw new ProcessingException("End node should have no descendants");

        Element element = doc.createElement("end");

        element.setAttribute("name", this.getOozieId());
        root.appendChild(element);
        return element;
    }

    public String getOozieId() {
        return this.getId();
    }
}
