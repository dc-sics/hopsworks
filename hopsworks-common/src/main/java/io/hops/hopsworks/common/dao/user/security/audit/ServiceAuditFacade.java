package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ServiceAuditFacade extends AbstractFacade<ServiceAudit> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ServiceAuditFacade() {
    super(ServiceAudit.class);
  }

  public List<ServiceAudit> findByInitiator(Users user) {
    TypedQuery<ServiceAudit> query = em.createNamedQuery("ServiceAudit.findByInitiator", ServiceAudit.class);
    query.setParameter("initiator", user);

    return query.getResultList();
  }

  public List<ServiceAudit> findByTarget(Users user) {
    TypedQuery<ServiceAudit> query = em.createNamedQuery("ServiceAudit.findByTarget", ServiceAudit.class);
    query.setParameter("target", user);

    return query.getResultList();
  }

}
