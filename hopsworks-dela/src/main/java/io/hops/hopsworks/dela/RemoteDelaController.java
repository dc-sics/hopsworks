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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import org.javatuples.Triplet;

@Stateless
public class RemoteDelaController {

  private final static Logger LOG = Logger.getLogger(RemoteDelaController.class.getName());

  @Resource
  TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtlr;

  private boolean ready = false;
  private KeyStore keystore;
  private KeyStore truststore;
  private String keystorePassword;

  @PostConstruct
  public void init() {
    timerService.createTimer(0, settings.getHOPSSITE_HEARTBEAT_RETRY(), "Timer for dela settings check.");
  }
  
  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void setup(Timer timer) {
    if (delaStateCtlr.hopsworksDelaSetup()) {
      Optional<Triplet<KeyStore, KeyStore, String>> certSetup = CertificateHelper.initKeystore(settings);
      if (certSetup.isPresent()) {
        ready = true;
        keystore = certSetup.get().getValue0();
        truststore = certSetup.get().getValue1();
        keystorePassword = certSetup.get().getValue2();

        timer.cancel();
      }
    }
  }

  private void checkReady() throws ThirdPartyException {
    if (!ready) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "service unavailable",
        ThirdPartyException.Source.SETTINGS, "certificates not ready");
    }
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

  private ClientWrapper getClient(String delaClusterAddress, String path, Class resultClass) {
    return ClientWrapper.httpsInstance(keystore, truststore, keystorePassword,
      HopssiteController.HopsSiteHostnameVerifier.INSTANCE, resultClass).setTarget(delaClusterAddress).setPath(path);
  }

  public static class Path {

    public static String readme(String publicDSId) {
      return "/remote/dela/datasets/" + publicDSId + "/readme";
    }
  }
}
