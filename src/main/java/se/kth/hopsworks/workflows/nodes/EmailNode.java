package se.kth.hopsworks.workflows.nodes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class EmailNode extends Node {
    public EmailNode(){}

    @XmlElement(name = "to")
    public String getTo() {
        if(this.getData().get("to") == null) return null;
        return this.getData().get("to").getValueAsText();
    }

    @XmlElement(name = "body")
    public String getBody() {
        if(this.getData().get("body") == null) return null;
        return this.getData().get("body").getValueAsText();
    }

    @XmlElement(name = "subject")
    public String getSubject() {
        if(this.getData().get("subject") == null) return null;
        return this.getData().get("subject").getValueAsText();
    }

    @XmlElement(name = "cc")
    public String getCC() {
        if(this.getData().get("cc") == null) return null;
        return this.getData().get("cc").getValueAsText();
    }

    @XmlElement(name = "name")
    public String getName() {
        if(this.getData().get("name") == null) return this.getId();
        return this.getData().get("name").getValueAsText();
    }

    public Element getWorkflowElement(Document doc , Element root) throws ProcessingException{
        /* Add  attachment content_type*/

        if(this.getTo().isEmpty() || this.getBody().isEmpty() || this.getSubject().isEmpty()) throw new ProcessingException("Missing arguments for Email");
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");

        Element action = doc.createElement("action");
        action.setAttribute("name", this.getOozieId());


        Element email = doc.createElement("email");
        Node child = this.getChildren().iterator().next();
        email.setAttribute("xmlns", "uri:oozie:email-action:0.1");

        Element to = doc.createElement("to");
        to.setTextContent(this.getTo());
        email.appendChild(to);

        Element subject = doc.createElement("subject");
        subject.setTextContent(this.getSubject());
        email.appendChild(subject);

        Element body = doc.createElement("body");
        body.setTextContent(this.getBody());
        email.appendChild(body);

        if(!this.getCC().isEmpty()){
            Element cc = doc.createElement("cc");
            cc.setTextContent(this.getCC());
            email.appendChild(cc);
        }

        action.appendChild(email);

        Element ok = doc.createElement("ok");
        ok.setAttribute("to", child.getOozieId());
        action.appendChild(ok);

        Element error = doc.createElement("error");
        error.setAttribute("to", "end");
        action.appendChild(error);

        root.appendChild(action);
        child.getWorkflowElement(doc, root);
        return action;
    }
}
