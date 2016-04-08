package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class DecisionNode extends Node {
    public DecisionNode(){}

    public Element getWorkflowElement(Document doc , Element root) throws ProcessingException {
        return null;
    }
}
