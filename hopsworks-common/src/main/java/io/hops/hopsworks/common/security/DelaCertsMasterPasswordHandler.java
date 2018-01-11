package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.util.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for clusters certificates for Dela when the master encryption password changes.
 * If something goes wrong during update, the old passwords are kept in the database.
 * The handler needs to register with the CertificateMgmService.
 * @see CertificatesMgmService#init()
 */
public class DelaCertsMasterPasswordHandler implements CertificatesMgmService
    .MasterPasswordChangeHandler<ClusterCertificateFacade> {
  private final Logger LOG = Logger.getLogger(DelaCertsMasterPasswordHandler.class.getName());
  private final Map<String, String> oldPasswordsForRollback;
  private final Settings settings;
  
  private ClusterCertificateFacade clusterCertificateFacade;
  
  DelaCertsMasterPasswordHandler(Settings settings) {
    this.settings = settings;
    oldPasswordsForRollback = new HashMap<>();
  }
  
  @Override
  public void setFacade(ClusterCertificateFacade certsFacade) {
    this.clusterCertificateFacade = certsFacade;
  }
  
  @Override
  public List<String> handleMasterPasswordChange(String oldMasterPassword, String newMasterPassword)
      throws EncryptionMasterPasswordException {
    List<String> updatedCertsName = new ArrayList<>();
    
    Optional<List<ClusterCertificate>> maybe = clusterCertificateFacade.getAllClusterCerts();
    if (maybe.isPresent()) {
      String mapKey = null, oldPassword, newEncCertPassword;
      try {
        for (ClusterCertificate cert : maybe.get()) {
          mapKey = cert.getClusterName();
          oldPassword = cert.getCertificatePassword();
          oldPasswordsForRollback.putIfAbsent(mapKey, oldPassword);
          newEncCertPassword = getNewUserPassword(settings.getHopsSiteClusterPswd().get(), oldPassword,
              oldMasterPassword, newMasterPassword);
          cert.setCertificatePassword(newEncCertPassword);
          clusterCertificateFacade.saveClusterCerts(cert);
          updatedCertsName.add(mapKey);
        }
      } catch (Exception ex) {
        String errorMsg = "Something went wrong while updating master encryption password for Cluster Certificates. " +
            "Cluster certificate provoked the error was: " + mapKey;
        LOG.log(Level.SEVERE, errorMsg + " rolling back...", ex);
        throw new EncryptionMasterPasswordException(errorMsg);
      }
    }
    return updatedCertsName;
  }
  
  @Override
  public void rollback() {
    LOG.log(Level.FINE, "Rolling back");
    for (Map.Entry<String, String> cert : oldPasswordsForRollback.entrySet()) {
      String key = cert.getKey();
      String value = cert.getValue();
      Optional<ClusterCertificate> optional = clusterCertificateFacade.getClusterCert(key);
      if (optional.isPresent()) {
        ClusterCertificate cc = optional.get();
        cc.setCertificatePassword(value);
        clusterCertificateFacade.saveClusterCerts(cc);
      }
    }
    oldPasswordsForRollback.clear();
  }
}
