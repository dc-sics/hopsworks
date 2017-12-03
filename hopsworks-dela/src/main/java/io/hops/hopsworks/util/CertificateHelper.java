package io.hops.hopsworks.util;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificate;
import io.hops.hopsworks.common.dao.dela.certs.ClusterCertificateFacade;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.io.FileUtils;
import org.javatuples.Triplet;

public class CertificateHelper {

  private final static Logger LOG = Logger.getLogger(CertificateHelper.class.getName());

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromFile(String masterPswd, Settings settings,
    ClusterCertificateFacade certFacade, CertificatesMgmService certificatesMgmService) {
    String certPath = settings.getHopsSiteCert();
    String intermediateCertPath = settings.getHopsSiteIntermediateCert();
    String keystorePath = settings.getHopsSiteKeyStorePath();
    String truststorePath = settings.getHopsSiteTrustStorePath();
    try {
      String certPswd = HopsUtils.randomString(64);
      String encryptedCertPswd = HopsUtils.encrypt(masterPswd, certPswd,
          certificatesMgmService.getMasterEncryptionPassword());
      File certFile = readFile(certPath);
      File intermediateCertFile = readFile(intermediateCertPath);
      String clusterName = getClusterName(certFile);
      settings.setHopsSiteClusterName(clusterName);
      generateKeystore(certFile, intermediateCertFile, certPswd, settings);
      File keystoreFile = readFile(keystorePath);
      File truststoreFile = readFile(truststorePath);
      KeyStore keystore, truststore;
      try (FileInputStream keystoreIS = new FileInputStream(keystoreFile);
        FileInputStream truststoreIS = new FileInputStream(truststoreFile)) {
        keystore = keystore(keystoreIS, certPswd);
        truststore = keystore(truststoreIS, certPswd);
      }
      try (FileInputStream keystoreIS = new FileInputStream(keystoreFile);
        FileInputStream truststoreIS = new FileInputStream(truststoreFile)) {
        certFacade.saveClusterCerts(clusterName, ByteStreams.toByteArray(keystoreIS),
          ByteStreams.toByteArray(truststoreIS), encryptedCertPswd);
      }
      return Optional.of(Triplet.with(keystore, truststore, certPswd));
    } catch (Exception ex) {
      settings.deleteHopsSiteClusterName();
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex.getMessage());
      return Optional.empty();
    } finally {
      FileUtils.deleteQuietly(new File(keystorePath));
      FileUtils.deleteQuietly(new File(truststorePath));
    }
  }

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromDB(String masterPswd, String clusterName,
    ClusterCertificateFacade certFacade, CertificatesMgmService certificatesMgmService) {
    try {
      Optional<ClusterCertificate> cert = certFacade.getClusterCert(clusterName);
      if (!cert.isPresent()) {
        return Optional.empty();
      }
      String certPswd = HopsUtils.decrypt(masterPswd, cert.get().getCertificatePassword(),
          certificatesMgmService.getMasterEncryptionPassword());
      KeyStore keystore, truststore;
      try (ByteArrayInputStream keystoreIS = new ByteArrayInputStream(cert.get().getClusterKey());
        ByteArrayInputStream truststoreIS = new ByteArrayInputStream(cert.get().getClusterCert())) {
        keystore = keystore(keystoreIS, certPswd);
        truststore = keystore(truststoreIS, certPswd);
      }
      return Optional.of(Triplet.with(keystore, truststore, certPswd));
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex.getMessage());
      return Optional.empty();
    }
  }

  private static void generateKeystore(File cert, File intermediateCert, String certPswd, Settings settings)
    throws IllegalStateException {
    if (!isCertSigned(cert, intermediateCert)) {
      throw new IllegalStateException("Certificate is not signed");
    }
    try {
      LocalhostServices.generateHopsSiteKeystore(settings, certPswd);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "keystore generate ex. {0}", ex.getMessage());
      throw new IllegalStateException("keystore generate ex", ex);
    }
  }

  private static String getClusterName(File certFile) throws IllegalStateException {
    X509Certificate cert = getX509Cert(certFile);
    String o = getCertificatePart(cert, "O");
    String ou = getCertificatePart(cert, "OU");
    String clusterName = o + "_" + ou;
    return clusterName;
  }

  private static X509Certificate getX509Cert(File cert) throws IllegalStateException {
    try (InputStream inStream = new FileInputStream(cert)) {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      X509Certificate x509Cert = (X509Certificate) factory.generateCertificate(inStream);
      return x509Cert;
    } catch (CertificateException | IOException ex) {
      LOG.log(Level.SEVERE, "cert ex {0}", ex);
      throw new IllegalStateException("cert ex", ex);
    }
  }

  private static boolean isCertSigned(File certFile, File intermediateCertFile) throws IllegalStateException {
    X509Certificate cert = getX509Cert(certFile);
    X509Certificate caCert = getX509Cert(intermediateCertFile);
    String intermediateSubjectDN = caCert.getSubjectDN().getName();
    String issuerDN = cert.getIssuerDN().getName();
    LOG.log(Level.INFO, "sign check: {0} {1}", new Object[]{issuerDN, intermediateSubjectDN});
    return issuerDN.equals(intermediateSubjectDN);
  }

  private static File readFile(String certPath) throws IllegalStateException {
    File certFile = new File(certPath);
    if (!certFile.exists()) {
      LOG.log(Level.SEVERE, "Could not find file:{0}", certPath);
      throw new IllegalStateException("Could not find file");
    }
    return certFile;
  }

  private static KeyStore keystore(InputStream is, String certPswd) throws IllegalStateException {
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(is, certPswd.toCharArray());
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
      throw new IllegalStateException("keystore ex", ex);
    }
  }

  public static String getCertificatePart(X509Certificate cert, String partName) {
    String tmpName, name = "";
    X500Principal principal = cert.getSubjectX500Principal();
    String part = partName + "=";
    int start = principal.getName().indexOf(part);
    if (start > -1) {
      tmpName = principal.getName().substring(start + part.length());
      int end = tmpName.indexOf(",");
      if (end > 0) {
        name = tmpName.substring(0, end);
      } else {
        name = tmpName;
      }
    }
    return name.toLowerCase();
  }
}
