package io.hops.hopsworks.common.dao.user.cluster;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ClusterCertFacade extends AbstractFacade<ClusterCert> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ClusterCertFacade() {
    super(ClusterCert.class);
  }

  public ClusterCert getByOrgUnitNameAndOrgName(String orgName, String UnitName) {
    return null;
  }

  public ClusterCert getBySerialNumber(Integer serialNum) {
    return null;
  }

  public List<ClusterCert> getByAgent() {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findAll", ClusterCert.class);
    return query.getResultList();
  }
}
