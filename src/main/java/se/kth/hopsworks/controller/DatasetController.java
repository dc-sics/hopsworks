package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.ValidationException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.meta.db.TemplateFacade;
import se.kth.meta.entity.Template;
import se.kth.meta.exception.DatabaseException;

/**
 * Contains business logic pertaining DataSet management.
 * <p>
 * @author stig
 */
@Stateless
public class DatasetController {

  @EJB
  private InodeFacade inodes;
  @EJB
  private FileOperations fileOps;
  @EJB
  private TemplateFacade templates;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ActivityFacade activityFacade;

  /**
   * Create a new DataSet. This is, a folder right under the project home
   * folder.
   * <p>
   * @param user The creating User. Cannot be null.
   * @param project The project under which to create the DataSet. Cannot be
   * null.
   * @param dataSetName The name of the DataSet being created. Cannot be null
   * and must satisfy the validity criteria for a folder name.
   * @param datasetDescription The description of the DataSet being created. Can
   * be null.
   * @param templateId The id of the metadata template to be associated with
   * this DataSet.
   * @throws NullPointerException If any of the given parameters is null.
   * @throws IllegalArgumentException If the given DataSetDTO contains invalid
   * folder names, or the folder already exists.
   * @throws IOException if the creation of the dataset failed.
   * @see FolderNameValidator.java
   */
  public void createDataset(User user, Project project, String dataSetName,
          String datasetDescription, int templateId)
          throws IOException {
    //Parameter checking.
    if (user == null) {
      throw new NullPointerException(
              "A valid user must be passed upon DataSet creation. Received null.");
    } else if (project == null) {
      throw new NullPointerException(
              "A valid project must be passed upon DataSet creation. Received null.");
    } else if (dataSetName == null) {
      throw new NullPointerException(
              "A valid DataSet name must be passed upon DataSet creation. Received null.");
    }
    try {
      FolderNameValidator.isValidName(dataSetName);
    } catch (ValidationException e) {
      throw new IllegalArgumentException("Invalid folder name for DataSet.", e);
    }
    //Logic
    boolean success;
    String dsPath = File.separator + Constants.DIR_ROOT + File.separator
            + project.getName();
    dsPath = dsPath + File.separator + dataSetName;
    Inode parent = inodes.getProjectRoot(project.getName());
    Inode ds = inodes.findByParentAndName(parent, dataSetName);

    if (ds != null) {
      throw new IllegalStateException(
              "Invalid folder name for DataSet creation. "
              + ResponseMessages.FOLDER_NAME_EXIST);
    }
    success = createFolder(dsPath, templateId);

    if (success) {
      try {
        ds = inodes.findByParentAndName(parent, dataSetName);
        Dataset newDS = new Dataset(ds, project);
        if (datasetDescription != null) {
          newDS.setDescription(datasetDescription);
        }
        datasetFacade.persistDataset(newDS);
        activityFacade.persistActivity(ActivityFacade.NEW_DATA, project, user);
      } catch (Exception e) {
        IOException failed = new IOException("Failed to create dataset at path "
                + dsPath + ".", e);
        try {
          fileOps.rmRecursive(dsPath);//if dataset persist fails rm ds folder.
          throw failed;
        } catch (IOException ex) {
          throw new IOException(
                  "Failed to clean up properly on dataset creation failure", ex);
        }
      }
    } else {
      throw new IOException("Could not create the directory at " + dsPath);
    }
  }

  /**
   * Create a directory under an existing DataSet.
   * <p>
   * @param project The project under which the directory is being created.
   * Cannot be null.
   * @param datasetName The name of the DataSet under which the folder is being
   * created. Must be an existing DataSet.
   * @param dsRelativePath The DataSet-relative path to be created. E.g. if the
   * full path is /Projects/projectA/datasetB/folder1/folder2/folder3, the
   * DataSetRelative path is /folder1/folder2/folder3. Must be a valid path; all
   * the folder names on it must be valid and it cannot be null.
   * @param templateId The id of the template to be associated with the newly
   * created directory.
   * @throws java.io.IOException If something goes wrong upon the creation of
   * the directory.
   * @throws IllegalArgumentException If:
   * <ul>
   * <li>Any of the folder names on the given path does not have a valid name or
   * </li>
   * <li>The dataset with given name does not exist or </li>
   * <li>Such a folder already exists. </li>
   * </ul>
   * @see FolderNameValidator.java
   * @throws NullPointerException If any of the non-null-allowed parameters is
   * null.
   */
  public void createSubDirectory(Project project, String datasetName,
          String dsRelativePath, int templateId) throws IOException {
    //Preliminary
    while(dsRelativePath.startsWith("/")){
      dsRelativePath = dsRelativePath.substring(1);
    }
    String[] relativePathArray = dsRelativePath.split(File.separator); //The array representing the DataSet-relative path
    String fullPath = "/" + Constants.DIR_ROOT + "/" + project.getName() + "/"
            + datasetName + "/" + dsRelativePath;
    //Parameter checking
    if (project == null) {
      throw new NullPointerException(
              "Cannot create a directory under a null project.");
    } else if (datasetName == null) {
      throw new NullPointerException(
              "Cannot create a directory under a null DataSet.");
    } else if (dsRelativePath == null) {
      throw new NullPointerException(
              "Cannot create a directory for a null path.");
    } else if (dsRelativePath.isEmpty()) {
      throw new IllegalArgumentException(
              "Cannot create a directory for an empty path.");
    } else if (datasetName.isEmpty()) {
      throw new IllegalArgumentException("Invalid dataset: emtpy name.");
    } else {
      //Check every folder name for validity.
      for (String s : relativePathArray) {
        try {
          FolderNameValidator.isValidName(s);
        } catch (ValidationException e) {
          throw new IllegalArgumentException("Invalid folder name on the path: "
                  + s + "Reason: " + e.getLocalizedMessage());
        }
      }
      //Check if the given dataset exists.
      Inode projectRoot = inodes.getProjectRoot(project.getName());
      if (inodes.findByParentAndName(projectRoot, datasetName) == null) {
        throw new IllegalArgumentException("DataSet does not exist: "
                + datasetName + " under " + project.getName());
      }
      //Check if the given folder already exists
      if (inodes.existsPath(fullPath)) {
        throw new IllegalArgumentException("The given path already exists.");
      }
    }
    //Now actually create the folder
    createFolder(fullPath, templateId);
  }

  /**
   * Creates a folder in HDFS at the given path, and associates a template with
   * that folder.
   * <p>
   * @param path The full HDFS path to the folder to be created (e.g.
   * /Projects/projectA/datasetB/folder1/folder2).
   * @param template The id of the template to be associated with the created
   * folder.
   * @return
   * @throws IOException
   */
  private boolean createFolder(String path, int template) throws IOException {
    boolean success = false;
    try {
      success = fileOps.mkDir(path);
      //The inode has been created in the file system
      if (success && template != 0) {
        //Get the newly created Inode.
        Inode created = inodes.getInodeAtPath(path);
        Template templ = templates.findByTemplateId(template);
        if (templ != null) {
          templ.getInodes().add(created);
          //persist the relationship table
          templates.updateTemplatesInodesMxN(templ);
        }
      }
    } catch (IOException ex) {
      throw new IOException("Could not create the directory at " + path, ex);
    } catch (DatabaseException e) {
      throw new IOException("Could not attach template to folder. ", e);
    }
    return success;
  }
}
