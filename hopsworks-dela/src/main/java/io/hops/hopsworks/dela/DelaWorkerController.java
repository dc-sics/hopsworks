package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.ExtendedDetails;
import io.hops.hopsworks.dela.old_dto.HDFSEndpoint;
import io.hops.hopsworks.dela.old_dto.HDFSResource;
import io.hops.hopsworks.dela.old_dto.HdfsDetails;
import io.hops.hopsworks.dela.old_dto.HopsDatasetDetailsDTO;
import io.hops.hopsworks.dela.old_dto.KafkaDetails;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.KafkaResource;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import io.hops.hopsworks.dela.util.DatasetHelper;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksTransferDTO;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopsSiteController;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaWorkerController {

  private Logger LOG = Logger.getLogger(DelaWorkerController.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private TransferDelaController delaCtrl;
  @EJB
  private HopsSiteController hopsSiteCtrl;
  @EJB
  private DelaDatasetController delaDatasetCtrl;
  @EJB
  private DelaHdfsController delaHdfsCtrl;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  public String publishDataset(Project project, Dataset dataset, Users user)
    throws ThirdPartyException {

    if (dataset.isPublicDs()) {
      return dataset.getPublicDsId();
    }
    if (dataset.isShared()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(),
        "dataset shared - can only publish owned datasets", ThirdPartyException.Source.LOCAL, "bad request");
    }
    String publicDSId = createPublicDSId(project.getName(), dataset.getName());
    if (settings.isDelaEnabled()) {
      delaHdfsCtrl.writeManifest(project, dataset, user);
      delaCtrlUpload(project, dataset, user, publicDSId);
      long datasetSize = delaHdfsCtrl.datasetSize(project, dataset, user);
      try {
        hopsSiteCtrl.performAsUser(user, new HopsSite.UserFunc<String>() {
          @Override
          public String perform() throws ThirdPartyException {
            return hopsSiteCtrl.publish(publicDSId, dataset.getName(), dataset.getDescription(), getCategories(),
                    datasetSize, user.getEmail());
          }
        });
      } catch (ThirdPartyException tpe) {
        if (ThirdPartyException.Source.HOPS_SITE.equals(tpe.getSource())
                && ThirdPartyException.Error.DATASET_EXISTS.is(tpe.getMessage())) {
          //TODO ask dela to checksum it;
        }
        throw tpe;
      }
    }
    delaDatasetCtrl.upload(dataset, publicDSId);
    LOG.log(Level.INFO, "{0} published", publicDSId);
    return publicDSId;
  }

  private void delaCtrlUpload(Project project, Dataset dataset, Users user, String publicDSId)
    throws ThirdPartyException {
    String datasetPath = DatasetHelper.getDatasetPath(project, dataset);
    HDFSResource resource = new HDFSResource(datasetPath, Settings.MANIFEST_FILE);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(dataset.getName(), project.getId(), dataset.getId());
    delaCtrl.upload(publicDSId, details, resource, endpoint);
  }

  public void cancel(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    if (!dataset.isPublicDs()) {
      return;
    }
    if (settings.isDelaEnabled()) {
      delaCtrl.cancel(dataset.getPublicDsId());
      delaHdfsCtrl.deleteManifest(project, dataset, user);
      hopsSiteCtrl.cancel(dataset.getPublicDsId());
    }
    delaDatasetCtrl.cancel(dataset);
  }
  
  public void cancelAndClean(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    cancel(project, dataset, user);
    delaDatasetCtrl.delete(project, dataset);
  }

  public ManifestJSON startDownload(Project project, Users user, HopsworksTransferDTO.Download downloadDTO)
    throws ThirdPartyException {
    Dataset dataset = delaDatasetCtrl.download(project, user, downloadDTO.getPublicDSId(), downloadDTO.getName());
    try {
      delaCtrlStartDownload(project, dataset, user, downloadDTO);
    } catch (ThirdPartyException tpe) {
      delaDatasetCtrl.delete(project, dataset);
      throw tpe;
    }

    ManifestJSON manifest = delaHdfsCtrl.readManifest(project, dataset, user);
    delaDatasetCtrl.updateDescription(dataset, manifest.getDatasetDescription());
    return manifest;
  }

  private void delaCtrlStartDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO) throws ThirdPartyException {
    String datasetPath = DatasetHelper.getDatasetPath(project, dataset);
    HDFSResource resource = new HDFSResource(datasetPath, "manifest.json");
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(downloadDTO.getName(), project.getId(), dataset.getId());
    delaCtrl.startDownload(downloadDTO.getPublicDSId(), details, resource, endpoint, downloadDTO.getBootstrap());
  }

  public void advanceDownload(Project project, Dataset dataset, Users user, HopsworksTransferDTO.Download downloadDTO,
    String sessionId, KafkaEndpoint kafkaEndpoint) throws ThirdPartyException {

    delaCtrlAdvanceDownload(project, dataset, user, downloadDTO, sessionId, kafkaEndpoint);
    hopsSiteCtrl.download(downloadDTO.getPublicDSId());
  }

  private void delaCtrlAdvanceDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO, String sessionId, KafkaEndpoint kafkaEndpoint)
    throws ThirdPartyException {
    String datasetPath = DatasetHelper.getDatasetPath(project, dataset);
    JSONObject fileTopics = new JSONObject(downloadDTO.getTopics());
    LinkedList<HdfsDetails> hdfsResources = new LinkedList<>();
    LinkedList<KafkaDetails> kafkaResources = new LinkedList<>();

    Iterator<String> iter = fileTopics.keys();
    while (iter.hasNext()) {
      String key = iter.next();
      try {
        String value = (String) fileTopics.get(key);
        if (!value.equals("") && kafkaEndpoint != null) {
          kafkaResources.add(new KafkaDetails(key, new KafkaResource(sessionId, value)));
        }
        hdfsResources.add(new HdfsDetails(key, new HDFSResource(datasetPath, key)));
      } catch (JSONException e) {
        // Something went wrong!
      }
    }
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint hdfsEndpoint = new HDFSEndpoint(getHDFSXmlPath(), hdfsUser);
    ExtendedDetails details = new ExtendedDetails(hdfsResources, kafkaResources);
    delaCtrl.advanceDownload(downloadDTO.getPublicDSId(), hdfsEndpoint, kafkaEndpoint, details);
  }

  //********************************************************************************************************************
  private String createPublicDSId(String projectName, String datasetName) {
    return Settings.getPublicDatasetId(settings.getDELA_CLUSTER_ID(), projectName, datasetName);
  }

  private Collection<String> getCategories() {
    Set<String> categories = new HashSet<>();
    return categories;
  }

  private String getHDFSXmlPath() {
    return settings.getHadoopConfDir() + File.separator + Settings.DEFAULT_HADOOP_CONFFILE_NAME;
  }
}