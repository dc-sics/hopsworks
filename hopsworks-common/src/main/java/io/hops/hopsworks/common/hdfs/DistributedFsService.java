package io.hops.hopsworks.common.hdfs;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import io.hops.hopsworks.common.util.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;

@Stateless
public class DistributedFsService {

  private static final Logger logger = Logger.getLogger(
          DistributedFsService.class.
          getName());
  
  @EJB
  private InodeFacade inodes;
  @EJB
  private UserGroupInformationService ugiService;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService bhcs;

  private Configuration conf;
  private String hadoopConfDir;
  private String hostname;
  private String transientDir;
  private String serviceCertsDir;

  public DistributedFsService() {
  }
  
  @PostConstruct
  public void init() {
    System.setProperty("hadoop.home.dir", settings.getHadoopDir());
    hadoopConfDir = settings.getHadoopConfDir();
    //Get the configuration file at found path
    File hadoopConfFile = new File(hadoopConfDir, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }

    File hdfsConfFile = new File(hadoopConfDir, "hdfs-site.xml");
    if (!hdfsConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              hdfsConfFile);
      throw new IllegalStateException("No hdfs conf file: hdfs-site.xml");
    }

    //Set the Configuration object for the hdfs client
    Path hdfsPath = new Path(hdfsConfFile.getAbsolutePath());
    Path hadoopPath = new Path(hadoopConfFile.getAbsolutePath());
    conf = new Configuration();
    conf.addResource(hadoopPath);
    conf.addResource(hdfsPath);
    conf.set("fs.permissions.umask-mode", "000");
    conf.setStrings("dfs.namenode.rpc-address", hdfsLeDescriptorsFacade.
            getSingleEndpoint());
//    conf.setStrings("dfs.namenodes.rpc.addresses", hdfsLeDescriptorsFacade.getActiveNN().getHostname());
//    conf.setStrings("fs.defaultFS", "hdfs://"+hdfsLeDescriptorsFacade.getActiveNN().getHostname());
    if (settings.getHopsRpcTls()) {
      bhcs.parseServerSSLConf(conf);
      
      try {
        hostname = InetAddress.getLocalHost().getHostName();
        transientDir = settings.getHopsworksTmpCertDir();
        serviceCertsDir = conf.get(HopsSSLSocketFactory.CryptoKeys
                .SERVICE_CERTS_DIR.getValue(),
            HopsSSLSocketFactory.CryptoKeys.SERVICE_CERTS_DIR.getDefaultValue());
      } catch (UnknownHostException ex) {
        logger.log(Level.SEVERE, "Could not determine hostname "
            + ex.getMessage(), ex);
        throw new RuntimeException("Could not determine hostname", ex);
      }
    }
  }

  @PreDestroy
  public void preDestroy() {
    conf.clear();
    conf = null;
  }

  /**
   * creates a new distributed file system operations with the super user
   * <p>
   * @return DistributedFileSystemOps
   */
  public DistributedFileSystemOps getDfsOps() {
    if (settings.getHopsRpcTls()) {
      Configuration newConf = new Configuration(conf);
  
      String keystorePath = bhcs.getSuperKeystorePath();
      String keystorePass = bhcs.getSuperKeystorePassword();
      String truststorePath = bhcs.getSuperTrustStorePath();
      String truststorePass = bhcs.getSuperTrustStorePassword();
  
      HopsSSLSocketFactory.setTlsConfiguration(keystorePath, keystorePass,
          truststorePath, truststorePass, newConf);
      
      return new DistributedFileSystemOps(
          UserGroupInformation.createRemoteUser(settings.getHdfsSuperUser()),
          newConf);
    }
  
    return new DistributedFileSystemOps(UserGroupInformation.createRemoteUser(
        settings.getHdfsSuperUser()), conf);
  }
  
  public DistributedFileSystemOps getDfsOps(URI uri) {
    if (settings.getHopsRpcTls()) {
      Configuration newConf = new Configuration(conf);
  
      String keystorePath = bhcs.getSuperKeystorePath();
      String keystorePass = bhcs.getSuperKeystorePassword();
      String truststorePath = bhcs.getSuperTrustStorePath();
      String truststorePass = bhcs.getSuperTrustStorePassword();
  
      HopsSSLSocketFactory.setTlsConfiguration(keystorePath, keystorePass,
          truststorePath, truststorePass, newConf);
      
      return new DistributedFileSystemOps(
          UserGroupInformation.createRemoteUser(settings.getHdfsSuperUser()),
          newConf, uri);
    }
    
    return new DistributedFileSystemOps(UserGroupInformation.createRemoteUser
        (settings.getHdfsSuperUser()), conf, uri);
  }
  
  /**
   * Returns the user specific distributed file system operations
   * <p>
   * @param username
   * @return
   */
  public DistributedFileSystemOps getDfsOps(String username) {
    if (username == null || username.isEmpty()) {
      throw new NullPointerException("username not set.");
    }
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUser(username, UserGroupInformation.
              getLoginUser());
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
    if (settings.getHopsRpcTls()) {
      // TODO(Antonis) We should propagate the original exception
      // Runtime exceptions are not useful
      try {
        bhcs.materializeCertsForNonSuperUser(username);
        Configuration newConf = new Configuration(conf);
        bhcs.configureTlsForProjectSpecificUser(username, transientDir,
            newConf);
  
        return new DistributedFileSystemOps(ugi, newConf);
      } catch (BaseHadoopClientsService.CryptoPasswordNotFoundException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        String[] project_username = username.split(HdfsUsersController
            .USER_NAME_DELIMITER);
        bhcs.removeNonSuperUserCertificate(project_username[1],
            project_username[0]);
        return null;
      }
    }

    return new DistributedFileSystemOps(ugi, conf);
  }

  public void closeDfsClient(DistributedFileSystemOps udfso) {
    if (null != udfso) {
      String[] tokens = udfso.getEffectiveUser().split(HdfsUsersController
          .USER_NAME_DELIMITER);
      if (null != tokens && tokens.length == 2) {
        bhcs.removeNonSuperUserCertificate(tokens[1], tokens[0]);
      }
      udfso.close();
    }
  }
  
  public DistributedFileSystemOps getDfsOpsForTesting(String username) {
    if (username == null || username.isEmpty()) {
      throw new NullPointerException("username not set.");
    }
    //Get hdfs groups
    Collection<HdfsGroups> groups = hdfsUsersFacade.findByName(username).
            getHdfsGroupsCollection();
    String[] userGroups = new String[groups.size()];
    Iterator<HdfsGroups> iter = groups.iterator();
    int i = 0;
    while (iter.hasNext()) {
      userGroups[i] = iter.next().getName();
      i++;
    }
    UserGroupInformation ugi;
    try {
      ugi = UserGroupInformation.createProxyUserForTesting(username,
              UserGroupInformation.
              getLoginUser(), userGroups);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }

    if (settings.getHopsRpcTls()) {
      // TODO(Antonis) We should propagate the original exception
      // Runtime exceptions are not useful
      try {
        bhcs.materializeCertsForNonSuperUser(username);
        Configuration newConf = new Configuration(conf);
        bhcs.configureTlsForProjectSpecificUser(username, transientDir,
            newConf);
    
        return new DistributedFileSystemOps(ugi, newConf);
      } catch (BaseHadoopClientsService.CryptoPasswordNotFoundException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        String[] project_username = username.split(HdfsUsersController
            .USER_NAME_DELIMITER);
        bhcs.removeNonSuperUserCertificate(project_username[1],
            project_username[0]);
        return null;
      }
    }

    return new DistributedFileSystemOps(ugi, conf);
  }

  /**
   * Removes the user group info and closes any file system created for this
   * user.
   * <p>
   * @param username
   */
  public void removeDfsOps(String username) {
    if (username == null || username.isEmpty()) {
      return;
    }
    UserGroupInformation ugi = ugiService.remove(username);
    if (ugi == null) {
      return;
    }
    try {
      FileSystem.closeAllForUGI(ugi);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Could not close file system for user " + ugi.
              getUserName(), ex);
    }
  }

  /**
   * Check if the inode at the given path is a directory.
   * <p/>
   * @param path
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean isDir(String path) {
    Inode i = inodes.getInodeAtPath(path);
    if (i != null) {
      return i.isDir();
    }
    return false;
  }

  /**
   * Get the inode for a given path.
   * <p/>
   * @param path
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Inode getInode(String path) {
    Inode i = inodes.getInodeAtPath(path);
    return i;
  }

  /**
   * Get a list of the names of the child files (so no directories) of the given
   * path.
   * <p/>
   * @param path
   * @return A list of filenames, empty if the given path does not have
   * children.
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public List<String> getChildNames(String path) {
    Inode inode = inodes.getInodeAtPath(path);
    if (inode.isDir()) {
      List<Inode> inodekids = inodes.getChildren(inode);
      ArrayList<String> retList = new ArrayList<>(inodekids.size());
      for (Inode i : inodekids) {
        if (!i.isDir()) {
          retList.add(i.getInodePK().getName());
        }
      }
      return retList;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Returns a list of inodes if the path is a directory empty list otherwise.
   *
   * @param path
   * @return
   */
  public List<Inode> getChildInodes(String path) {
    Inode inode = inodes.getInodeAtPath(path);
    if (inode.isDir()) {
      return inodes.getChildren(inode);
    } else {
      return Collections.EMPTY_LIST;
    }
  }
}
