package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.eclipse.persistence.annotations.AdditionalCriteria;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.workflows.nodes.BlankNode;
import se.kth.hopsworks.workflows.nodes.RootNode;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    @Basic(optional = false)
    @Column(name = "xml_created_at")
    private Date xmlCreatedAt;

    public Date getXmlCreatedAt() {
        return xmlCreatedAt;
    }

    public void setXmlCreatedAt(Date xmlCreatedAt) {
        this.xmlCreatedAt = xmlCreatedAt;
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
    private Collection<BlankNode> blankNodes;

    @JsonIgnore
    public Collection<BlankNode> getBlankNodes() {
        return blankNodes;
    }

    @OneToOne(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private RootNode rootNode;

    @JsonIgnore
    public RootNode getRootNode() {
        return rootNode;
    }

    public Boolean isComplete(){
        if(this.getBlankNodes().size() > 0) return false;
        return true;
    }

    private transient String path;
    @JsonIgnore
    @XmlTransient
    public String getPath() {
        return path;
    }

    private transient DistributedFsService dfs;
    @JsonIgnore
    @XmlTransient
    public DistributedFsService getDfs() {
        return dfs;
    }

    public Document makeWorkflowFile(String path, DistributedFsService dfs) throws ParserConfigurationException, ProcessingException, UnsupportedOperationException{

        if(!this.isComplete()) throw new ProcessingException("Workflow is not in a complete state");
        this.path = path;
        this.dfs = dfs;
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element workflow = doc.createElement("workflow-app");
        workflow.setAttribute("name", this.getName());
        workflow.setAttribute("xmlns", "uri:oozie:workflow:0.5");
        rootNode.getWorkflowElement(doc, workflow);
        doc.appendChild(workflow);
        return doc;
    }


}
