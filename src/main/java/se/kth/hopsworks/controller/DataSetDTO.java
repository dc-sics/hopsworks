package se.kth.hopsworks.controller;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeam;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.users.UserCardDTO;

/**
 *
 * @author ermiasg
 */
@XmlRootElement
public class DataSetDTO {

  private Integer inodeId;
  private String name;
  private String description;
  private boolean isPublic;
  private boolean searchable;
  private boolean editable;
  private int template;
  private Integer projectId;
  private String projectName;
  private String templateName;
  private List<UserCardDTO> projectTeam;
  private List<String> sharedWith;

  public DataSetDTO() {
  }

  public DataSetDTO(String name, String description, boolean searchable,
          int template) {
    this.name = name;
    this.description = description;
    this.searchable = searchable;
    this.template = template;
  }

  public DataSetDTO(Dataset ds, Project project, List<String> sharedWith) {
    this.inodeId = ds.getInode().getId();
    this.name = ds.getInode().getInodePK().getName();
    this.description = ds.getDescription();
    this.projectName = project.getName();
    this.sharedWith = sharedWith;
    this.projectTeam = new ArrayList<>();
    this.isPublic = ds.isPublicDs();
    //this have to be done because project team contains too much info.
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      projectTeam.add(new UserCardDTO(member.getUser().getFname(), member.
              getUser().getLname(), member.getUser().getEmail()));
    }
  }

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public int getTemplate() {
    return this.template;
  }

  public void setTemplate(int template) {
    this.template = template;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public List<UserCardDTO> getMembers() {
    return projectTeam;
  }

  public void setMembers(List<UserCardDTO> members) {
    this.projectTeam = members;
  }

  public List<String> getSharedWith() {
    return sharedWith;
  }

  public void setSharedWith(List<String> sharedWith) {
    this.sharedWith = sharedWith;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public List<UserCardDTO> getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(List<UserCardDTO> projectTeam) {
    this.projectTeam = projectTeam;
  }

  public boolean isIsPublic() {
    return isPublic;
  }

  public void setIsPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  
  @Override
  public String toString() {
    return "DataSetDTO{" + "name=" + name + ", description=" + description
            + ", searchable=" + searchable + ", template=" + this.template + '}';
  }

}
