package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.NodePK;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class DecisionNode extends Node {
    public DecisionNode(){}

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException {
        Element action = execution.getDoc().createElement("decision");
        action.setAttribute("name", this.getOozieId());
        root.appendChild(action);

        Element decisions = execution.getDoc().createElement("switch");
        action.appendChild(decisions);
        for(Node node: this.getChildren()){
            Element decision = execution.getDoc().createElement("case");
            decision.setAttribute("to", node.getOozieId());
            if(node.getDecision() == null) throw new ProcessingException("Decision should not be empty");
            decision.setTextContent(node.getDecision());
            decisions.appendChild(decision);

            node.getWorkflowElement(execution, root);
        }

        return action;
    }
}
