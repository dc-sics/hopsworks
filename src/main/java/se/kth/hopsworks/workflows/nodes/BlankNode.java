package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class BlankNode extends Node {
    public BlankNode(){}

    public Element getWorkflowElement(OozieFacade execution, Element root) throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Blank node is not part of the Workflow");
    }
    public String getOozieId() throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Blank node is not part of the Workflow");
    }
}
