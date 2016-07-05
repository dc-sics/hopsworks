package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.ValidationException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.meta.db.InodeBasicMetadataFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.entity.InodeBasicMetadata;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.user.model.Users;

/**
 * Contains business logic pertaining DataSet management.
 * <p/>
 * @author stig
 */
@Stateless
public class DatasetController {

  private static final Logger logger = Logger.getLogger(DatasetController.class.
          getName());
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
  @EJB
  private InodeBasicMetadataFacade inodeBasicMetaFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfsSingleton;

  /**
   * Create a new DataSet. This is, a folder right under the project home
   * folder.
   * <p/>
   * @param user The creating Users. Cannot be null.
   * @param project The project under which to create the DataSet. Cannot be
   * null.
   * @param dataSetName The name of the DataSet being created. Cannot be null
   * and must satisfy the validity criteria for a folder name.
   * @param datasetDescription The description of the DataSet being created. Can
   * be null.
   * @param templateId The id of the metadata template to be associated with
   * this DataSet.
   * @param searchable Defines whether the dataset can be indexed or not (i.e.
   * whether it can be visible in the search results or not)
   * @param globallyVisible
   * @throws NullPointerException If any of the given parameters is null.
   * @throws IllegalArgumentException If the given DataSetDTO contains invalid
   * folder names, or the folder already exists.
   * @throws IOException if the creation of the dataset failed.
   * @see FolderNameValidator.java
   */
  public void createDataset(Users user, Project project, String dataSetName,
          String datasetDescription, int templateId, boolean searchable,
          boolean globallyVisible)
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
    String dsPath = File.separator + Settings.DIR_ROOT + File.separator
            + project.getName();
    dsPath = dsPath + File.separator + dataSetName;
    Inode parent = inodes.getProjectRoot(project.getName());
    Inode ds = inodes.findByParentAndName(parent, dataSetName);

    if (ds != null) {
      throw new IllegalStateException(
              "Invalid folder name for DataSet creation. "
              + ResponseMessages.FOLDER_NAME_EXIST);
    }
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    //Permission hdfs dfs -chmod 750 or 755
    FsAction global = (globallyVisible ? FsAction.READ_EXECUTE
            : FsAction.NONE);
    FsAction group = (globallyVisible ? FsAction.ALL
            : FsAction.READ_EXECUTE);
    FsPermission fsPermission = new FsPermission(FsAction.ALL,
            group, global, globallyVisible);
    success = createFolder(dsPath, templateId, username, fsPermission);

    if (success) {
      //set the dataset meta enabled. Support 3 level indexing
      this.fileOps.setMetaEnabled(dsPath);
      try {

        ds = inodes.findByParentAndName(parent, dataSetName);
        Dataset newDS = new Dataset(ds, project);
        newDS.setSearchable(searchable);

        if (datasetDescription != null) {
          newDS.setDescription(datasetDescription);
        }
        datasetFacade.persistDataset(newDS);
        activityFacade.persistActivity(ActivityFacade.NEW_DATA + dataSetName, project, user);
        // creates a dataset and adds user as owner.
        hdfsUsersBean.addDatasetUsersGroups(user, project, newDS);
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
   * Create a directory under an existing DataSet. With the same permission as
   * the parent.
   * <p/>
   * @param user creating the folder
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
   * @param description The description of the directory
   * @param searchable Defines if the directory can be searched upon
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
  public void createSubDirectory(Users user, Project project, String datasetName,
          String dsRelativePath, int templateId, String description,
          boolean searchable) throws IOException {

    //Preliminary
    while (dsRelativePath.startsWith("/")) {
      dsRelativePath = dsRelativePath.substring(1);
    }
    String[] relativePathArray = dsRelativePath.split(File.separator); //The array representing the DataSet-relative path
    String fullPath = "/" + Settings.DIR_ROOT + "/" + project.getName() + "/"
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
    }
    //Check if the given folder already exists
    if (inodes.existsPath(fullPath)) {
      throw new IllegalArgumentException("The given path already exists.");
    }

    //Check if the given dataset exists.
    Inode projectRoot = inodes.getProjectRoot(project.getName());
    
    String parentPath = fullPath;
    // strip any trailing '/' in the pathname
    while (parentPath != null && parentPath.length() > 0 && parentPath.charAt(parentPath.length()-1)=='/') {
      parentPath = parentPath.substring(0, parentPath.length()-1);
    }    
    // parent path prefixes the last '/' in the pathname
    parentPath = parentPath.substring(0, parentPath.lastIndexOf("/"));
    Inode parent = inodes.getInodeAtPath(parentPath);
    if (parent == null) {
      throw new IllegalArgumentException("Path for parent folder does not exist: "
              + parentPath + " under " + project.getName());
    }

    String username = hdfsUsersBean.getHdfsUserName(project, user);
    //Now actually create the folder
    boolean success = this.createFolder(fullPath, templateId, username, null);

    //if the folder was created successfully, persist basic metadata to it -
    //description and searchable attribute
    if (success) {
      //get the folder name. The last part of fullPath
      String pathParts[] = fullPath.split("/");
      String folderName = pathParts[pathParts.length - 1];

      //find the corresponding inode
      Inode folder = this.inodes.findByParentAndName(parent, folderName);
      InodeBasicMetadata basicMeta = new InodeBasicMetadata(folder, description,
              searchable);
      this.inodeBasicMetaFacade.addBasicMetadata(basicMeta);
    }
  }

  /**
   * Deletes a folder recursively as the given user.
   * <p>
   * @param path
   * @param user
   * @param project
   * @return
   * @throws java.io.IOException
   */
  public boolean deleteDataset(String path, Users user, Project project) throws
          IOException {
    boolean success;
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    Path location = new Path(path);
    success = dfsSingleton.getDfsOps(username).rm(location, true);
    return success;
  }

  /**
   * Change permission of the folder in path. This is performed with the
   * username
   * of the user in the given project.
   * <p>
   * @param path
   * @param user
   * @param project
   * @param pemission
   * @throws IOException
   */
  public void changePermission(String path, Users user, Project project,
          FsPermission pemission) throws IOException {
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    Path location = new Path(path);
    dfsSingleton.getDfsOps(username).setPermission(location, pemission);
  }

  /**
   * Creates a folder in HDFS at the given path, and associates a template with
   * that folder.
   * <p/>
   * @param path The full HDFS path to the folder to be created (e.g.
   * /Projects/projectA/datasetB/folder1/folder2).
   * @param template The id of the template to be associated with the created
   * folder.
   * @return
   * @throws IOException
   */
  private boolean createFolder(String path, int template, String username,
          FsPermission fsPermission) throws IOException {
    boolean success = false;
    Path location = new Path(path);
    DistributedFileSystemOps dfs;
    if (fsPermission == null) {
      fsPermission = dfsSingleton.getDfsOps().getParentPermission(location);
    }
    try {
      //create the folder in the file system
      if (username == null) {
        dfs = dfsSingleton.getDfsOps();
      } else {
        dfs = dfsSingleton.getDfsOps(username);
      }
      success = dfs.mkdir(location, fsPermission);
      if (success) {
        dfs.setPermission(location, fsPermission);
      }
      if (success && template != 0 && template != -1) {
        //Get the newly created Inode.
        Inode created = inodes.getInodeAtPath(path);
        Template templ = templates.findByTemplateId(template);
        if (templ != null) {
          templ.getInodes().add(created);
          //persist the relationship table
          templates.updateTemplatesInodesMxN(templ);
        }
      }
    } catch (AccessControlException ex) {
      throw new AccessControlException(ex);
    } catch (IOException ex) {
      throw new IOException("Could not create the directory at " + path, ex);
    } catch (DatabaseException e) {
      throw new IOException("Could not attach template to folder. ", e);
    }
    return success;
  }
}
