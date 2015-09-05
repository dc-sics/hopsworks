package se.kth.bbc.jobs.cuneiform.model;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * A wrapper around a WorkflowDTO and a YarnJobConfiguration for easy
 * communication with the client.
 * <p>
 * @author stig
 */
@XmlRootElement
public class CuneiformJobConfiguration extends YarnJobConfiguration {

  private WorkflowDTO wf;

  protected static final String KEY_WORKFLOW = "workflow";

  public CuneiformJobConfiguration() {
    super();
  }

  public CuneiformJobConfiguration(WorkflowDTO wf) {
    this.wf = wf;
  }

  public WorkflowDTO getWf() {
    return wf;
  }

  public void setWf(WorkflowDTO wf) {
    this.wf = wf;
  }

  @Override
  public JobType getType() {
    return JobType.CUNEIFORM;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    if (wf == null) {
      throw new NullPointerException(
              "Null workflowDTO in CuneiformJobConfiguration.");
    }
    MutableJsonObject obj = super.getReducedJsonObject();
    obj.set(KEY_TYPE, JobType.CUNEIFORM.name());
    obj.set(KEY_WORKFLOW, wf.getReducedJsonObject());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    WorkflowDTO workflow;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.CUNEIFORM) {
        throw new IllegalArgumentException("JobType must be CUNEIFORM.");
      }
      MutableJsonObject jsonWf = json.getJsonObject(KEY_WORKFLOW);
      workflow = new WorkflowDTO();
      workflow.updateFromJson(jsonWf);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into CuneiformJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity.
    json.set(KEY_TYPE, JobType.YARN.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.wf = workflow;
  }

}
