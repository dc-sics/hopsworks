package io.hops.hopsworks.dela.cluster;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dataset.DatasetController;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterDatasetController {

  private Logger logger = Logger.getLogger(ClusterDatasetController.class.getName());

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private DatasetFacade datasetFacade;

  public Dataset shareWithCluster(Dataset dataset) {
    if (dataset.isPublicDs()) {
      return dataset;
    }
    dataset.setPublicDsState(Dataset.SharedState.CLUSTER);
    dataset.setEditable(false);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }

  public Dataset unshareFromCluster(Dataset dataset) {
    if (!dataset.isPublicDs()) {
      return dataset;
    }
    dataset.setPublicDsState(Dataset.SharedState.PRIVATE);
    dataset.setEditable(true);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }
  
  public List<Dataset> getPublicDatasets() {
    return datasetFacade.findAllDatasetsByState(Dataset.SharedState.CLUSTER.state, false);
  }
}
