/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.dao.RemoteMaterialRefID;
import io.hops.hopsworks.common.security.dao.RemoteMaterialReferences;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.bag.HashBag;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("Settings")
public class CertificateMaterializer {
  private static final Logger LOG = Logger.getLogger(CertificateMaterializer.class.getName());
  
  private final static String KEYSTORE_SUFFIX = "__kstore.jks";
  private final static String TRUSTSTORE_SUFFIX = "__tstore.jks";
  private final static String CERT_PASS_SUFFIX = "__cert.key";
  private final static Pattern HDFS_SCHEME = Pattern.compile("^hdfs://.*");
  private final static int MAX_NUMBER_OF_RETRIES = 3;
  private final static long RETRY_WAIT_TIMEOUT = 10;
  
  private final Map<MaterialKey, Bag> materializedCerts;
  private final Map<MaterialKey, CryptoMaterial> materialCache;
  private final Map<MaterialKey, Map<String, Runnable>> fileRemovers;
  private final ReentrantReadWriteLock localLock;
  private final ReentrantReadWriteLock.ReadLock localReadLock;
  private final ReentrantReadWriteLock.WriteLock localWriteLock;
  private final ReentrantReadWriteLock remoteLock;
  private final ReentrantReadWriteLock.ReadLock remoteReadLock;
  private final ReentrantReadWriteLock.WriteLock remoteWriteLock;
  private final Set<Integer> projectsWithOpenInterpreters;
  
  private String lock_id;
  
  private String transientDir;
  private Long DELAY_VALUE;
  private TimeUnit DELAY_TIMEUNIT;
  
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private RemoteMaterialReferencesFacade remoteMaterialReferencesFacade;
  @EJB
  private DistributedFsService distributedFsService;
  @Resource
  private ManagedScheduledExecutorService scheduler;
  
  public CertificateMaterializer() {
    materializedCerts = new HashMap<>();
    materialCache = new HashMap<>();
    fileRemovers = new HashMap<>();
    localLock = new ReentrantReadWriteLock(true);
    localReadLock = localLock.readLock();
    localWriteLock = localLock.writeLock();
    remoteLock = new ReentrantReadWriteLock(true);
    remoteWriteLock = remoteLock.writeLock();
    remoteReadLock = remoteLock.readLock();
    projectsWithOpenInterpreters = new ConcurrentSkipListSet<>();
  }
  
  @PostConstruct
  public void init() {
    File tmpDir = new File(settings.getHopsworksTmpCertDir());
    if (!tmpDir.exists()) {
      throw new IllegalStateException("Transient certificates directory <" +
          tmpDir.getAbsolutePath() + "> does NOT exist!");
    }
    
    try {
      PosixFileAttributeView fileView = Files.getFileAttributeView(tmpDir
          .toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> permissions = fileView.readAttributes()
          .permissions();
      boolean ownerRead = permissions.contains(PosixFilePermission.OWNER_READ);
      boolean ownerWrite = permissions.contains(PosixFilePermission
          .OWNER_WRITE);
      boolean ownerExecute = permissions.contains(PosixFilePermission
          .OWNER_EXECUTE);
      
      boolean groupRead = permissions.contains(PosixFilePermission.GROUP_READ);
      boolean groupWrite = permissions.contains(PosixFilePermission
          .GROUP_WRITE);
      boolean groupExecute = permissions.contains(PosixFilePermission
          .GROUP_EXECUTE);
      
      boolean othersRead = permissions.contains(PosixFilePermission
          .OTHERS_READ);
      boolean othersWrite = permissions.contains(PosixFilePermission
          .OTHERS_WRITE);
      boolean othersExecute = permissions.contains(PosixFilePermission
          .OTHERS_EXECUTE);
      
      // Permissions should be 750
      if ((ownerRead && ownerWrite && ownerExecute)
          && (groupRead && !groupWrite && groupExecute)
          && (!othersRead && !othersWrite & !othersExecute)) {
        String owner = fileView.readAttributes().owner().getName();
        String group = fileView.readAttributes().group().getName();
        String permStr = PosixFilePermissions.toString(permissions);
        LOG.log(Level.INFO, "Passed permissions check for " + tmpDir.getAbsolutePath()
            + ". Owner: " + owner + " Group: " + group + " permissions: " + permStr);
      } else {
        throw new IllegalStateException("Wrong permissions for " +
            tmpDir.getAbsolutePath() + ", it should be 0750");
      }
    } catch (UnsupportedOperationException ex) {
      LOG.log(Level.WARNING, "Associated filesystem is not POSIX compliant. " +
          "Continue without checking the permissions of " + tmpDir
          .getAbsolutePath() + " This might be a security problem.");
    } catch (IOException ex) {
      throw new IllegalStateException("Error while getting filesystem " +
          "permissions of " + tmpDir.getAbsolutePath(), ex);
    }
    
    try {
      FileUtils.cleanDirectory(tmpDir);
    } catch (IOException ex) {
      LOG.log(Level.WARNING, "Could not clean directory " + tmpDir
          .getAbsolutePath() + " during startup, there might be stale " +
          "certificates", ex);
    }
    transientDir = tmpDir.getAbsolutePath();
    String delayRaw = settings.getCertificateMaterializerDelay();
    DELAY_VALUE = Settings.getConfTimeValue(delayRaw);
    DELAY_TIMEUNIT = Settings.getConfTimeTimeUnit(delayRaw);
    
    try {
      String hostAddress = InetAddress.getLocalHost().getHostAddress();
      long threadId = Thread.currentThread().getId();
      String lock_identifier = hostAddress + "_" + threadId;
      lock_id = lock_identifier.length() <= 30 ? lock_identifier : lock_identifier.substring(0, 30);
    } catch (UnknownHostException ex) {
      throw new IllegalStateException(ex);
    }
  }
  
  @PreDestroy
  public void tearDown() {
    try {
      FileUtils.cleanDirectory(new File(transientDir));
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not clean directory " + transientDir
          + " Administrator should clean it manually!", ex);
    }
  }
  
  /*
   * Start of Certificate materializer API
   */
  
  /**
   * Materialize project *generic* certificates in *local* filesystem in the *standard* directory
   *
   * @param projectName Name of the Project
   * @throws IOException
   */
  public void materializeCertificatesLocal(String projectName) throws IOException {
    materializeCertificatesLocal(null, projectName);
  }
  
  /**
   * Materialize project *specific* certificates in *local* filesystem in the *standard* directory
   *
   * @param userName Username of the user
   * @param projectName Name of the Project
   * @throws IOException
   */
  public void materializeCertificatesLocal(String userName, String projectName)
      throws IOException {
    MaterialKey key = new MaterialKey(userName, projectName);
    try {
      localWriteLock.lock();
      materializeLocalInternal(key, transientDir);
    } finally {
      localWriteLock.unlock();
    }
  }
  
  /**
   * Materialize project *generic* certificates in *local* filesystem in a *non-standard* directory.
   * Developer should take care of the correct permission of the directory
   *
   * @param projectName Name of the Project
   * @param localDirectory Directory to materialize certificates
   * @throws IOException
   */
  public void materializeCertificatesLocalCustomDir(String projectName, String localDirectory)
    throws IOException {
    materializeCertificatesLocalCustomDir(null, projectName, localDirectory);
  }
  
  /**
   * Materialize project *specific* certificates in *local* filesystem in a *non-standard* directory.
   * Developer should take care of the correct permission of the directory
   *
   * @param userName Username of the user
   * @param projectName Name of the Project
   * @param localDirectory Directory to materialize certificates
   * @throws IOException
   */
  public void materializeCertificatesLocalCustomDir(String userName, String projectName, String localDirectory)
    throws IOException {
    MaterialKey key = new MaterialKey(userName, projectName);
    localDirectory = localDirectory != null ? localDirectory : transientDir;
    try {
      localWriteLock.lock();
      materializeLocalInternal(key, localDirectory);
    } finally {
      localWriteLock.unlock();
    }
  }
  
  /**
   * Remove project *generic* certificates from *local* filesystem, from *standard* directory
   *
   * @param projectName Name of the Project
   */
  public void removeCertificatesLocal(String projectName) {
    removeCertificatesLocal(null, projectName);
  }
  
  /**
   * Remove project *specific* certificates from *local* filesystem, from the *standard* directory
   *
   * @param userName Username of the user
   * @param projectName Name of the Project
   */
  public void removeCertificatesLocal(String userName, String projectName) {
    MaterialKey key = new MaterialKey(userName, projectName);
    try {
      localWriteLock.lock();
      removeLocal(key, transientDir);
    } finally {
      localWriteLock.unlock();
    }
  }
  
  /**
   * Remove project *generic* certificates from *local* filesystem, from a *non-standard* directory
   *
   * @param projectName Name of the Project
   * @param localDirectory Local directory where the certificates were materialized
   */
  public void removeCertificatesLocalCustomDir(String projectName, String localDirectory) {
    removeCertificatesLocalCustomDir(null, projectName, localDirectory);
  }
  
  /**
   * Remove project *specific* certificates from *local* filesystem, from a *non-standard* directory
   *
   * @param username Username of the user
   * @param projectName Name of the Project
   * @param localDirectory Local directory where the certificates were materialized
   */
  public void removeCertificatesLocalCustomDir(String username, String projectName, String localDirectory) {
    MaterialKey key = new MaterialKey(username, projectName);
    localDirectory = localDirectory != null ? localDirectory : transientDir;
    try {
      localWriteLock.lock();
      removeLocal(key, localDirectory);
    } finally {
      localWriteLock.unlock();
    }
  }
  
  /**
   * Materialize project *specific* certificates in *remote* filesystem
   *
   * @param userName Username of the user
   * @param projectName Name of the project
   * @param ownerName Owner of remote files
   * @param groupName Group of remote files
   * @param permissions Permissions of remote files
   * @param remoteDirectory Remote directory to put the material
   * @throws IOException
   */
  public void materializeCertificatesRemote(String userName, String projectName, String ownerName, String groupName,
      FsPermission permissions, String remoteDirectory) throws IOException {
    if (remoteDirectory == null) {
      throw new IllegalArgumentException("Remote directory should not be null");
    }
    remoteDirectory = normalizeURI(remoteDirectory);
    MaterialKey key = new MaterialKey(userName, projectName);
    try {
      remoteWriteLock.lock();
      materializeRemoteInternal(key, ownerName, groupName, permissions, remoteDirectory);
    } finally {
      remoteWriteLock.unlock();
    }
  }
  
  /**
   * Remote project *specific* certificates from *remote* filesystem
   *
   * @param userName Username of the user
   * @param projectName Name of the Project
   * @param remoteDirectory Remote directory the material was put to
   */
  public void removeCertificatesRemote(String userName, String projectName, String remoteDirectory) {
    if (remoteDirectory == null) {
      throw new IllegalArgumentException("Remote directory cannot be null");
    }
    remoteDirectory = normalizeURI(remoteDirectory);
    MaterialKey key = new MaterialKey(userName, projectName);
    try {
      remoteWriteLock.lock();
      removeRemoteInternal(key, remoteDirectory, false);
    } finally {
      remoteWriteLock.unlock();
    }
  }
  
  /**
   * Forcefully remove crypto material from *remote* filesystem regardless the references
   *
   * CAUTION: Other applications or Hopsworks instances might be still using them
   *
   * @param username Username of the user
   * @param projectName Name of the Project
   * @param remoteDirectory Remote directory the material was put to
   * @param bothProjectAndUser Remove both project specific and project generic material
   */
  public void forceRemoveRemoteMaterial(String username, String projectName, String remoteDirectory,
      boolean bothProjectAndUser) {
    if (remoteDirectory == null) {
      throw new IllegalArgumentException("Remote directory cannot be null");
    }
    remoteDirectory = normalizeURI(remoteDirectory);
    try {
      remoteWriteLock.lock();
      MaterialKey key = new MaterialKey(username, projectName);
      removeRemoteInternal(key, remoteDirectory, true);
      if (bothProjectAndUser) {
        key = new MaterialKey(null, projectName);
        removeRemoteInternal(key, remoteDirectory, true);
      }
    } finally {
      remoteWriteLock.unlock();
    }
  }
  
  /**
   * Get reference to project *generic* material. The certificates should have already been materialized
   *
   * @param projectName Name of the project
   * @return Reference to crypto material
   * @throws CryptoPasswordNotFoundException In case the material was not found in local store
   */
  public CryptoMaterial getUserMaterial(String projectName) throws CryptoPasswordNotFoundException {
    return getUserMaterial(null, projectName);
  }
  
  /**
   * Get reference to project *specific* material. The certificates should have already been materialized
   *
   * @param username Username of the user
   * @param projectName Name of the project
   * @return Reference to crypto material
   * @throws CryptoPasswordNotFoundException In case the material was not found in local store
   */
  public CryptoMaterial getUserMaterial(String username, String projectName) throws CryptoPasswordNotFoundException {
    MaterialKey key = new MaterialKey(username, projectName);
    try {
      localReadLock.lock();
      CryptoMaterial material = materialCache.get(key);
      if (material == null) {
        throw new CryptoPasswordNotFoundException("Cryptographic material for user <" + key.getExtendedUsername() + "" +
            " does not exist in the cache!");
      }
      return material;
    } finally {
      localReadLock.unlock();
    }
  }
  
  /**
   * Forcefully remove crypto material from *local* filesystem regardless the references
   *
   * CAUTION: Other applications might be still using them
   *
   * @param username Username of the user
   * @param projectName Name of the Project
   * @param materializationDirectory Local directory the material was put to
   * @param bothProjectAndUser Remove both project specific and project generic material
   */
  public void forceRemoveLocalMaterial(String username, String projectName, String materializationDirectory,
      boolean bothProjectAndUser) {
    forceRemoveLocalMaterial(username, projectName, materializationDirectory);
    if (bothProjectAndUser) {
      forceRemoveLocalMaterial(null, projectName, materializationDirectory);
    }
  }
  
  /**
   * Check if certificates have been materialized in the *local* filesystem in the directory specified
   *
   * @param username Username of the user
   * @param projectName Name of the project
   * @param directory Directory to check if the certificates have been materialized
   * @return True if the material exists in the cache, otherwise false
   */
  public boolean existsInLocalStore(String username, String projectName, String directory) {
    directory = directory != null ? directory : transientDir;
    MaterialKey key = new MaterialKey(username, projectName);
    try {
      localReadLock.lock();
      Bag materializedPaths = materializedCerts.get(key);
      if (materializedPaths == null) {
        return false;
      }
      
      return materializedPaths.contains(directory);
    } finally {
      localReadLock.unlock();
    }
  }
  
  /**
   * Check if certificates have been materialized in the *remote* filesystem in the directory specified
   *
   * @param username Username of the user
   * @param projectName Name of the project
   * @param remoteDirectory Directory to check if the certificates have been materialized
   * @return True if the material exists in the cache, otherwise false
   */
  public boolean existsInRemoteStore(String username, String projectName, String remoteDirectory) {
    if (remoteDirectory == null) {
      throw new IllegalArgumentException("Remote directory cannot be null");
    }
    MaterialKey key = new MaterialKey(username, projectName);
    RemoteMaterialRefID identifier = new RemoteMaterialRefID(key.getExtendedUsername(), remoteDirectory);
    RemoteMaterialReferences ref = remoteMaterialReferencesFacade.findById(identifier);
    
    return ref != null;
  }
  
  /*
   * End of Certificate materializer API
   */
  
  
  /*
   * This section provides methods for monitoring and control
   */
  
  /**
   * Returns the state of the CertificateMaterializer service
   * The state includes:
   * 1) Identifier of material, materialization directory and number of references for the certificates in the local
   * filesystem
   *
   * 2) Identifier of material, materialization directory and number of references for the certificates in the remote
   * filesystem (HDFS)
   *
   * 3) Identifier of the material that are scheduled to be removed from the local filesystem
   *
   * @return The state of the CertificateMaterializer at that point of time
   */
  @SuppressWarnings("unchecked")
  public MaterializerState<Map<String, Map<String, Integer>>, Map<String, Map<String, Integer>>,
      Map<String, Set<String>>> getState() {
    MaterializerState<Map<MaterialKey, Bag>, List<RemoteMaterialReferences>, Map<MaterialKey, Map<String, Runnable>>>
        state = getImmutableState();
    
    Map<MaterialKey, Bag> localMaterialState = state.getLocalMaterial();
    
    // <Username, <MaterialPath, NumberOfReferences>>
    Map<String, Map<String, Integer>> simpleLocalMaterialState = new HashMap<>(localMaterialState.size());
    
    for (Map.Entry<MaterialKey, Bag> entry : localMaterialState.entrySet()) {
      String username = entry.getKey().getExtendedUsername();
      Map<String, Integer> referencesMap = new HashMap<>();
      Bag pathsBag = entry.getValue();
      Set<String> paths = pathsBag.uniqueSet();
      for (String path : paths) {
        referencesMap.put(path, pathsBag.getCount(path));
      }
      simpleLocalMaterialState.put(username, referencesMap);
    }
    
    List<RemoteMaterialReferences> remoteMaterialState = state.getRemoteMaterial();
    // <Username, <MaterialPath, NumberOfReferences>>
    Map<String, Map<String, Integer>> simpleRemoteMaterialState = new HashMap<>(remoteMaterialState.size());
    
    for (RemoteMaterialReferences ref : remoteMaterialState) {
      String username = ref.getIdentifier().getUsername();
      Map<String, Integer> references = simpleRemoteMaterialState.get(username);
      if (references == null) {
        references = new HashMap<>();
        references.put(ref.getIdentifier().getPath(), ref.getReferences());
        simpleRemoteMaterialState.put(username, references);
      } else {
        references.put(ref.getIdentifier().getPath(), ref.getReferences());
      }
    }
    
    Map<MaterialKey, Map<String, Runnable>> fileRemovals = state.getScheduledRemovals();
    // <Username, [MaterialPath]>
    Map<String, Set<String>> simpleScheduledRemovals = new HashMap<>();
    
    for (Map.Entry<MaterialKey, Map<String, Runnable>> entry : fileRemovals.entrySet()) {
      String username = entry.getKey().getExtendedUsername();
      simpleScheduledRemovals.put(username, entry.getValue().keySet());
    }
    
    return new MaterializerState<>(simpleLocalMaterialState, simpleRemoteMaterialState, simpleScheduledRemovals);
  }
  
  private MaterializerState<Map<MaterialKey, Bag>, List<RemoteMaterialReferences>,
      Map<MaterialKey, Map<String, Runnable>>> getImmutableState() {
    Map<MaterialKey, Bag> localMaterial = null;
    Map<MaterialKey, Map<String, Runnable>> scheduledRemovals = null;
    List<RemoteMaterialReferences> remoteMaterial = null;
    try {
      localWriteLock.lock();
      localMaterial = MapUtils.unmodifiableMap(materializedCerts);
      scheduledRemovals = MapUtils.unmodifiableMap(fileRemovers);
    } finally {
      localWriteLock.unlock();
    }
    
    try {
      remoteWriteLock.lock();
      remoteMaterial = remoteMaterialReferencesFacade.findAll();
    } finally {
      remoteWriteLock.unlock();
    }
    
    return new MaterializerState(localMaterial, remoteMaterial, scheduledRemovals);
  }
  
  
  public class MaterializerState<T, S, R> {
    private final T localMaterial;
    private final S remoteMaterial;
    private final R scheduledRemovals;
    
    public MaterializerState(T localMaterial, S remoteMaterial, R scheduledRemovals) {
      this.localMaterial = localMaterial;
      this.remoteMaterial = remoteMaterial;
      this.scheduledRemovals = scheduledRemovals;
    }
    
    public T getLocalMaterial() {
      return localMaterial;
    }
    
    public S getRemoteMaterial() {
      return remoteMaterial;
    }
    
    public R getScheduledRemovals() {
      return scheduledRemovals;
    }
  }
  
  
  /*
   * Methods to keep track of open interpreters and when to materialize certificates
   */
  /**
   * It is called every time a paragraph is executed in Zeppelin. If the certificates for a Project has already been
   * materialized, this method will return false and they will not be materialized again.
   * @param projectId
   * @return True if it is the first time a paragraph is executed for that project. Otherwise false
   */
  public boolean openedInterpreter(Integer projectId) {
    return projectsWithOpenInterpreters.add(projectId);
  }
  
  /**
   * It is called only when a project has not running interpreters, thus it is safe to remove the certificates.
   * @param projectId ID of the project
   */
  public void closedInterpreter(Integer projectId) {
    projectsWithOpenInterpreters.remove(projectId);
  }
  
  /*
   * Materialize local section
   */
  private void materializeLocalInternal(MaterialKey key, String localDirectory) throws IOException {
    Bag materializedDirs = materializedCerts.get(key);
    if (materializedDirs == null) {
      // Check to see if there is any scheduled removal
      // If there is try to cancel it
      // If not possible materialize
    
      boolean shouldContinue = checkWithScheduledRemovalsLocal(key, localDirectory);
      if (shouldContinue) {
        // First time it was requested to be materialized
        // 1. Get certs fro DB
        CryptoMaterial material = getMaterialFromDatabase(key);
        // 2. Add them to L1 Cache
        materialCache.put(key, material);
        // 3. Write them to local FS
        flushToLocalFileSystem(key, material, localDirectory);
        // 4. Add Directory to Bag and then to materializedCerts
        Bag materialBag = new HashBag();
        String targetDir = localDirectory != null ? localDirectory : transientDir;
        materialBag.add(targetDir, 1);
        materializedCerts.put(key, materialBag);
      }
    } else {
      int cardinality = materializedDirs.getCount(localDirectory);
      if (cardinality == 0) {
        // Check to see if there is any scheduled removal
        // If there is try to cancel it
        // If not possible materialize
        boolean shouldContinue = checkWithScheduledRemovalsLocal(key, localDirectory);
      
        if (shouldContinue) {
          // First time for this directory, but not for the material in general
          // 1. Get byte material from L1 Cache. If not there, something went wrong
          // but fetch them from DB anyways
          CryptoMaterial material = materialCache.get(key);
          if (material == null) {
            material = getMaterialFromDatabase(key);
          }
          // 2. Flush buffers to local filesystem
          flushToLocalFileSystem(key, material, localDirectory);
          // 3. Increment cardinality
          materializedDirs.add(localDirectory, 1);
        }
      } else {
        // Materialization in this Directory has already been requested
        // 1. Increment cardinality for this Material and Directory
        materializedDirs.add(localDirectory, 1);
      }
    }
  }
  
  // Return true if materializeCertificates should proceed with the materialization
  private boolean checkWithScheduledRemovalsLocal(MaterialKey key, String materializationDirectory)
      throws IOException {
    Map<String, Runnable> materialRemovers = fileRemovers.get(key);
    if (materialRemovers == null) {
      return true;
    }
    
    LocalFileRemover localFileRemover = (LocalFileRemover) materialRemovers.get(materializationDirectory);
    if (localFileRemover == null) {
      return true;
    }
    
    boolean managedToCancel = localFileRemover.scheduledFuture.cancel(false);
    if (managedToCancel) {
      // Put back to L1 cache
      if (!materialCache.containsKey(key)) {
        if (localFileRemover.cryptoMaterial != null) {
          materialCache.put(key, localFileRemover.cryptoMaterial);
        } else {
          CryptoMaterial material = getMaterialFromDatabase(key);
          materialCache.put(key, material);
        }
      }
      // Put back to material map
      Bag materializeBag = materializedCerts.get(key);
      if (materializeBag != null) {
        materializeBag.add(materializationDirectory, 1);
      } else {
        Bag materializedBag = new HashBag();
        materializedBag.add(materializationDirectory);
        materializedCerts.put(key, materializedBag);
      }
  
      // Remove from scheduled removers
      materialRemovers.remove(materializationDirectory);
      if (materialRemovers.isEmpty()) {
        fileRemovers.remove(key);
      }
      return false;
    } else {
      forceRemoveLocalMaterial(key.username, key.projectName, materializationDirectory);
      return true;
    }
  }
  
  private void flushToLocalFileSystem(MaterialKey key, CryptoMaterial cryptoMaterial, String materializationDirectory)
      throws IOException {
    String targetDir = materializationDirectory != null ? materializationDirectory : transientDir;
    File keyStoreFile = Paths.get(targetDir, key.getExtendedUsername() + KEYSTORE_SUFFIX).toFile();
    File trustStoreFile = Paths.get(targetDir, key.getExtendedUsername() + TRUSTSTORE_SUFFIX).toFile();
    File passwordFile = Paths.get(targetDir, key.getExtendedUsername() + CERT_PASS_SUFFIX).toFile();
    
    FileUtils.writeByteArrayToFile(keyStoreFile, cryptoMaterial.getKeyStore().array(), false);
    FileUtils.writeByteArrayToFile(trustStoreFile, cryptoMaterial.getTrustStore().array(), false);
    FileUtils.write(passwordFile, new String(cryptoMaterial.getPassword()), false);
  }
  
  
  
  /*
   * Remove local section
   */
  private void removeLocal(MaterialKey key, String materializationDirectory) {
    Bag materialBag = materializedCerts.get(key);
    if (materialBag != null) {
      materialBag.remove(materializationDirectory, 1);
      if (materialBag.getCount(materializationDirectory) <= 0) {
        scheduleFileRemover(key, materializationDirectory);
      }
    }
  }
  
  private void scheduleFileRemover(MaterialKey key, String materializationDirectory) {
    LocalFileRemover fileRemover = new LocalFileRemover(key, materialCache.get(key), materializationDirectory);
    fileRemover.scheduledFuture = scheduler.schedule(fileRemover, DELAY_VALUE, DELAY_TIMEUNIT);
    
    Map<String, Runnable> materialRemovesForKey = fileRemovers.get(key);
    if (materialRemovesForKey != null) {
      materialRemovesForKey.put(materializationDirectory, fileRemover);
    } else {
      materialRemovesForKey = new HashMap<>();
      materialRemovesForKey.put(materializationDirectory, fileRemover);
      fileRemovers.put(key, materialRemovesForKey);
    }
    
    LOG.log(Level.FINEST, "Scheduled local file removal for <" + key.getExtendedUsername() + ">");
  }
  
  private void deleteMaterialFromLocalFs(MaterialKey key, String materializationDirectory) {
    File keyStoreFile = Paths.get(materializationDirectory, key.getExtendedUsername() + KEYSTORE_SUFFIX)
        .toFile();
    File trustStoreFile = Paths.get(materializationDirectory, key.getExtendedUsername() + TRUSTSTORE_SUFFIX)
        .toFile();
    File passwordFile = Paths.get(materializationDirectory, key.getExtendedUsername() + CERT_PASS_SUFFIX)
        .toFile();
    FileUtils.deleteQuietly(keyStoreFile);
    FileUtils.deleteQuietly(trustStoreFile);
    FileUtils.deleteQuietly(passwordFile);
  }
  
  private void forceRemoveLocalMaterial(String username, String projectName, String materializationDirectory) {
    try {
      localWriteLock.lock();
      materializationDirectory = materializationDirectory != null ? materializationDirectory : transientDir;
      MaterialKey key = new MaterialKey(username, projectName);
      // First remove from File Removers list
      Map<String, Runnable> materialRemovers = fileRemovers.get(key);
      if (materialRemovers != null) {
        LocalFileRemover fileRemover = (LocalFileRemover) materialRemovers.remove(materializationDirectory);
        if (fileRemover != null) {
          fileRemover.scheduledFuture.cancel(true);
        }
        if (materialRemovers.isEmpty()) {
          fileRemovers.remove(key);
        }
      }
      
      // Then remove from material Map and maybe from Cache
      Bag materialBag = materializedCerts.get(key);
      if (materialBag != null) {
        materialBag.remove(materializationDirectory);
        if (materialBag.isEmpty()) {
          materializedCerts.remove(key);
          CryptoMaterial material = materialCache.remove(key);
          if (material != null) {
            material.wipePassword();
          }
        }
      }
      
      // Then from local FS
      deleteMaterialFromLocalFs(key, materializationDirectory);
    } finally {
      localWriteLock.unlock();
    }
  }
  
  
  /*
   * Materialize remote section
   */
  private void materializeRemoteInternal(MaterialKey key, String ownerName, String groupName,
      FsPermission permissions, String remoteDirectory) throws IOException {
    
    RemoteMaterialReferences materialRef = null;
    RemoteMaterialRefID identifier = new RemoteMaterialRefID(key.getExtendedUsername(), remoteDirectory);
    
    int retries = 0;
    while (materialRef == null && retries < MAX_NUMBER_OF_RETRIES) {
      try {
        materialRef = remoteMaterialReferencesFacade.acquireLock(identifier, lock_id);
        
        // Managed to take the lock, proceed
        if (materialRef == null) {
          remoteMaterialReferencesFacade.createNewMaterialReference(identifier);
          materialRef = remoteMaterialReferencesFacade.acquireLock(identifier, lock_id);
          // First time request for this material in this directory
          // 1. Check if in cache otherwise fetch from DB
          CryptoMaterial material = materialCache.get(key);
          if (material == null) {
            material = getMaterialFromDatabase(key);
          }
          
          // 2. Upload to HDFS
          DistributedFileSystemOps dfso = distributedFsService.getDfsOps();
          try {
            Path keyStore = new Path(remoteDirectory + Path.SEPARATOR + key.getExtendedUsername()
                + KEYSTORE_SUFFIX);
            writeToHDFS(dfso, keyStore, material.getKeyStore().array());
            dfso.setOwner(keyStore, ownerName, groupName);
            dfso.setPermission(keyStore, permissions);
            
            Path trustStore = new Path(remoteDirectory + Path.SEPARATOR + key.getExtendedUsername()
                + TRUSTSTORE_SUFFIX);
            writeToHDFS(dfso, trustStore, material.getKeyStore().array());
            dfso.setOwner(trustStore, ownerName, groupName);
            dfso.setPermission(trustStore, permissions);
  
            Path passwordFile = new Path(remoteDirectory + Path.SEPARATOR + key.getExtendedUsername()
                + CERT_PASS_SUFFIX);
            writeToHDFS(dfso, passwordFile, new String(material.getPassword()));
            dfso.setOwner(passwordFile, ownerName, groupName);
            dfso.setPermission(passwordFile, permissions);
  
            // Cache should be flushed otherwise NN will raise permission exceptions
            dfso.flushCache(ownerName, groupName);
          } finally {
            if (dfso != null) {
              distributedFsService.closeDfsClient(dfso);
            }
          }
          
          // 3. Set the correct initial references and persist
          materialRef.setReferences(1);
          remoteMaterialReferencesFacade.update(materialRef);
        } else {
          materialRef.incrementReferences();
          remoteMaterialReferencesFacade.update(materialRef);
        }
      } catch (Exception ex) {
        if (ex instanceof AcquireLockException) {
          LOG.log(Level.WARNING, ex.getMessage(), ex);
          retries++;
          try {
            TimeUnit.MILLISECONDS.sleep(RETRY_WAIT_TIMEOUT);
          } catch (InterruptedException iex) {
            throw new IOException(iex);
          }
        } else {
          throw new IOException(ex);
        }
      } finally {
        try {
          remoteMaterialReferencesFacade.releaseLock(identifier, lock_id);
        } catch (AcquireLockException ex) {
          LOG.log(Level.SEVERE, "Cannot release lock for " + identifier, ex);
        }
      }
    }
    
    if (materialRef == null) {
      throw new IOException("Could not materialize certificates for " + key.getExtendedUsername()
          + " in remote directory " + remoteDirectory);
    }
  }
  
  
  private void writeToHDFS(DistributedFileSystemOps dfso, Path path, byte[] data) throws IOException {
    if (dfso == null) {
      throw new IOException("DistributedFilesystemOps is null");
    }
    FSDataOutputStream fsStream = dfso.getFilesystem().create(path);
    try {
      fsStream.write(data);
      fsStream.hflush();
    } finally {
      if (fsStream != null) {
        fsStream.close();
      }
    }
  }
  
  private void writeToHDFS(DistributedFileSystemOps dfso, Path path, String data) throws IOException {
    if (dfso == null) {
      throw new IOException("DistributedFilesystemOps is null");
    }
    FSDataOutputStream fsStream = dfso.getFilesystem().create(path);
    try {
      fsStream.writeUTF(data);
      fsStream.hflush();
    } finally {
      if (fsStream != null) {
        fsStream.close();
      }
    }
  }
  
  /*
   * Remove remote section
   */
  
  private void removeRemoteInternal(MaterialKey key, String remoteDirectory, boolean force) {
    RemoteMaterialReferences materialRef = null;
    RemoteMaterialRefID identifier = new RemoteMaterialRefID(key.getExtendedUsername(), remoteDirectory);
    
    int retries = 0;
    boolean deletedMaterial = false;
    while (materialRef == null && retries < MAX_NUMBER_OF_RETRIES) {
      try {
        if (force) {
          materialRef = new RemoteMaterialReferences(identifier);
        } else {
          materialRef = remoteMaterialReferencesFacade.acquireLock(identifier, lock_id);
        }
        
        if (materialRef != null) {
          materialRef.decrementReferences();
          
          if (materialRef.getReferences() <= 0 || force) {
            DistributedFileSystemOps dfso = distributedFsService.getDfsOps();
            try {
              dfso.rm(new Path(remoteDirectory), true);
            } catch (IOException ex) {
              LOG.log(Level.SEVERE, "Crypto material for <" + key.getExtendedUsername()
                  + "> could not be removed from HDFS. You SHOULD clean them manually!");
            }
            remoteMaterialReferencesFacade.delete(materialRef.getIdentifier());
            deletedMaterial = true;
          } else {
            materialRef.decrementReferences();
            remoteMaterialReferencesFacade.update(materialRef);
          }
        } else {
          LOG.log(Level.WARNING, "Could not find remote crypto material for " + key.getExtendedUsername() + " to " +
              "remove");
          break;
        }
      } catch (Exception ex) {
        if (ex instanceof AcquireLockException) {
          LOG.log(Level.WARNING, ex.getMessage(), ex);
          retries++;
          try {
            TimeUnit.MILLISECONDS.sleep(RETRY_WAIT_TIMEOUT);
          } catch (InterruptedException iex) {
            throw new IllegalStateException(iex);
          }
        } else {
          throw new RuntimeException(ex);
        }
      } finally {
        try {
          if (!deletedMaterial) {
            remoteMaterialReferencesFacade.releaseLock(identifier, lock_id);
          }
        } catch (AcquireLockException ex) {
          LOG.log(Level.SEVERE, "Cannot release lock for " + identifier, ex);
        }
      }
    }
  }
  
  private String normalizeURI(String uri) {
    Matcher uriMatcher = HDFS_SCHEME.matcher(uri);
    if (!uriMatcher.matches()) {
      uri = "hdfs://" + uri;
    }
    return uri;
  }
  
  private void deleteFromHDFS(DistributedFileSystemOps dfso, Path path) throws IOException {
    dfso.rm(path, false);
  }
  
  
  /*
   * Utility methods
   */
  private CryptoMaterial getMaterialFromDatabase(MaterialKey key) throws IOException {
    if (key.isProjectUser()) {
      ProjectGenericUserCerts projectGenericUserCerts = certsFacade.findProjectGenericUserCerts(key
          .getExtendedUsername());
      if (projectGenericUserCerts == null) {
        String msg = "Could not find certificates in the database for user <" + key.getExtendedUsername() + ">";
        LOG.log(Level.SEVERE, msg);
        throw new IOException(msg);
      }
      ByteBuffer keyStore = ByteBuffer.wrap(projectGenericUserCerts.getKey());
      ByteBuffer trustStore = ByteBuffer.wrap(projectGenericUserCerts.getCert());
      char[] password = decryptMaterialPassword(key.projectName, projectGenericUserCerts
          .getCertificatePassword(), ProjectGenericUserCerts.class);
      return new CryptoMaterial(keyStore, trustStore, password);
    }
  
    UserCerts projectSpecificCerts = certsFacade.findUserCert(key.projectName, key.username);
    ByteBuffer keyStore = ByteBuffer.wrap(projectSpecificCerts.getUserKey());
    ByteBuffer trustStore = ByteBuffer.wrap(projectSpecificCerts.getUserCert());
    char[] password = decryptMaterialPassword(key.getExtendedUsername(), projectSpecificCerts.getUserKeyPwd(),
        UserCerts.class);
    return new CryptoMaterial(keyStore, trustStore, password);
  }
  
  private <T> char[] decryptMaterialPassword(String certificateIdentifier, String encryptedPassword, Class<T> cls)
      throws IOException {
    String userPassword;
    if (ProjectGenericUserCerts.class == cls) {
      // Project generic certificates
      // Certificate Identifier will be the name of the Project
      Project project = projectFacade.findByName(certificateIdentifier);
      if (project == null) {
        String msg = "Project <" + certificateIdentifier + "> could not be found in the system";
        LOG.log(Level.SEVERE, msg);
        throw new IOException(msg);
      }
      Users owner = project.getOwner();
      userPassword = owner.getPassword();
    } else if (UserCerts.class == cls) {
      // Project specific certificates
      // Certificates Identifier will be the project specific username
      String username = hdfsUsersController.getUserName(certificateIdentifier);
      Users user = userFacade.findByUsername(username);
      if (user == null) {
        String msg = "Could not find user <" + certificateIdentifier + "> in the system";
        LOG.log(Level.SEVERE, msg);
        throw new IOException(msg);
      }
      userPassword = user.getPassword();
    } else {
      String msg = "Unknown certificate type: " + cls.getName();
      LOG.log(Level.SEVERE, msg);
      throw new IllegalArgumentException(msg);
    }
    try {
      String decryptedPassword = HopsUtils.decrypt(userPassword, encryptedPassword, certificatesMgmService
          .getMasterEncryptionPassword());
      return decryptedPassword.toCharArray();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Error while decrypting certificate password for user <" + certificateIdentifier + ">");
      throw new IOException(ex);
    }
  }
  
  
  public class CryptoMaterial {
    private final ByteBuffer keyStore;
    private final ByteBuffer trustStore;
    private final char[] password;
    
    public CryptoMaterial(ByteBuffer keyStore, ByteBuffer trustStore, char[] password) {
      this.keyStore = keyStore;
      this.trustStore = trustStore;
      this.password = password;
    }
  
    public ByteBuffer getKeyStore() {
      return keyStore;
    }
  
    public ByteBuffer getTrustStore() {
      return trustStore;
    }
  
    public char[] getPassword() {
      return password;
    }
    
    public void wipePassword() {
      for (int i = 0; i < password.length; i++) {
        password[i] = 0;
      }
    }
  }
  
  private class MaterialKey {
    private final String username;
    private final String projectName;
    private final boolean isProjectUser;
    
    private MaterialKey(String username, String projectName) {
      this.username = username;
      if (username == null) {
        this.isProjectUser = true;
      } else {
        this.isProjectUser = false;
      }
      this.projectName = projectName;
    }
    
    private boolean isProjectUser() {
      return isProjectUser;
    }
    
    private String getExtendedUsername() {
      if (isProjectUser) {
        return projectName + Settings.PROJECT_GENERIC_USER_SUFFIX;
      }
      return projectName + HdfsUsersController.USER_NAME_DELIMITER + username;
    }
  
    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
    
      if (other instanceof CertificateMaterializer.MaterialKey) {
        if (null != this.username && null != ((CertificateMaterializer.MaterialKey) other).username) {
          return this.username.equals(((CertificateMaterializer.MaterialKey) other).username)
              && this.projectName.equals(((CertificateMaterializer.MaterialKey) other).projectName);
        }
        return this.projectName.equals(((CertificateMaterializer.MaterialKey) other).projectName);
      }
    
      return false;
    }
  
    @Override
    public int hashCode() {
      int result = 17;
      if (null != username) {
        result = 31 * result + username.hashCode();
      }
      result = 31 * result + projectName.hashCode();
      return result;
    }
  }
  
  private class LocalFileRemover implements Runnable {
    private final MaterialKey key;
    private final CryptoMaterial cryptoMaterial;
    private final String materializationDirectory;
    private ScheduledFuture scheduledFuture;
    
    private LocalFileRemover(MaterialKey key, CryptoMaterial cryptoMaterial, String materializationDirectory) {
      this.key = key;
      this.cryptoMaterial = cryptoMaterial;
      this.materializationDirectory = materializationDirectory != null ? materializationDirectory : transientDir;
    }
    
    @Override
    public void run() {
      deleteMaterialFromLocalFs(key, materializationDirectory);
      Map<String, Runnable> materialRemovers = fileRemovers.get(key);
      if (materialRemovers != null) {
        materialRemovers.remove(materializationDirectory);
        if (materialRemovers.isEmpty()) {
          fileRemovers.remove(key);
        }
  
        // No more references to that crypto material, wipe out password
        try {
          localWriteLock.lock();
          Bag materialBag = materializedCerts.get(key);
          if (materialBag.isEmpty()) {
            materializedCerts.remove(key);
            CryptoMaterial material = materialCache.remove(key);
            if (material != null) {
              material.wipePassword();
            }
          }
        } finally {
          localWriteLock.unlock();
        }
        
        LOG.log(Level.FINEST, "Deleted crypto material for <" + key.getExtendedUsername() + "> from directory "
            + materializationDirectory);
      }
    }
  }
}
