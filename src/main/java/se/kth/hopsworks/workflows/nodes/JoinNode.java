package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class JoinNode extends Node{
    public JoinNode(){}

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException {
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");

        Element element = execution.getDoc().createElement("join");
        root.appendChild(element);

        Node child = this.getChildren().iterator().next();

        element.setAttribute("to", child.getOozieId());
        element.setAttribute("name", this.getOozieId());
        child.getWorkflowElement(execution, root);

        return element;
    }
}
