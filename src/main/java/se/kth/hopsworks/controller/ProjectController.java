package se.kth.hopsworks.controller;

import io.hops.bbc.ConsentStatus;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.*;
import javax.ws.rs.core.Response;

import io.hops.bbc.ProjectPaymentAction;
import java.io.FilenameFilter;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectPaymentsHistory;
import se.kth.bbc.project.ProjectPaymentsHistoryFacade;
import se.kth.bbc.project.ProjectPaymentsHistoryPK;
import se.kth.bbc.project.ProjectRoleTypes;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.ProjectTeamPK;
import se.kth.bbc.jobs.quota.YarnProjectsQuota;
import se.kth.bbc.jobs.quota.YarnProjectsQuotaFacade;
import se.kth.bbc.jobs.quota.YarnRunningPrice;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServiceFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.certificates.UserCertsFacade;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfs.fileoperations.HdfsInodeAttributes;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.ProjectInternalFoldersFailedException;
import se.kth.hopsworks.user.model.SshKeys;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.SshkeysFacade;
import se.kth.hopsworks.util.LocalhostServices;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private final static Logger logger = Logger.getLogger(ProjectController.class.
    getName());
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectPaymentsHistoryFacade projectPaymentsHistoryFacade;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private ProjectServiceFacade projectServicesFacade;
  @EJB
  private InodeFacade inodes;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private SshkeysFacade sshKeysBean;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private Settings settings;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserCertsFacade userCertsFacade;
  @EJB
  private DistributedFsService dfs;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  /**
   * Creates a new project(project), the related DIR, the different services in the project, and the master of the
   * project.
   * <p>
   * This needs to be an atomic operation (all or nothing) REQUIRES_NEW will make sure a new transaction is created even
   * if this method is called from within a transaction.
   * <p/>
   * @param newProject
   * @param email
   * @param dfso
   * @return
   * @throws IllegalArgumentException if the project name already exists.
   * @throws se.kth.hopsworks.rest.AppException
   * @throws IOException if the DIR associated with the project could not be created. For whatever reason.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Project createProject(ProjectDTO newProject, String email,
    DistributedFileSystemOps dfso) throws
    IOException, AppException {
    Users user = userBean.getUserByEmail(email);

    if (!FolderNameValidator.isValidName(newProject.getProjectName())) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.INVALID_PROJECT_NAME);
    } else if (projectFacade.numProjectsLimitReached(user)) {
      logger.log(Level.SEVERE,
        "You have reached the maximum number of allowed projects.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.NUM_PROJECTS_LIMIT_REACHED);
    } else if (projectFacade.projectExists(newProject.getProjectName())) {
      logger.log(Level.INFO, "Project with name {0} already exists!",
        newProject.getProjectName());
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.PROJECT_EXISTS);
    } else if (dfso.exists(File.separator + settings.DIR_ROOT
      + File.separator + newProject.getProjectName())) {
      logger.log(Level.WARNING, "Project with name {0} already exists in hdfs. "
        + "Possible inconsistency! project name not in database.",
        newProject.getProjectName());
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.PROJECT_EXISTS);
    } else { // create the project!


      /*
       * first create the folder structure in hdfs. If it is successful move on
       * to create the project in hopsworks database
       */
      String projectPath = mkProjectDIR(newProject.getProjectName(), dfso);
      if (projectPath != null) {

        //Create a new project object
        Date now = new Date();
        Project project = new Project(newProject.getProjectName(), user, now);
        project.setDescription(newProject.getDescription());

        // make ethical status pending
        project.setEthicalStatus(ConsentStatus.PENDING.name());

        // set retention period to next 10 years by default
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, 10);
        project.setRetentionPeriod(cal.getTime());

        Inode projectInode = this.inodes.getProjectRoot(project.getName());
        if (projectInode == null) {
          // delete the project if there's an error/
          DistributedFileSystemOps udfso = dfs.getDfsOps(project.getOwner().getUsername());
          try {
            removeByID(project.getId(), project.getOwner().getEmail(), true, udfso);
          } catch (IOException | AppException t) {
            // do nothing
          } finally {
            udfso.close();
          }
          logger.log(Level.SEVERE, "Couldn't get Inode for the project: {0}", project.getName());
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            ResponseMessages.INTERNAL_SERVER_ERROR);
        }
        project.setInode(projectInode);

        //Persist project object
        this.projectFacade.persistProject(project);
        this.projectFacade.flushEm();
        this.projectPaymentsHistoryFacade.persistProjectPaymentsHistory(
          new ProjectPaymentsHistory(new ProjectPaymentsHistoryPK(project
            .getName(), project.getCreated()), project.
            getOwner().getEmail(),
            ProjectPaymentAction.DEPOSIT_MONEY, 0));
        this.projectPaymentsHistoryFacade.flushEm();
        this.yarnProjectsQuotaFacade.persistYarnProjectsQuota(
          new YarnProjectsQuota(project.getName(), Integer.parseInt(
            settings
            .getYarnDefaultQuota()), 0));
        this.yarnProjectsQuotaFacade.flushEm();
        //Add the activity information
        logActivity(ActivityFacade.NEW_PROJECT + project.getName(),
          ActivityFacade.FLAG_PROJECT, user, project);
        //update role information in project
        addProjectOwner(project.getId(), user.getEmail());
        logger.log(Level.FINE, "{0} - project created successfully.", project.
          getName());

        //Create default DataSets
        return project;
      }
    }
    return null;
  }

  /**
   * Project default datasets Logs and Resources need to be created in a separate transaction after the project creation
   * is complete.
   * <p/>
   * @param username
   * @param project
   * @param dfso
   * @param udfso
   * @throws ProjectInternalFoldersFailedException
   * @throws se.kth.hopsworks.rest.AppException
   */
  public void createProjectLogResources(String username, Project project,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws
    ProjectInternalFoldersFailedException, AppException {

    Users user = userBean.getUserByEmail(username);

    try {
      for (Settings.DefaultDataset ds : Settings.DefaultDataset.values()) {
        boolean globallyVisible = (ds.equals(Settings.DefaultDataset.RESOURCES)
          || ds.equals(Settings.DefaultDataset.LOGS));
        datasetController.createDataset(user, project, ds.getName(), ds.
          getDescription(), -1, false, globallyVisible, dfso, udfso);
      }
    } catch (IOException | EJBException e) {
      throw new ProjectInternalFoldersFailedException(
        "Could not create project resources ", e);
    }
  }

  public void createProjectConsentFolder(String username, Project project,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso)
    throws
    ProjectInternalFoldersFailedException, AppException {

    Users user = userBean.getUserByEmail(username);

    try {
      datasetController.createDataset(user, project, "consents",
        "Biobanking consent forms", -1, false, false, dfso, udfso);
    } catch (IOException | EJBException e) {
      throw new ProjectInternalFoldersFailedException(
        "Could not create project consents folder ", e);
    }
  }

  /**
   * Returns a Project
   * <p/>
   *
   * @param id the identifier for a Project
   * @return Project
   * @throws se.kth.hopsworks.rest.AppException if the project could not be found.
   */
  public Project findProjectById(Integer id) throws AppException {

    Project project = projectFacade.find(id);
    if (project != null) {
      return project;
    } else {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
        ResponseMessages.PROJECT_NOT_FOUND);
    }
  }

  public boolean addServices(Project project, List<ProjectServiceEnum> services,
    String userEmail) {
    boolean addedService = false;
    //Add the desired services
    boolean sshAdded = false;
    for (ProjectServiceEnum se : services) {
      if (!projectServicesFacade.findEnabledServicesForProject(project).
        contains(se)) {
        projectServicesFacade.addServiceForProject(project, se);
        addedService = true;
        if (se == ProjectServiceEnum.SSH) {
          sshAdded = true;
        }
      }
    }

    if (addedService) {
      Users user = userBean.getUserByEmail(userEmail);
      String servicesString = "";
      for (int i = 0; i < services.size(); i++) {
        servicesString = servicesString + services.get(i).name() + " ";
      }
      logActivity(ActivityFacade.ADDED_SERVICES + servicesString,
        ActivityFacade.FLAG_PROJECT,
        user, project);
//      if (sshAdded == true) {
//        try {
//
//          // For all members of the project, create an account for them and copy their public keys to ~/.ssh/authorized_keys
//          List<ProjectTeam> members = projectTeamFacade.findMembersByProject(
//                  project);
//          for (ProjectTeam pt : members) {
//            Users myUser = pt.getUser();
//            List<SshKeys> keys = sshKeysBean.findAllById(myUser.getUid());
//            List<String> publicKeys = new ArrayList<>();
//            for (SshKeys k : keys) {
//              publicKeys.add(k.getPublicKey());
//            }
//            LocalhostServices.createUserAccount(myUser.getUsername(), project.
//                    getName(), publicKeys);
//          }
//
//        } catch (IOException e) {
//          // TODO - propagate exception?
//          logger.warning("Could not create user account: " + e.getMessage());
//        }
//      }

    }
    return addedService;
  }

  /**
   * Change the project description
   * <p/>
   *
   * @param project
   * @param proj
   * @param userEmail of the user making the change
   */
  public void updateProject(Project project, ProjectDTO proj,
    String userEmail) {
    Users user = userBean.getUserByEmail(userEmail);

    project.setDescription(proj.getDescription());
    project.setRetentionPeriod(proj.getRetentionPeriod());

    projectFacade.mergeProject(project);
    logActivity(ActivityFacade.PROJECT_DESC_CHANGED, ActivityFacade.FLAG_PROJECT,
      user, project);
  }

  //Set the project owner as project master in ProjectTeam table
  private void addProjectOwner(Integer project_id, String userName) {
    ProjectTeamPK stp = new ProjectTeamPK(project_id, userName);
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getTeam());
    st.setTimestamp(new Date());
    projectTeamFacade.persistProjectTeam(st);
  }

  //create project in HDFS
  private String mkProjectDIR(String projectName, DistributedFileSystemOps dfso)
    throws IOException {

    String rootDir = settings.DIR_ROOT;

    boolean rootDirCreated = false;
    boolean projectDirCreated = false;
    boolean childDirCreated = false;

    if (!dfso.isDir(rootDir)) {
      /*
       * if the base path does not exist in the file system, create it first
       * and set it metaEnabled so that other folders down the dir tree
       * are getting registered in hdfs_metadata_log table
       */
      Path location = new Path(File.separator + rootDir);
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
        FsAction.ALL); // permission 777 so any one can creat a project.
      rootDirCreated = dfso.mkdir(location, fsPermission);
    } else {
      rootDirCreated = true;
    }

    /*
     * Marking a project path as meta enabled means that all child folders/files
     * that'll be created down this directory tree will have as a parent this
     * inode.
     */
    String fullProjectPath = File.separator + rootDir + File.separator
      + projectName;
    String project = this.extractProjectName(fullProjectPath + File.separator);
    String projectPath = File.separator + rootDir + File.separator + project;
    //Create first the projectPath
    projectDirCreated = dfso.mkdir(projectPath); //fails here

    ProjectController.this.setHdfsSpaceQuotaInMBs(projectName, settings.
      getHdfsDefaultQuotaInMBs(), dfso);

    //create the rest of the child folders if any
    if (projectDirCreated && !fullProjectPath.equals(projectPath)) {
      childDirCreated = dfso.mkdir(fullProjectPath);
    } else if (projectDirCreated) {
      childDirCreated = true;
    }

    if (rootDirCreated && projectDirCreated && childDirCreated) {
      return projectPath;
    }
    return null;
  }

  /**
   * Remove a project and optionally all associated files.
   *
   * @param projectID to be removed
   * @param email
   * @param deleteFilesOnRemove if the associated files should be deleted
   * @return true if the project and the associated files are removed successfully, and false if the associated files
   * could not be removed.
   * @throws IOException if the hole operation failed. i.e the project is not removed.
   * @throws AppException if the project could not be found.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public boolean removeByID(Integer projectID, String email,
    boolean deleteFilesOnRemove, DistributedFileSystemOps udfso) throws
    IOException, AppException {
    boolean success = !deleteFilesOnRemove;

    Project project = projectFacade.find(projectID);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.PROJECT_NOT_FOUND);
    }

    ProjectPaymentsHistory projectPaymentsHistory
      = projectPaymentsHistoryFacade.findByProjectName(project.getName());
    YarnProjectsQuota yarnProjectsQuota = yarnProjectsQuotaFacade.
      findByProjectName(project.getName());
    List<Dataset> dsInProject = datasetFacade.findByProject(project);
    Collection<ProjectTeam> projectTeam = projectTeamFacade.
      findMembersByProject(project);
    //if we remove the project we cant store activity that has a reference to it!!
    //logActivity(ActivityFacade.REMOVED_PROJECT,
    //ActivityFacade.FLAG_PROJECT, user, project);
    if (deleteFilesOnRemove) {
      String path = File.separator + settings.DIR_ROOT + File.separator
        + project.getName();
      Path location = new Path(path);
      success = udfso.rm(location, true);
      //if the files are removed the group should also go.
      if (success) {
        hdfsUsersBean.deleteProjectGroupsRecursive(project, dsInProject);
        hdfsUsersBean.deleteProjectUsers(project, projectTeam);
        zeppelinConfFactory.deleteZeppelinConfDir(project.getName());
        //projectPaymentsHistoryFacade.remove(projectPaymentsHistory);
        yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
      }
    } else {
      projectFacade.remove(project);
      //projectPaymentsHistoryFacade.remove(projectPaymentsHistory);
      yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
    }

    // TODO: DELETE THE KAFKA TOPICS
    userCertsFacade.removeAllCertsOfAProject(project.getName());

    LocalhostServices.deleteProjectCertificates(settings.getIntermediateCaDir(), project.getName());
    logger.log(Level.INFO, "{0} - project removed.", project.getName());

    return success;
  }

  /**
   * Adds new team members to a project(project) - bulk persist if team role not specified or not in (Data owner or Data
   * scientist)defaults to Data scientist
   * <p/>
   *
   * @param project
   * @param email
   * @param projectTeams
   * @return a list of user names that could not be added to the project team list.
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, String email,
    List<ProjectTeam> projectTeams) {
    List<String> failedList = new ArrayList<>();
    Users user = userBean.getUserByEmail(email);
    Users newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(user.
          getEmail())) {

          //if the role is not properly set set it to the default resercher.
          if (projectTeam.getTeamRole() == null || (!projectTeam.getTeamRole().
            equals(ProjectRoleTypes.DATA_SCIENTIST.getTeam())
            && !projectTeam.
            getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.getTeam()))) {
            projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getTeam());
          }

          projectTeam.setTimestamp(new Date());
          newMember = userBean.getUserByEmail(projectTeam.getProjectTeamPK().
            getTeamMember());
          if (newMember != null && !projectTeamFacade.isUserMemberOfProject(
            project, newMember)) {
            //this makes sure that the member is added to the project sent as the
            //first param b/c the securty check was made on the parameter sent as path.
            projectTeam.getProjectTeamPK().setProjectId(project.getId());
            projectTeamFacade.persistProjectTeam(projectTeam);
            try {
              hdfsUsersBean.addNewProjectMember(project, projectTeam);
            } catch (IOException ex) {
              projectTeamFacade.removeProjectTeam(project, newMember);
              throw new EJBException("Could not add member to HDFS.");
            }
            LocalhostServices.createUserCertificates(settings.getIntermediateCaDir(),
              project.getName(), newMember.getUsername());

            userCertsFacade.putUserCerts(project.getName(), newMember.
              getUsername());

            logger.log(Level.FINE, "{0} - member added to project : {1}.",
              new Object[]{newMember.getEmail(),
                project.getName()});
            List<SshKeys> keys = sshKeysBean.findAllById(newMember.getUid());
            List<String> publicKeys = new ArrayList<>();
            for (SshKeys k : keys) {
              publicKeys.add(k.getPublicKey());
            }

            logActivity(ActivityFacade.NEW_MEMBER + projectTeam.
              getProjectTeamPK().getTeamMember(),
              ActivityFacade.FLAG_PROJECT, user, project);
//            createUserAccount(project, projectTeam, publicKeys, failedList);
          } else if (newMember == null) {
            failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
              + " was not found in the system.");
          } else {
            failedList.add(newMember.getEmail()
              + " is already a member in this project.");
          }

        } else {
          failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
            + " is already a member in this project.");
        }
      } catch (EJBException ejb) {
        failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
          + "could not be added. Try again later.");
        logger.log(Level.SEVERE, "Adding  team member {0} to members failed",
          projectTeam.getProjectTeamPK().getTeamMember());
      } catch (IOException ex) {
        Logger.getLogger(ProjectController.class.getName()).log(Level.SEVERE,
          null, ex);
      }
    }
    return failedList;
  }

  // Create Account for user on localhost if the SSH service is enabled
//  private void createUserAccount(Project project, ProjectTeam projectTeam,
//          List<String> publicKeys, List<String> failedList) {
//    for (ProjectServices ps : project.getProjectServicesCollection()) {
//      if (ps.getProjectServicesPK().getService().compareTo(
//              ProjectServiceEnum.SSH) == 0) {
//        try {
//          String email = projectTeam.getProjectTeamPK().getTeamMember();
//          Users user = userBean.getUserByEmail(email);
//          LocalhostServices.createUserAccount(user.getUsername(), project.
//                  getName(), publicKeys);
//        } catch (IOException e) {
//          failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
//                  + "could not create the account on localhost. Try again later.");
//          logger.log(Level.SEVERE,
//                  "Create account on localhost for team member {0} failed",
//                  projectTeam.getProjectTeamPK().getTeamMember());
//        }
//      }
//    }
//  }
  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param projectID of the project
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getProjectByID(Integer projectID) throws AppException {
    Project project = projectFacade.find(projectID);
    String name = project.getName();

    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(File.separator + settings.DIR_ROOT
      + File.separator + name);

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
      project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
      findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    return new ProjectDTO(project, inode.getId(), services, projectTeam,
      getYarnQuota(name));
//    ,getHdfsSpaceQuotaInBytes(name), getHdfsSpaceUsageInBytes(name));
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param name
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getProjectByName(String name) throws AppException {
    //find the project entity from hopsworks database
    Project project = projectFacade.findByName(name);

    //find the project as an inode from hops database
    String path = File.separator + settings.DIR_ROOT + File.separator + name;
    Inode inode = inodes.getInodeAtPath(path);

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
      project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
      findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    Inode parent;
    List<InodeView> kids = new ArrayList<>();

    Collection<Dataset> dsInProject = project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      kids.add(new InodeView(parent, ds, inodes.getPath(ds.getInode())));
    }

    //send the project back to client
    String quota = getYarnQuota(name);
    return new ProjectDTO(project, inode.getId(), services, projectTeam, kids,
      quota);
  }

  public String getYarnQuota(String name) {
    YarnProjectsQuota yarnQuota = yarnProjectsQuotaFacade.
      findByProjectName(name);
    if (yarnQuota != null) {
      return Float.toString(yarnQuota.getQuotaRemaining());
    }
    return "";
  }

  public void setHdfsSpaceQuotaInMBs(String projectname, long diskspaceQuotaInMB,
    DistributedFileSystemOps dfso)
    throws IOException {
    dfso.setHdfsSpaceQuotaInMBs(new Path(settings.getProjectPath(projectname)),
      diskspaceQuotaInMB);
  }

//  public Long getHdfsSpaceQuotaInBytes(String name) throws AppException {
//    String path = settings.getProjectPath(name);
//    try {
//      long quota = dfs.getDfsOps().getHdfsSpaceQuotaInMbs(new Path(path));
//      logger.log(Level.INFO, "HDFS Quota for {0} is {1}", new Object[]{path, quota});
//      return quota;
//    } catch (IOException ex) {
//      logger.severe(ex.getMessage());
//      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
//          ". Cannot find quota for the project: " + path);
//    }
  public HdfsInodeAttributes getHdfsQuotas(int inodeId) throws AppException {

    HdfsInodeAttributes res = em.find(HdfsInodeAttributes.class, inodeId);
    if (res == null) {
      return new HdfsInodeAttributes(inodeId);
    }

    return res;
  }

//  public Long getHdfsSpaceUsageInBytes(String name) throws AppException {
//    String path = settings.getProjectPath(name);
//
//    try {
//      long usedQuota = dfs.getDfsOps().getUsedQuotaInMbs(new Path(path));
//      logger.log(Level.INFO, "HDFS Quota for {0} is {1}", new Object[]{path, usedQuota});
//      return usedQuota;
//    } catch (IOException ex) {
//      logger.severe(ex.getMessage());
//      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
//          ". Cannot find quota for the project: " + path);
//    }
//  }  
  /**
   * Deletes a member from a project
   *
   * @param project
   * @param email
   * @param toRemoveEmail
   * @throws AppException
   */
  public void deleteMemberFromTeam(Project project, String email,
    String toRemoveEmail) throws AppException {
    Users userToBeRemoved = userBean.getUserByEmail(toRemoveEmail);
    if (userToBeRemoved == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project,
      userToBeRemoved);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.TEAM_MEMBER_NOT_FOUND);
    }
    projectTeamFacade.removeProjectTeam(project, userToBeRemoved);
    Users user = userBean.getUserByEmail(email);
    //remove the user name from HDFS
    hdfsUsersBean.removeProjectMember(projectTeam.getUser(), project);
    logActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail,
      ActivityFacade.FLAG_PROJECT, user, project);

//    try {
//      LocalhostServices.deleteUserAccount(email, project.getName());
//    } catch (IOException e) {
//      String errMsg = "Could not delete user account: " + LocalhostServices.
//              getUsernameInProject(email, project.getName()) + " ";
//      logger.warning(errMsg + e.getMessage());
//      //  TODO: Should this be rethrown to give a HTTP Response to client??
//    }
  }

  /**
   * Updates the role of a member
   * <p/>
   *
   * @param project
   * @param owner that is performing the update
   * @param toUpdateEmail
   * @param newRole
   * @throws AppException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateMemberRole(Project project, String owner,
    String toUpdateEmail, String newRole) throws AppException {
    Users projOwner = project.getOwner();
    Users opsOwner = userBean.getUserByEmail(owner);
    Users user = userBean.getUserByEmail(toUpdateEmail);
    if (projOwner.equals(user)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        "Can not change the role of a project owner.");
    }
    if (user == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, user);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        ResponseMessages.TEAM_MEMBER_NOT_FOUND);
      //member not found
    }
    if (!projectTeam.getTeamRole().equals(newRole)) {
      projectTeam.setTeamRole(newRole);
      projectTeam.setTimestamp(new Date());
      projectTeamFacade.update(projectTeam);

      if (newRole.equals(AllowedRoles.DATA_OWNER)) {
        hdfsUsersBean.addUserToProjectGroup(project, projectTeam);
      } else {
        hdfsUsersBean.modifyProjectMembership(user, project);
      }

      logActivity(ActivityFacade.CHANGE_ROLE + toUpdateEmail,
        ActivityFacade.FLAG_PROJECT, opsOwner, project);
    }

  }

  /**
   * Retrieves all the project teams that a user have a role
   * <p/>
   *
   * @param email of the user
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectByUser(String email) {
    Users user = userBean.getUserByEmail(email);
    return projectTeamFacade.findByMember(user);
  }

  /**
   * Retrieves all the project teams for a project
   * <p/>
   *
   * @param projectID
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectTeamById(Integer projectID) {
    Project project = projectFacade.find(projectID);
    return projectTeamFacade.findMembersByProject(project);
  }

  /**
   * Logs activity
   * <p/>
   *
   * @param activityPerformed the description of the operation performed
   * @param flag on what the operation was performed(FLAG_PROJECT, FLAG_USER)
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   */
  public void logActivity(String activityPerformed, String flag,
    Users performedBy, Project performedOn) {
    Date now = new Date();
    Activity activity = new Activity();
    activity.setActivity(activityPerformed);
    activity.setFlag(flag);
    activity.setProject(performedOn);
    activity.setTimestamp(now);
    activity.setUser(performedBy);

    activityFacade.persistActivity(activity);
  }

  /**
   * Extracts the project name out of the given path. The project name is the second part of this path.
   * <p/>
   * @param path
   * @return
   */
  private String extractProjectName(String path) {

    int startIndex = path.indexOf('/', 1);
    int endIndex = path.indexOf('/', startIndex + 1);

    return path.substring(startIndex + 1, endIndex);
  }

  public void addExampleJarToExampleProject(String username, Project project,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws AppException {

    Users user = userBean.getUserByEmail(username);
    try {
      datasetController.createDataset(user, project, "TestJob",
        "jar file to calculate pi", -1, false, true, dfso, udfso);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    String exampleDir = settings.getSparkDir() + Settings.SPARK_EXAMPLES_DIR + "/";
    try {
      File dir = new File(exampleDir);
      File[] file = dir.listFiles((File dir1, String name)
        -> name.matches("spark-examples(.*).jar"));
      if (file.length == 0) {
        throw new IllegalStateException("No spark-examples*.jar was found in "
          + dir.getAbsolutePath());
      }
      if (file.length > 1) {
        logger.log(Level.WARNING, "More than one spark-examples*.jar found in {0}.", dir.getAbsolutePath());
      }
      udfso.copyToHDFSFromLocal(false, file[0].getAbsolutePath(),
        File.separator + Settings.DIR_ROOT + File.separator + project.
        getName() + "/TestJob/spark-examples.jar");

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }

  }

  public YarnRunningPrice getYarnPrice() {
    YarnRunningPrice price = yarnProjectsQuotaFacade.getPrice();
    if (price == null) {
      price = new YarnRunningPrice();
      price.setPrice(Settings.DEFAULT_YARN_PRICE);
      price.setTime(System.currentTimeMillis());
      price.setId("-1");
    }
    return price;
  }

}
