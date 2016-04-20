package se.kth.hopsworks.workflows.nodes;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.*;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@XmlRootElement
public class ForkNode extends Node {


    @JsonIgnore
    @XmlTransient
    public String getJoinNodeId() {
        if(this.getData().get("joinNodeId") == null) return null;
        return this.getData().get("joinNodeId").getValueAsText();
    }

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException {
        Element fork = execution.getDoc().createElement("fork");
        fork.setAttribute("name", this.getOozieId());
        root.appendChild(fork);
        for(Node node: this.getChildren()){
            Element path = execution.getDoc().createElement("path");
            path.setAttribute("start", node.getOozieId());
            fork.appendChild(path);

            node.getWorkflowElement(execution, root);
        }
        JoinNode joinNode = execution.getEm().find(JoinNode.class, new NodePK(this.getJoinNodeId(), this.getWorkflowId()));
        joinNode.getWorkflowElement(execution, root);

        return fork;
    }
}
