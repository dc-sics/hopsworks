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
@Table(name = "hopsworks.edges")
@NamedQueries({
        @NamedQuery(name = "Edge.findAll",
                query
                        = "SELECT e FROM Edge e"),
        @NamedQuery(name = "Edge.findById",
                query
                        = "SELECT e FROM Edge e WHERE e.edgePK = :edgePK")
})

public class Edge implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    protected EdgePK edgePK;

    @JoinColumn(name = "workflow_id",
            insertable = false,
            updatable = false)
    @ManyToOne(optional = false)
    private Workflow workflow;

    public Edge() {
        this.edgePK = new EdgePK();
    }

    public Edge(EdgePK edgePK, String type) {
        this.edgePK = edgePK;
        this.type = type;
    }

    public Edge(String id, Workflow workflow, String type) {
        this.edgePK = new EdgePK(id, workflow.getId());
        this.type = type;
    }

    @JsonIgnore
    @XmlTransient
    public EdgePK getEdgePK() {
        return edgePK;
    }

    public void setEdgePK(EdgePK edgePK) {
        this.edgePK = edgePK;
    }

    public String getId() {
        return edgePK.getId();
    }
    public Integer getWorkflowId() {
        return edgePK.getWorkflowId();
    }

    public void setWorkflowId(Integer workflowId) {
        this.edgePK.setWorkflowId(workflowId);
    }

    public void setId(String id) {
        this.edgePK.setId(id);
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
    @Column(name = "type", length = 255)
    private String type;
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Basic
    @Column(name = "target_id", nullable = false, length = 255)
    private String targetId;
    public String getTargetId() {
        return targetId;
    }
    public void setTargetId(String targetId) {
        if(targetId != null){
            this.targetId = targetId;
        }
    }

    @Basic
    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        if(sourceId != null){
            this.sourceId = sourceId;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (edgePK != null ? edgePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Edge)) {
            return false;
        }
        Edge other = (Edge) object;
        if ((this.edgePK == null && other.edgePK != null)
                || (this.edgePK != null && !this.edgePK.equals(
                other.edgePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "[" + workflow + "," + edgePK.getId() + " ]";
    }
    
}
