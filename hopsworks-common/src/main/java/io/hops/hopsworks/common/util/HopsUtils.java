package io.hops.hopsworks.common.util;

import com.google.common.io.Files;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.user.CertificateMaterializer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.net.util.Base64;

/**
 * Utility methods.
 * <p>
 */
public class HopsUtils {

  private static final Logger LOG = Logger.getLogger(HopsUtils.class.getName());
  public static final int ROOT_DIR_PARTITION_KEY = 0;
  public static final short ROOT_DIR_DEPTH = 0;
  public static int RANDOM_PARTITIONING_MAX_LEVEL = 1;
  public static int ROOT_INODE_ID = 1;
  public static int PROJECTS_DIR_DEPTH = 1;
  public static String PROJECTS_DIR_NAME = "Projects";

  /**
   *
   * @param <E>
   * @param value
   * @param enumClass
   * @return
   */
  public static <E extends Enum<E>> boolean isInEnum(String value,
      Class<E> enumClass) {
    for (E e : enumClass.getEnumConstants()) {
      if (e.name().equals(value)) {
        return true;
      }
    }
    return false;
  }

  public static int fileOrDirPartitionId(int parentId, String name) {
    return parentId;
  }

  public static int projectPartitionId(String name) {
    return calculatePartitionId(ROOT_INODE_ID, PROJECTS_DIR_NAME,
        PROJECTS_DIR_DEPTH);
  }

  public static int dataSetPartitionId(Inode parent, String name) {
    return calculatePartitionId(parent.getId(), name, 3);
  }

  public static int calculatePartitionId(int parentId, String name, int depth) {
    if (isTreeLevelRandomPartitioned(depth)) {
      return partitionIdHashFunction(parentId, name, depth);
    } else {
      return parentId;
    }
  }

  private static int partitionIdHashFunction(int parentId, String name,
      int depth) {
    if (depth == ROOT_DIR_DEPTH) {
      return ROOT_DIR_PARTITION_KEY;
    } else {
      return (name + parentId).hashCode();
    }
  }

  private static boolean isTreeLevelRandomPartitioned(int depth) {
    return depth <= RANDOM_PARTITIONING_MAX_LEVEL;
  }

  /**
   * Retrieves the global hadoop classpath.
   *
   * @param params
   * @return
   */
  public static String getHadoopClasspathGlob(String... params) {
    ProcessBuilder pb = new ProcessBuilder(params);
    try {
      Process process = pb.start();
      int errCode = process.waitFor();
      if (errCode != 0) {
        return "";
      }
      StringBuilder sb = new StringBuilder();
      try (BufferedReader br
          = new BufferedReader(new InputStreamReader(process.
              getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line);
        }
      }
      //Now we must remove the yarn shuffle library as it creates issues for 
      //Zeppelin Spark Interpreter
      StringBuilder classpath = new StringBuilder();

      for (String path : sb.toString().split(File.pathSeparator)) {
        if (!path.contains("yarn") && !path.contains("jersey") && !path.
            contains("servlet")) {
          classpath.append(path).append(File.pathSeparator);
        }
      }
      if (classpath.length() > 0) {
        return classpath.toString().substring(0, classpath.length() - 1);
      }

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class.getName()).log(Level.SEVERE, null, ex);
    }
    return "";
  }

  public static String getProjectKeystoreName(String project, String user) {
    return project + "__" + user + "__kstore.jks";
  }

  public static String getProjectTruststoreName(String project, String user) {
    return project + "__" + user + "__tstore.jks";
  }

  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
      String localTmpDir, String remoteTmpDir, CertificateMaterializer
      certMat) {
    copyUserKafkaCerts(userCerts, project, username, localTmpDir, remoteTmpDir,
        null, null, null, null, null, null, certMat);
  }

  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
      String localTmpDir, String remoteTmpDir, JobType jobType,
      DistributedFileSystemOps dfso,
      List<LocalResourceDTO> projectLocalResources,
      Map<String, String> jobSystemProperties,
      String applicationId, CertificateMaterializer certMat) {
    copyUserKafkaCerts(userCerts, project, username, localTmpDir, remoteTmpDir,
        jobType, dfso, projectLocalResources, jobSystemProperties,
        null, applicationId, certMat);
  }
  
  private static boolean checkMaterializedCertificatesExist(String username,
      String projectName, String remoteFSDir, DistributedFileSystemOps dfso,
      boolean isForZeppelin)
    throws IOException {
    
    return checkMatCertsInHDFS(username, projectName, remoteFSDir, dfso, isForZeppelin);
  }
  
  private static boolean checkMatCertsInHDFS(String username, String
      projectName, String remoteFSDir, DistributedFileSystemOps dfso, boolean
      isForZeppelin)
      throws IOException {
    Path kstoreU = new Path(remoteFSDir + Path.SEPARATOR +
        username + Path.SEPARATOR + username + "__kstore.jks");
    Path tstoreU = new Path(remoteFSDir + Path.SEPARATOR +
        username + Path.SEPARATOR + username + "__tstore.jks");
    
    if (isForZeppelin) {
      Path kstoreP = new Path(remoteFSDir + Path.SEPARATOR + projectName +
          Path.SEPARATOR + projectName + "__kstore.jks");
      Path tstoreP = new Path(remoteFSDir + Path.SEPARATOR + projectName +
          Path.SEPARATOR + projectName + "__tstore.jks");
  
      return dfso.exists(kstoreU.toString()) && dfso.exists(tstoreU.toString())
          && dfso.exists(kstoreP.toString()) && dfso.exists(tstoreP.toString());
    }
    
    return dfso.exists(kstoreU.toString()) && dfso.exists(tstoreU.toString());
  }
  
  /**
   * Remote user certificates materialized both from the local
   * filesystem and from HDFS
   * @param username
   * @param remoteFSDir
   * @param dfso
   * @param certificateMaterializer
   * @param isForZeppelin When set to true it will also remove the
   *                      project-wide certificates
   * @throws IOException
   */
  public static void cleanupCertificatesForUser(String username,
      String projectName, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer
      certificateMaterializer, boolean isForZeppelin) throws IOException {
    String projectSpecificUsername = projectName + HdfsUsersController
        .USER_NAME_DELIMITER + username;
    cleanupCertsLocal(username, projectName, certificateMaterializer, isForZeppelin);
    cleanupCertsHDFS(projectSpecificUsername, projectName, remoteFSDir, dfso,
        isForZeppelin);
  }
  
  private static void cleanupCertsLocal(String username, String
      projectName, CertificateMaterializer certificateMaterializer, boolean
      isForZeppelin) {
    certificateMaterializer.removeCertificate(username, projectName);
    if (isForZeppelin) {
      certificateMaterializer.removeCertificate(projectName);
    }
  }
  
  private static void cleanupCertsHDFS(String username, String
      projectName, String remoteFSDir, DistributedFileSystemOps dfso, boolean
      isForZeppelin) throws IOException {
    Path remoteProjectDirK = new Path(remoteFSDir + Path.SEPARATOR
      + username + Path.SEPARATOR + username + "__kstore.jks");
    Path remoteProjectDirT = new Path(remoteFSDir + Path.SEPARATOR
        + username + Path.SEPARATOR + username + "__tstore.jks");
    dfso.rm(remoteProjectDirK, false);
    dfso.rm(remoteProjectDirT, false);
  
    if (isForZeppelin) {
      remoteProjectDirK = new Path(remoteFSDir + Path.SEPARATOR
          + projectName + Path.SEPARATOR + projectName + "__kstore.jks");
      remoteProjectDirT = new Path(remoteFSDir + Path.SEPARATOR
          + projectName + Path.SEPARATOR + projectName + "__tstore.jks");
      dfso.rm(remoteProjectDirK, false);
      dfso.rm(remoteProjectDirT, false);
    }
  }
  
  /**
   * Utility method that materializes user certificates in the local
   * filesystem and in HDFS
   * @param projectName
   * @param localFSDir
   * @param remoteFSDir
   * @param dfso
   * @param certificateMaterializer
   * @param settings
   * @param isForZeppelin When it is set to true it will materialize also the
   *                     project-wide certificates for the Spark interpreter
   *                      in Zeppelin
   * @throws IOException
   */
  public static void materializeCertificatesForUser(String projectName,
      String userName, String localFSDir, String remoteFSDir,
      DistributedFileSystemOps dfso, CertificateMaterializer
      certificateMaterializer, Settings settings, boolean isForZeppelin) throws
      IOException {
    // For Spark interpreter, jobs are launched as user Project
    // For Livy interpreter, jobs are launched as user Project__Username
    // Both will be materialized
    String projectSpecificUsername = projectName + "__" + userName;
    
    if (isForZeppelin) {
      certificateMaterializer.materializeCertificates(projectName);
    }
    certificateMaterializer.materializeCertificates(userName, projectName);
    
    // If certificates exist in HDFS do not materialize them again
    if (checkMaterializedCertificatesExist(projectSpecificUsername, projectName,
        remoteFSDir, dfso, isForZeppelin)) {
      return;
    }
    
    String kStorePath, tStorePath;
    
    if (isForZeppelin) {
      kStorePath =
          localFSDir + File.separator + projectName + "__kstore.jks";
      tStorePath =
          localFSDir + File.separator + projectName + "__tstore.jks";
  
      materializeCertsRemote(projectName, remoteFSDir, kStorePath, tStorePath,
          dfso);
  
    }
    kStorePath = localFSDir + File.separator + projectSpecificUsername + "__kstore.jks";
    tStorePath = localFSDir + File.separator + projectSpecificUsername + "__tstore.jks";
    materializeCertsRemote(projectSpecificUsername, remoteFSDir, kStorePath,
        tStorePath, dfso);
    
    // If RPC SSL is not enabled, we don't need them anymore in the local fs
    if (!settings.getHopsRpcTls()) {
      if (isForZeppelin) {
        certificateMaterializer.removeCertificate(projectName);
      }
      certificateMaterializer.removeCertificate(userName, projectName);
    }
  }
  
  private static void materializeCertsRemote(String prefix, String
      remoteFSDir, String kStorePath, String tStorePath,
      DistributedFileSystemOps dfso) throws IOException {
    
    if (!dfso.exists(remoteFSDir)) {
      Path remoteFSTarget = new Path(remoteFSDir);
      dfso.mkdir(remoteFSTarget, new FsPermission(
          FsAction.ALL, FsAction.ALL, FsAction.ALL));
    }
  
    // Now upload them also to HDFS
    Path projectRemoteFSDir = new Path(remoteFSDir + Path.SEPARATOR +
        prefix);
    Path remoteProjectKStore = new Path(projectRemoteFSDir, prefix +
        "__kstore.jks");
    Path remoteProjectTStore = new Path(projectRemoteFSDir, prefix +
        "__tstore.jks");
    if (dfso.exists(projectRemoteFSDir.toString())) {
      dfso.rm(remoteProjectKStore, false);
      dfso.rm(remoteProjectTStore, false);
    } else {
      dfso.mkdir(projectRemoteFSDir, new FsPermission(
          FsAction.ALL, FsAction.ALL, FsAction.NONE));
      dfso.setOwner(projectRemoteFSDir, prefix, prefix);
    }
    
    FsPermission materialPermissions = new FsPermission(FsAction.ALL,
        FsAction.NONE, FsAction.NONE);
  
    dfso.copyToHDFSFromLocal(false, kStorePath,
        remoteProjectKStore.toString());
    dfso.setPermission(remoteProjectKStore, materialPermissions);
    dfso.setOwner(remoteProjectKStore, prefix, prefix);
  
    dfso.copyToHDFSFromLocal(false, tStorePath,
        remoteProjectTStore.toString());
    dfso.setPermission(remoteProjectTStore, materialPermissions);
    dfso.setOwner(remoteProjectTStore, prefix, prefix);
    
    // Cache should be flushed otherwise NN will raise permission exceptions
    dfso.flushCachedUser(prefix);
  }

  /**
   * Utility method that copies Kafka user certificates from the Database, to
   * either hdfs to be passed as LocalResources to the YarnJob or to used
   * by another method.
   *
   * @param userCerts
   * @param project
   * @param username
   * @param localTmpDir
   * @param remoteTmpDir
   * @param jobType
   * @param dfso
   * @param projectLocalResources
   * @param jobSystemProperties
   * @param flinkCertsDir
   * @param applicationId
   */
  public static void copyUserKafkaCerts(CertsFacade userCerts,
      Project project, String username,
      String localTmpDir, String remoteTmpDir, JobType jobType,
      DistributedFileSystemOps dfso,
      List<LocalResourceDTO> projectLocalResources,
      Map<String, String> jobSystemProperties,
      String flinkCertsDir, String applicationId, CertificateMaterializer
      certMat) {
  
    // Let the Certificate Materializer handle the certificates
    UserCerts userCert = new UserCerts(project.getName(), username);
    try {
      certMat.materializeCertificates(username, project.getName());
      byte[][] material = certMat.getUserMaterial(username, project.getName());
      if (material == null) {
        throw new IOException("User certificates are null");
      }
      userCert.setUserKey(material[0]);
      userCert.setUserCert(material[1]);
    } catch (IOException ex) {
      throw new RuntimeException("Could not materialize user certificates", ex);
    }
    
    //Pull the certificate of the client
    /*UserCerts userCert = userCerts.findUserCert(project.getName(),
        username);*/
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null && userCert.getUserCert().length > 0
        && userCert.getUserKey() != null && userCert.getUserKey().length
        > 0) {
    
      Map<String, byte[]> kafkaCertFiles = new HashMap<>();
      kafkaCertFiles.put(Settings.T_CERTIFICATE, userCert.getUserCert());
      kafkaCertFiles.put(Settings.K_CERTIFICATE, userCert.getUserKey());
      //Create tmp cert directory if not exists for certificates to be copied to hdfs.
      //Certificates will later be deleted from this directory when copied to HDFS.
      
      // This is done in CertificateMaterializer
      /*File certDir = new File(localTmpDir);
      if (!certDir.exists()) {
        try {
          certDir.setExecutable(false);
          certDir.setReadable(true, true);
          certDir.setWritable(true, true);
          certDir.mkdir();
        } catch (SecurityException ex) {
          LOG.log(Level.SEVERE, ex.getMessage());//handle it
        }
      }*/
      Map<String, File> kafkaCerts = new HashMap<>();
      try {
        String kCertName = HopsUtils.getProjectKeystoreName(project.getName(),
            username);
        String tCertName = HopsUtils.getProjectTruststoreName(project.
            getName(), username);
      
        // if file doesnt exists, then create it
        try {
          if (jobType == null) {
            // This is done in CertificateMaterializer
            
            //Copy the certificates in the local tmp dir
            /*File kCert = new File(localTmpDir
                + File.separator + kCertName);
            File tCert = new File(localTmpDir
                + File.separator + tCertName);
            if (!kCert.exists()) {
              Files.write(kafkaCertFiles.get(Settings.K_CERTIFICATE),
                  kCert);
              Files.write(kafkaCertFiles.get(Settings.T_CERTIFICATE),
                  tCert);
            }*/
          } else //If it is a Flink job, copy the certificates into the config dir
          {
            switch (jobType) {
              case FLINK:
                File appDir = Paths.get(flinkCertsDir, applicationId).toFile();
                if (!appDir.exists()) {
                  appDir.mkdir();
                }
              
                File f_k_cert = new File(appDir.toString() + File.separator +
                    kCertName);
                f_k_cert.setExecutable(false);
                f_k_cert.setReadable(true, true);
                f_k_cert.setWritable(false);
                File t_k_cert = new File(appDir.toString() + File.separator +
                    tCertName);
                t_k_cert.setExecutable(false);
                t_k_cert.setReadable(true, true);
                t_k_cert.setWritable(false);
                if (!f_k_cert.exists()) {
                  Files.write(kafkaCertFiles.get(Settings.K_CERTIFICATE),
                      f_k_cert);
                  Files.write(kafkaCertFiles.get(Settings.T_CERTIFICATE),
                      t_k_cert);
                }
                jobSystemProperties.put(Settings.K_CERTIFICATE, f_k_cert.toString());
                jobSystemProperties.put(Settings.T_CERTIFICATE, t_k_cert.toString());
                break;
              case TENSORFLOW:
              case PYSPARK:
              case TFSPARK:
              case SPARK:
                kafkaCerts.put(Settings.K_CERTIFICATE, new File(
                    localTmpDir + File.separator + kCertName));
                kafkaCerts.put(Settings.T_CERTIFICATE, new File(
                    localTmpDir + File.separator + tCertName));
                for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
                  /*if (!entry.getValue().exists()) {
                    entry.getValue().createNewFile();
                  }*/
                
                  //Write the actual file(cert) to localFS
                  //Create HDFS kafka certificate directory. This is done
                  //So that the certificates can be used as LocalResources
                  //by the YarnJob
                  if (!dfso.exists(remoteTmpDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpDir), new FsPermission(
                            FsAction.ALL,
                            FsAction.ALL, FsAction.ALL));
                  }
                  //Put project certificates in its own dir
                  String certUser = project.getName() + "__"
                      + username;
                  String remoteTmpProjDir = remoteTmpDir + File.separator
                      + certUser;
                  if (!dfso.exists(remoteTmpProjDir)) {
                    dfso.mkdir(
                        new Path(remoteTmpProjDir),
                        new FsPermission(FsAction.ALL,
                            FsAction.ALL, FsAction.NONE));
                    dfso.setOwner(new Path(remoteTmpProjDir),
                        certUser, certUser);
                  }
                
                  String remoteProjAppDir = remoteTmpProjDir + File.separator
                      + applicationId;
                  Path remoteProjAppPath = new Path(remoteProjAppDir);
                  if (!dfso.exists(remoteProjAppDir)) {
                    dfso.mkdir(remoteProjAppPath,
                        new FsPermission(FsAction.ALL,
                            FsAction.ALL, FsAction.NONE));
                    dfso.setOwner(remoteProjAppPath, certUser, certUser);
                  }
                
                  /*Files.write(kafkaCertFiles.get(entry.getKey()), entry.
                      getValue());*/
                  dfso.copyToHDFSFromLocal(false, entry.getValue().
                          getAbsolutePath(),
                      remoteProjAppDir + File.separator
                          + entry.getValue().getName());
                
                  dfso.setPermission(new Path(remoteProjAppDir
                          + File.separator
                          + entry.getValue().getName()),
                      new FsPermission(FsAction.ALL, FsAction.NONE,
                          FsAction.NONE));
                  dfso.setOwner(new Path(remoteProjAppDir + File.separator
                      + entry.getValue().getName()), certUser, certUser);
                
                  projectLocalResources.add(new LocalResourceDTO(
                      entry.getKey(),
                      "hdfs://" + remoteProjAppDir + File.separator + entry
                          .getValue().getName(),
                      LocalResourceVisibility.APPLICATION.toString(),
                      LocalResourceType.FILE.toString(), null));
                }
                break;
              default:
                break;
            }
          }
        } catch (IOException ex) {
          LOG.log(Level.SEVERE,
              "Error writing Kakfa certificates to local fs", ex);
        }
      
      } finally {
        //In case the certificates where not removed
        /*for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
          if (entry.getValue().exists()) {
            entry.getValue().delete();
          }
        }*/
        if (jobType != null) {
          certMat.removeCertificate(username, project.getName());
        }
      }
    }
  }

  /**
   *
   * @param jobName
   * @param dissalowedChars
   * @return
   */
  public static boolean jobNameValidator(String jobName, String dissalowedChars) {
    for (char c : dissalowedChars.toCharArray()) {
      if (jobName.contains("" + c)) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   * @param key
   * @param salt
   * @param plaintext
   * @return
   * @throws Exception
   */
  public static String encrypt(String key, String salt, String plaintext) throws Exception {
    Key aesKey = new SecretKeySpec(key.substring(0, 16).getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] encrypted = cipher.doFinal(plaintext.getBytes());
    return Base64.encodeBase64String(encrypted);
  }

  /**
   *
   * @param key
   * @param salt
   * @param ciphertext
   * @return
   * @throws Exception
   */
  public static String decrypt(String key, String salt, String ciphertext) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    Key aesKey = new SecretKeySpec(key.substring(0, 16).getBytes(), "AES");
    cipher.init(Cipher.DECRYPT_MODE, aesKey);
    String decrypted = new String(cipher.doFinal(Base64.decodeBase64(ciphertext)));
    return decrypted;
  }

  /**
   * Generates a pseudo-random password for the user keystore. A list of characters is excluded.
   *
   * @param length
   * @return
   */
  public static String randomString(int length) {
    char[] characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    Random random = new SecureRandom();
    char[] result = new char[length];
    for (int i = 0; i < result.length; i++) {
      // picks a random index out of character set > random character
      int randomCharIndex = random.nextInt(characterSet.length);
      result[i] = characterSet[randomCharIndex];
    }
    return new String(result);
  }

}
