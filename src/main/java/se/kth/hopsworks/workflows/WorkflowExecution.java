package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "hopsworks.workflow_executions")
@XmlRootElement
public class WorkflowExecution implements Serializable {

    public WorkflowExecution(){}

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @XmlElement(name = "id")
    private Integer id;
    public Integer getId() {
        return id;
    }

    @Basic(optional = false)
    @Column(name = "job_id", nullable = false, length = 255)
    private String jobId;
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Basic(optional = false)
    @NotNull
    @Column(name = "workflow_id", nullable = false)
    private Integer workflowId;
    public Integer getWorkflowId() {
        return workflowId;
    }
    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
    }



    @Basic(optional = false)
    @Column(name = "workflow_timestamp")
    private Date workflowTimestamp;
    public Date getWorkflowTimestamp() {
        return workflowTimestamp;
    }

    public void setWorkflowTimestamp(Date workflowTimestamp) {
        this.workflowTimestamp = workflowTimestamp;
    }

    @Basic(optional = false)
    @Column(name = "error", nullable = false)
    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkflowExecution workflowExecution = (WorkflowExecution) o;

        if (id != null ? !id.equals(workflowExecution.id) : workflowExecution.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        return result;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @PrimaryKeyJoinColumn(name="workflow_id")
    private Workflow workflow;

    @JsonIgnore
    @XmlTransient
    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

}
