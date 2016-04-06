package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;


@Entity
@Table(name = "hopsworks.workflows")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Workflow.findAll",
                query
                        = "SELECT w FROM Workflow w"),
        @NamedQuery(name = "Workflow.findById",
                query
                        = "SELECT w FROM Workflow w WHERE w.id = :id"),
        @NamedQuery(name = "Workflow.findByName",
                query
                        = "SELECT w FROM Workflow w WHERE w.name = :name")})
public class Workflow implements Serializable {

    public Workflow(){

    }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "id", nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "name", nullable = false, length = 20)
    private String name;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Workflow workflows = (Workflow) o;

        if (id != null ? !id.equals(workflows.id) : workflows.id != null) return false;
        if (name != null ? !name.equals(workflows.name) : workflows.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<Edge> edges;
    public Collection<Edge> getEdges() {
        return edges;
    }

    public void setEdges(Collection<Edge> edges) {
        this.edges = edges;
    }


    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<Node> nodes;
    public Collection<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<Node> blankNodes;
    public Collection<Node> getblankNodes() {
        return blankNodes;
    }
}
