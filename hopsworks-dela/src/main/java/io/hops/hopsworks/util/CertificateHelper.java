package io.hops.hopsworks.util;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dao.certs.ClusterCertificate;
import io.hops.hopsworks.dela.dao.certs.ClusterCertificateFacade;
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

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromFile(String certPswd, Settings settings,
    ClusterCertificateFacade certFacade) {
    String certPath = settings.getHopsSiteCert();
    String caCertPath = settings.getHopsSiteCaCert();
    String keystorePath = settings.getHopsSiteKeyStorePath();
    String truststorePath = settings.getHopsSiteTrustStorePath();
    try {
      File certFile = readFile(certPath);
      File caCertFile = readFile(caCertPath);
      String clusterName = getClusterName(certFile);
      settings.setHopsSiteClusterName(clusterName);
      generateKeystore(certFile, caCertFile, certPswd, settings);
      File keystoreFile = readFile(keystorePath);
      File truststoreFile = readFile(truststorePath);
      try (FileInputStream keystoreIS = new FileInputStream(keystoreFile);
        FileInputStream truststoreIS = new FileInputStream(truststoreFile)) {
        KeyStore keystore = keystore(keystoreIS, certPswd);
        KeyStore truststore = keystore(truststoreIS, certPswd);
        certFacade.saveClusterCerts(clusterName, ByteStreams.toByteArray(keystoreIS),
          ByteStreams.toByteArray(truststoreIS), certPswd);
        FileUtils.deleteQuietly(new File(certPath));
        FileUtils.deleteQuietly(new File(caCertPath));
        return Optional.of(Triplet.with(keystore, truststore, certPswd));
      } catch (IOException ex) {
        settings.deleteHopsSiteClusterName();
        LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
        throw new IllegalStateException("keystore ex", ex);
      }
    } catch (IllegalStateException ex) {
      settings.deleteHopsSiteClusterName();
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
      return Optional.empty();
    } finally {
      FileUtils.deleteQuietly(new File(keystorePath));
      FileUtils.deleteQuietly(new File(truststorePath));
    }
  }

  public static Optional<Triplet<KeyStore, KeyStore, String>> loadKeystoreFromDB(String clusterName,
    ClusterCertificateFacade certFacade) {
    try {
      Optional<ClusterCertificate> cert = certFacade.getClusterCert(clusterName);
      if(!cert.isPresent()) {
        return Optional.empty();
      }
      try (ByteArrayInputStream keystoreIS = new ByteArrayInputStream(cert.get().getClusterKey());
        ByteArrayInputStream truststoreIS = new ByteArrayInputStream(cert.get().getClusterCert())) {
        KeyStore keystore = keystore(keystoreIS, cert.get().getCertificatePassword());
        KeyStore truststore = keystore(truststoreIS, cert.get().getCertificatePassword());
        return Optional.of(Triplet.with(keystore, truststore, cert.get().getCertificatePassword()));
      } catch (IOException | IllegalStateException ex) {
        LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
        return Optional.empty();
      }
    } catch (IllegalStateException ex) {
      LOG.log(Level.SEVERE, "keystore ex. {0}", ex);
      return Optional.empty();
    }
  }

  private static void generateKeystore(File cert, File caCert, String certPswd, Settings settings)
    throws IllegalStateException {
    if (!isCertSigned(cert, caCert)) {
      throw new IllegalStateException("Certificate is not signed");
    }
    try {
      LocalhostServices.generateHopsSiteKeystore(settings, certPswd);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not generate keystore");
      throw new IllegalStateException("Could not generate keystore");
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

  private static boolean isCertSigned(File certFile, File caCertFile) throws IllegalStateException {
    String hopsRootCA;
    X509Certificate cert = getX509Cert(certFile);
    X509Certificate caCert = getX509Cert(caCertFile);
    hopsRootCA = caCert.getIssuerDN().getName();
    String issuerdn = cert.getIssuerDN().getName();
    return issuerdn.equals(hopsRootCA);
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
