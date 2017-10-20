package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.util.ClientWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.util.CertificateHelper;
import java.security.KeyStore;
import java.util.Optional;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import org.javatuples.Triplet;

@Stateless
public class RemoteDelaController {

  private final static Logger LOG = Logger.getLogger(RemoteDelaController.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtlr;

  private void checkReady() throws ThirdPartyException {
    delaStateCtlr.checkHopsworksDelaSetup();
  }

  //********************************************************************************************************************
  public FilePreviewDTO readme(String publicDSId, ClusterAddressDTO source) throws ThirdPartyException {
    checkReady();
    try {
      ClientWrapper client = getClient(source.getDelaClusterAddress(), Path.readme(publicDSId), FilePreviewDTO.class);
      LOG.log(Settings.DELA_DEBUG, "dela:cross:readme {0}", client.getFullPath());
      FilePreviewDTO result = (FilePreviewDTO) client.doGet();
      LOG.log(Settings.DELA_DEBUG, "dela:cross:readme:done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ex) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "communication fail",
        ThirdPartyException.Source.REMOTE_DELA, source.toString());
    }
  }

  private ClientWrapper getClient(String delaClusterAddress, String path, Class resultClass) 
    throws ThirdPartyException {
    Optional<Triplet<KeyStore, KeyStore, String>> certSetup = CertificateHelper.initKeystore(settings);
    if(!certSetup.isPresent()) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "service unavailable",
        ThirdPartyException.Source.SETTINGS, "certificates not ready");
    }
    KeyStore keystore = certSetup.get().getValue0();
    KeyStore truststore = certSetup.get().getValue1();
    String keystorePassword = certSetup.get().getValue2();
    return ClientWrapper.httpsInstance(keystore, truststore, keystorePassword,
      new HopssiteController.HopsSiteHostnameVerifier(settings), 
      resultClass).setTarget(delaClusterAddress).setPath(path);
  }

  public static class Path {

    public static String readme(String publicDSId) {
      return "/remote/dela/datasets/" + publicDSId + "/readme";
    }
  }
}
