package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ServiceStatusFacade extends AbstractFacade<ServiceStatus> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ServiceStatusFacade() {
    super(ServiceStatus.class);
  }

  public List<ServiceStatus> findByInitiator(Users user) {
    TypedQuery<ServiceStatus> query = em.createNamedQuery("ServiceStatus.findByInitiator", ServiceStatus.class);
    query.setParameter("initiator", user);

    return query.getResultList();
  }

  public List<ServiceStatus> findByTarget(Users user) {
    TypedQuery<ServiceStatus> query = em.createNamedQuery("ServiceStatus.findByTarget", ServiceStatus.class);
    query.setParameter("target", user);

    return query.getResultList();
  }

}
