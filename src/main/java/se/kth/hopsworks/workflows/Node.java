package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@XmlRootElement
@Table(name = "hopsworks.nodes")
@NamedQueries({
        @NamedQuery(name = "Node.findAll",
                query
                        = "SELECT n FROM Node n"),
        @NamedQuery(name = "Node.findById",
                query
                        = "SELECT n FROM Node n WHERE n.nodePK = :nodePK")})
public class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    protected NodePK nodePK;

    private String type;

    @JoinColumn(name = "workflow_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false)
    @ManyToOne(optional = false)
    private Workflow workflow;

    public Node() {
        this.nodePK = new NodePK();
    }

    public Node(NodePK nodePK, String type) {
        this.nodePK = nodePK;
        this.type = type;
    }

    public Node(String id, Workflow workflow, String type) {
        this.nodePK = new NodePK(id, workflow.getId());
        this.type = type;
    }

    @JsonIgnore
    @XmlTransient
    public NodePK getNodePK() {
        return nodePK;
    }


    public void setNodePK(NodePK nodePK) {
        this.nodePK = nodePK;
    }

    public String getId() {
        return nodePK.getId();
    }
    public Integer getWorkflowId() {
        return nodePK.getWorkflowId();
    }

    public void setWorkflowId(Integer workflowId) {
        this.nodePK.setWorkflowId(workflowId);
    }

    public void setId(String id) {
        this.nodePK.setId(id);
    }

    @JsonIgnore
    @XmlTransient
    public Workflow getWorkflow() {
        return workflow;
    }


    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    @Basic(optional = false)
    @Column(name = "created_at")
    private Date createdAt;
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Basic(optional = false)
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Basic
    @Column(name = "type", nullable = false, length = 255)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (nodePK != null ? nodePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Node)) {
            return false;
        }
        Node other = (Node) object;
        if ((this.nodePK == null && other.nodePK != null)
                || (this.nodePK != null && !this.nodePK.equals(
                other.nodePK))) {
            return false;
        }
        return true;
    }

//    @Override
//    public String toString() {
//        return "[" + workflow + "," + nodePK.getId() + " ]";
//    }








    /*
    private Set<Node> children;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    public Set<Node> getChildren() {
        return this.children;
    }

    public void setChildren(Set<Node> children) {
        this.children = children;
    }

    @ManyToOne(optional = false)
    @JoinTable(name = "edges", joinColumns = { @JoinColumn(name = "source_id", referencedColumnName = "id") }, inverseJoinColumns = { @JoinColumn(name = "target_id", referencedColumnName = "id") })
    private Node parent;

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }
    */
}
