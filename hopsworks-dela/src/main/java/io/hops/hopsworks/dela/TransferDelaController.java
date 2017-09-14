package io.hops.hopsworks.dela;

import com.google.gson.Gson;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.util.ClientWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.ExtendedDetails;
import io.hops.hopsworks.dela.old_dto.HDFSEndpoint;
import io.hops.hopsworks.dela.old_dto.HDFSResource;
import io.hops.hopsworks.dela.old_dto.HopsContentsReqJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.dela.old_dto.HopsDatasetDetailsDTO;
import io.hops.hopsworks.dela.old_dto.HopsTorrentAdvanceDownload;
import io.hops.hopsworks.dela.old_dto.HopsTorrentStartDownload;
import io.hops.hopsworks.dela.old_dto.HopsTorrentUpload;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.SuccessJSON;
import io.hops.hopsworks.dela.old_dto.TorrentExtendedStatusJSON;
import io.hops.hopsworks.dela.old_dto.TorrentId;
import io.hops.hopsworks.util.SettingsHelper;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import org.javatuples.Pair;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TransferDelaController {

  private Logger logger = Logger.getLogger(TransferDelaController.class.getName());
  @EJB
  Settings settings;

  public AddressJSON getDelaPublicEndpoint(String delaVersion) throws ThirdPartyException {
    String delaTransferHttpEndpoint = SettingsHelper.delaTransferHttpEndpoint(settings);
    try {
      ClientWrapper<AddressJSON> rc = ClientWrapper
        .httpInstance(AddressJSON.class)
        .setTarget(delaTransferHttpEndpoint)
        .setPath(TransferDelaPaths.CONTACT)
        .setPayload(delaVersion);
      logger.log(Settings.DELA_DEBUG, "dela:contact {0}", rc.getFullPath());
      AddressJSON result = rc.doPost();
      logger.log(Settings.DELA_DEBUG, "dela:contact - done {0} {1}", new Object[]{rc.getFullPath(), result.getIp()});
      return result;
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela:contact - communication fail{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public void upload(String publicDSId, HopsDatasetDetailsDTO datasetDetails, HDFSResource resource,
    HDFSEndpoint endpoint) throws ThirdPartyException {
    logger.log(Settings.DELA_DEBUG, "{0} upload - transfer");
    HopsTorrentUpload reqContent = new HopsTorrentUpload(new TorrentId(publicDSId), datasetDetails.getDatasetName(),
      datasetDetails.getProjectId(), datasetDetails.getDatasetId(), resource, endpoint);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/upload/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public void startDownload(String publicDSId, HopsDatasetDetailsDTO datasetDetails, HDFSResource resource,
    HDFSEndpoint endpoint, List<ClusterAddressDTO> bootstrap)
    throws ThirdPartyException {

    List<AddressJSON> bootstrapAdr = new LinkedList<>();
    Gson gson = new Gson();
    for(ClusterAddressDTO b : bootstrap) {
      bootstrapAdr.add(gson.fromJson(b.getDelaTransferAddress(), AddressJSON.class));
    }
    HopsTorrentStartDownload reqContent = new HopsTorrentStartDownload(new TorrentId(publicDSId), datasetDetails.
      getDatasetName(), datasetDetails.getProjectId(), datasetDetails.getDatasetId(), resource, bootstrapAdr, endpoint);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/download/start/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public void advanceDownload(String publicDSId, HDFSEndpoint hdfsEndpoint, KafkaEndpoint kafkaEndpoint,
    ExtendedDetails details)
    throws ThirdPartyException {

    HopsTorrentAdvanceDownload reqContent = new HopsTorrentAdvanceDownload(new TorrentId(publicDSId),
      kafkaEndpoint, hdfsEndpoint, details);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/download/advance/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public void cancel(String publicDSId) throws ThirdPartyException {
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/stop")
        .setPayload(new TorrentId(publicDSId));
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public HopsContentsSummaryJSON.Contents getContents(List<Integer> projectIds) throws ThirdPartyException {
    HopsContentsReqJSON reqContent = new HopsContentsReqJSON(projectIds);
    try {
      ClientWrapper<HopsContentsSummaryJSON.JsonWrapper> rc = ClientWrapper
        .httpInstance(HopsContentsSummaryJSON.JsonWrapper.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("library/hopscontents")
        .setPayload(reqContent);
      HopsContentsSummaryJSON.Contents result = rc.doPost().resolve();
      return result;
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  public TorrentExtendedStatusJSON details(TorrentId torrentId) throws ThirdPartyException {
    try {
      ClientWrapper<TorrentExtendedStatusJSON> rc = ClientWrapper
        .httpInstance(TorrentExtendedStatusJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("/library/extended")
        .setPayload(torrentId);
      TorrentExtendedStatusJSON result = rc.doPost();
      return result;
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), ise.getMessage(),
        ThirdPartyException.Source.DELA, "communication fail");
    }
  }

  /**
   * @return <upldDS, dwnlDS>
   */
  public Pair<List<String>, List<String>> getContents() throws ThirdPartyException {
    HopsContentsSummaryJSON.Contents contents = TransferDelaController.this.getContents(new LinkedList<>());
    List<String> upldDSIds = new LinkedList<>();
    List<String> dwnlDSIds = new LinkedList<>();
    for (ElementSummaryJSON[] ea : contents.getContents().values()) {
      for (ElementSummaryJSON e : ea) {
        if (e.getTorrentStatus().toLowerCase().equals("uploading")) {
          upldDSIds.add(e.getTorrentId().getVal());
        } else if (e.getTorrentStatus().toLowerCase().equals("downloading")) {
          dwnlDSIds.add(e.getTorrentId().getVal());
        }
      }
    }
    return Pair.with(upldDSIds, dwnlDSIds);
  }

}
