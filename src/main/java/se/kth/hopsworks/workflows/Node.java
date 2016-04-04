package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.json.JSONObject;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@XmlRootElement
@Table(name = "hopsworks.nodes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "classname")
@NamedQueries({
        @NamedQuery(name = "Node.findAll",
                query
                        = "SELECT n FROM Node n"),
        @NamedQuery(name = "Node.findById",
                query
                        = "SELECT n FROM Node n WHERE n.nodePK = :nodePK")})
public abstract  class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classname;

    @EmbeddedId
    protected NodePK nodePK;

    private String type;

    @JoinColumn(name = "workflow_id",
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

    @Basic
    @Column(name = "data")
    private  String data;

    public JSONObject getData() {
        return new JSONObject(this.data);
    }

    public void setData(JSONObject data) {
        this.data = data.toString();
    }

//    public void setData(String data) {
//        this.data = data;
//    }

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


    @OneToMany(cascade = CascadeType.ALL,
        fetch = FetchType.LAZY)
    @JoinTable(name = "hopsworks.edges",
            joinColumns = {
                    @JoinColumn(table= "nodes", name = "source_id", referencedColumnName = "id") ,
                    @JoinColumn(table= "nodes", name = "workflow_id", referencedColumnName = "workflow_id")
            },
            inverseJoinColumns ={
                    @JoinColumn(table= "nodes", name = "target_id", referencedColumnName = "id") ,
                    @JoinColumn(table= "nodes", name = "workflow_id", referencedColumnName = "workflow_id")
            }
    )
    private Set<Node> children;
    @JsonIgnore
    @XmlTransient
    public Set<Node> getChildren() {
        return this.children;
    }

    @XmlElement(name = "childrenIds")
    public Set<String> getChildrenIds() {
        Set<String> ids = new HashSet();
        for(Node child : this.children){
            ids.add(child.getId());
        }
        return ids;
    }

    public void setChildren(Set<Node> children) {
        this.children = children;
    }

    @PreUpdate
    public void updateTimeStamps() {
       this.updatedAt = new Date();
    }

}
