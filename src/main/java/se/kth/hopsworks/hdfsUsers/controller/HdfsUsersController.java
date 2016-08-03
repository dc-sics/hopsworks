package se.kth.hopsworks.hdfsUsers.controller;

import se.kth.hopsworks.hdfsUsers.model.HdfsGroups;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.hdfsUsers.HdfsUsersFacade;
import se.kth.hopsworks.user.model.Users;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfsUsers.HdfsGroupsFacade;
import se.kth.hopsworks.hdfsUsers.model.HdfsUsers;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.Settings;

@Stateless
public class HdfsUsersController {

    private static final Logger logger = Logger.getLogger(
            HdfsUsersController.class.
            getName());
    public static final String USER_NAME_DELIMITER = "__";

    @EJB
    private HdfsUsersFacade hdfsUsersFacade;
    @EJB
    private HdfsGroupsFacade hdfsGroupsFacade;
    @EJB
    private DistributedFsService dfsService;
    @EJB
    private InodeFacade inodes;
    @EJB
    private Settings settings;
    @EJB
    private UserFacade userFacade;
    @EJB
    private DatasetFacade datasetFacade;
    @EJB
    private ProjectTeamFacade projectTeamFacade;

    /**
     * Creates a new group in HDFS with the name <code>projectName</code> if it
     * does not exist, then creates the owner in HDFS with the name
     * <code>projectName</code>__<code>username</code> , also if it does not
     * exist, and gets added to the group <code>projectName</code>.
     * <p>
     * @param project
   * @param dfso
     * @throws java.io.IOException
     */
  public void addProjectFolderOwner(Project project,
          DistributedFileSystemOps dfso) throws IOException {
    String owner = getHdfsUserName(project, project.getOwner());
    String projectPath = File.separator + settings.DIR_ROOT + File.separator
            + project.getName();
    Path location = new Path(projectPath);
    //FsPermission(FsAction u, FsAction g, FsAction o) 775
    //Gives owner and group all access and read, execute for others
    //This means group is for data_owners and others for data_scientist
    //This means every body can see the content of a project.
    FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
            FsAction.READ_EXECUTE);// 775
    dfso.setOwner(location, owner, project.getName());
    dfso.setPermission(location, fsPermission);
  }

    /**
     * Adds a new member to the project. This will create a new user in HDFS
     * with the name <code>projectName</code>__<code>username</code> and adds it
     * to the group <code>projectName</code>. throws IllegalArgumentException if
     * the project group is not found.
     * <p>
     * @param project
     * @param member
     * @throws java.io.IOException
     */
    public void addNewProjectMember(Project project, ProjectTeam member) throws
            IOException {
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
        if (hdfsGroup == null) {
            throw new IllegalArgumentException("No group found for project in HDFS.");
        }
        String hdfsUsername;
        HdfsUsers memberHdfsUser;
        Users newMember = userFacade.findByEmail(member.getProjectTeamPK().
                getTeamMember());
        hdfsUsername = getHdfsUserName(project, newMember);
        memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
        if (memberHdfsUser == null) {
            memberHdfsUser = new HdfsUsers(hdfsUsername);
            hdfsUsersFacade.persist(memberHdfsUser);
        }
        if (memberHdfsUser.getHdfsGroupsCollection() == null) {
            memberHdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
        }
        //add only data_owners to project group
        if (member.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
            if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
                memberHdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
            }
        }
        byte[] dsGroupId;
        String dsGroups;
        HdfsGroups hdfsDsGroup;
        // add the member to all dataset groups in the project.
        List<Dataset> dsInProject = datasetFacade.findByProject(project);
        for (Dataset ds : dsInProject) {
            dsGroups = getHdfsGroupName(ds);
            hdfsDsGroup = hdfsGroupsFacade.findByName(dsGroups);
            if (hdfsDsGroup != null) {
                if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsDsGroup)) {
                    memberHdfsUser.getHdfsGroupsCollection().add(hdfsDsGroup);
                }
            }
        }
        hdfsUsersFacade.merge(memberHdfsUser);
    }

    /**
     * Adds a user to project group if the member have a Data owner role in the
     * project.
     * <p>
     * throws IllegalArgumentException if the project group is not found.
     * <p>
     * @param project
     * @param member
     */
    public void addUserToProjectGroup(Project project, ProjectTeam member) {
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
        if (hdfsGroup == null) {
            throw new IllegalArgumentException("No group found for project in HDFS.");
        }
        String hdfsUsername;
        HdfsUsers memberHdfsUser;
        Users newMember = userFacade.findByEmail(member.getProjectTeamPK().
                getTeamMember());
        hdfsUsername = getHdfsUserName(project, newMember);
        memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
        if (memberHdfsUser == null) {
            memberHdfsUser = new HdfsUsers(hdfsUsername);
            hdfsUsersFacade.persist(memberHdfsUser);
        }
        if (memberHdfsUser.getHdfsGroupsCollection() == null) {
            memberHdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
        }
        //add only data_owners to project group
        if (member.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
            if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
                memberHdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
            }
        }
    }

    /**
     * Create a new group in HDFS with the name project.name__datasetName if it
     * does not exist, then adds all members of the project to this group. This
     * is done when a new dataset is created in a project. If stickyBit is set
     * true: all members of the project will be given r, w, x privileges. If
     * stickyBit is set false: user will get all privileges, and all other
     * members will have r and x privileges.
     * <p>
     * @param owner
     * @param project
     * @param dataset
     * @throws java.io.IOException
     */
    public void addDatasetUsersGroups(Users owner, Project project,
            Dataset dataset, DistributedFileSystemOps dfso) throws IOException {
        if (owner == null || project == null || project.getProjectTeamCollection()
                == null || dataset == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String datasetGroup = getHdfsGroupName(project, dataset);
        String dsOwner = getHdfsUserName(project, owner);
        String dsPath = File.separator + settings.DIR_ROOT + File.separator
                + project.getName() + File.separator + dataset.getInode().
                getInodePK().getName();
        Path location = new Path(dsPath);
        dfso.setOwner(location, dsOwner, datasetGroup);

        String hdfsUsername;
        HdfsUsers hdfsUser;
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
        if (hdfsGroup == null) {
            throw new IllegalArgumentException(
                    "Could not create dataset group in HDFS.");
        }
        if (hdfsGroup.getHdfsUsersCollection() == null) {
            hdfsGroup.setHdfsUsersCollection(new ArrayList<HdfsUsers>());
        }
        /**
         * ****** add project name as a user ********
         */
        hdfsUser = hdfsUsersFacade.findByName(project.getName());
        if (hdfsUser == null) {
            hdfsUser = new HdfsUsers(project.getName());
            hdfsUsersFacade.persist(hdfsUser);
        }
        if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
            hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
        }
        /**
         * ****** **************************** ********
         */
        //add every member to the new ds group
        for (ProjectTeam member : project.getProjectTeamCollection()) {
            hdfsUsername = getHdfsUserName(project, member.getUser());
            hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
            //the owner does not need to be added to the group.
            if (hdfsUsername.equals(dsOwner)) {
                continue;
            }
            if (hdfsUser == null) {
                hdfsUser = new HdfsUsers(hdfsUsername);
                hdfsUsersFacade.persist(hdfsUser);
            }
            if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
                hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
            }
        }
        hdfsGroupsFacade.merge(hdfsGroup);
    }

    /**
     * Removes the user project__username. This should cascade to the groups the
     * user is a member of. This can be used to remove a data_owner or a
     * data_scientist from project.
     * <p>
     * @param user
     * @param project
     */
    public void removeProjectMember(Users user, Project project) {
        if (user == null || project == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String userName = getHdfsUserName(project, user);
        HdfsUsers hdfsUser = hdfsUsersFacade.findByName(userName);
        dfsService.removeDfsOps(userName);
        if (hdfsUser != null) {
            hdfsUsersFacade.removeHdfsUser(hdfsUser);
        }
    }

    /**
     * Removes the user project__username from the group projectName. This means
     * the user is no longer a data_owner in this project. (will be a
     * data_scientist with r, x privileges on datasets inside the project)
     * <p>
     * @param user
     * @param project
     */
    public void modifyProjectMembership(Users user, Project project) {
        if (user == null || project == null || project.getProjectTeamCollection()
                == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String userName = getHdfsUserName(project, user);
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
        HdfsUsers hdfsUser = hdfsUsersFacade.findByName(userName);
        if (hdfsUser == null || hdfsGroup == null) {
            throw new IllegalArgumentException(
                    "Hdfs user not found or not in project group.");
        }
        hdfsUser.getHdfsGroupsCollection().remove(hdfsGroup);
        hdfsUsersFacade.merge(hdfsUser);
    }

    /**
     * Adds all members of project to the dataset's group. This will give the
     * added members read and execute privileges.
     * <p>
     * @param project
     * @param dataset
     */
    public void shareDataset(Project project, Dataset dataset) {
        if (project == null || dataset == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String datasetGroup = getHdfsGroupName(dataset);
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
        if (hdfsGroup == null) {
            throw new IllegalArgumentException("Dataset group not found");
        }
        if (hdfsGroup.getHdfsUsersCollection() == null) {
            hdfsGroup.setHdfsUsersCollection(new ArrayList<HdfsUsers>());
        }
        String hdfsUsername;
        HdfsUsers hdfsUser;

        hdfsUser = hdfsUsersFacade.findByName(project.getName());
        if (hdfsUser == null) {
            hdfsUser = new HdfsUsers(project.getName());
            hdfsUsersFacade.persist(hdfsUser);
        }
        if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
            hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
        }

        Collection<ProjectTeam> projectTeam = projectTeamFacade.
                findMembersByProject(project);

        //every member of the project the ds is going to be shard with is
        //added to the dataset group.
        for (ProjectTeam member : projectTeam) {
            hdfsUsername = getHdfsUserName(project, member.getUser());
            hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
            if (hdfsUser == null) {
                hdfsUser = new HdfsUsers(hdfsUsername);
            }
            if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
                hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
            }
        }
        hdfsGroupsFacade.merge(hdfsGroup);
    }

    /**
     * Deletes the project group from HDFS
     * <p>
     * @param project
     */
    public void deleteProjectGroup(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
        if (hdfsGroup != null) {
            hdfsGroupsFacade.remove(hdfsGroup);
        }
    }

    /**
     * Deletes the project group and all associated groups from HDFS
     * <p>
     * @param project
     * @param dsInProject
     */
    public void deleteProjectGroupsRecursive(Project project,
            List<Dataset> dsInProject) {
        if (project == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
        if (hdfsGroup != null) {
            hdfsGroupsFacade.remove(hdfsGroup);
        }
        byte[] dsGroupId;
        String dsGroups;
        HdfsGroups hdfsDsGroup;
        for (Dataset ds : dsInProject) {
            dsGroups = getHdfsGroupName(project, ds);
            hdfsDsGroup = hdfsGroupsFacade.findByName(dsGroups);
            if (hdfsDsGroup != null) {
                hdfsGroupsFacade.remove(hdfsDsGroup);
            }
        }
    }

    /**
     * Deletes all users associated with this project from HDFS
     * <p>
     * @param project
     * @param projectTeam
     */
    public void deleteProjectUsers(Project project,
            Collection<ProjectTeam> projectTeam) {
        if (project == null || projectTeam == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String hdfsUsername;
        HdfsUsers hdfsUser;
        hdfsUser = hdfsUsersFacade.findByName(project.getName());
        if (hdfsUser != null) {
            hdfsUsersFacade.removeHdfsUser(hdfsUser);
        }
        for (ProjectTeam member : projectTeam) {
            hdfsUsername = getHdfsUserName(project, member.getUser());
            hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
            dfsService.removeDfsOps(hdfsUsername);
            if (hdfsUser != null) {
                hdfsUsersFacade.removeHdfsUser(hdfsUser);
            }
        }
    }

    /**
     * Deletes the dataset group from HDFS
     * <p>
     * @param dataset
     */
    public void deleteDatasetGroup(Dataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String datasetGroup = getHdfsGroupName(dataset);
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
        if (hdfsGroup != null) {
            hdfsGroupsFacade.remove(hdfsGroup);
        }

    }

    /**
     * Remove all members of the project from the dataset group.
     * <p>
     * @param project
     * @param dataset
     */
    public void unShareDataset(Project project, Dataset dataset) {
        if (project == null || dataset == null) {
            throw new IllegalArgumentException("One or more arguments are null.");
        }
        String datasetGroup = getHdfsGroupName(dataset);
        HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
        if (hdfsGroup == null) {
            throw new IllegalArgumentException("Dataset group not found");
        }
        if (hdfsGroup.getHdfsUsersCollection() == null) {
            throw new IllegalArgumentException("The dataset group have no members.");
        }
        Collection<ProjectTeam> projectTeam = projectTeamFacade.
                findMembersByProject(project);
        String hdfsUsername;
        HdfsUsers hdfsUser;
        hdfsUser = hdfsUsersFacade.findByName(project.getName());
        if (hdfsUser != null) {
            hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
        }
        //every member of the project the ds is going to be unshard from is
        //removed from the dataset group.
        for (ProjectTeam member : projectTeam) {
            hdfsUsername = getHdfsUserName(project, member.getUser());
            hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
            if (hdfsUser != null) {
                hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
            }
        }
        hdfsGroupsFacade.merge(hdfsGroup);
    }

    /**
     * Returns the hdfs username for the user in this project
     * <p>
     * @param project
     * @param user
     * @return
     */
    public String getHdfsUserName(Project project, Users user) {
        if (project == null || user == null) {
            return null;
        }
        return project.getName() + USER_NAME_DELIMITER + user.getUsername();
    }

    /**
     * Returns the username given a hdfs username
     * <p>
     * @param hdfsUser
     * @return
     */
    public String getUserName(String hdfsUser) {
        return hdfsUser.split(USER_NAME_DELIMITER)[1];
    }

    /**
     * Returns the project name given a hdfs username
     * <p>
     * @param hdfsUser
     * @return
     */
    public String getProjectName(String hdfsUser) {
        return hdfsUser.split(USER_NAME_DELIMITER)[0];
    }
    
    /**
     * If the dataset is shared with this project we will get a group name that
     * does not exist.
     * <p>
     * @param project
     * @param ds
     * @return
     */
    public String getHdfsGroupName(Project project, Dataset ds) {
        if (project == null || ds == null) {
            return null;
        }
        return project.getName() + USER_NAME_DELIMITER + ds.getInode().getInodePK().
                getName();
    }

    /**
     * This will return a group name for the dataset Warning if the dataset is
     * shared this will still give us the group in the owning project.
     * <p>
     * @param dataset
     * @return
     */
    public String getHdfsGroupName(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        Inode inode = inodes.findById(dataset.getInode().getInodePK().getParentId());
        return inode.getInodePK().getName() + USER_NAME_DELIMITER
                + dataset.getInode().getInodePK().getName();

    }
}
