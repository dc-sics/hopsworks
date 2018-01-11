package io.hops.hopsworks.common.dao.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnApplicationstateFacade extends AbstractFacade<YarnApplicationstate> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnApplicationstateFacade() {
    super(YarnApplicationstate.class);
  }

  @Override
  public List<YarnApplicationstate> findAll() {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findAll",
            YarnApplicationstate.class);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppname(String appname) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppname",
            YarnApplicationstate.class).setParameter(
                    "appname", appname);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppuserAndAppState(String appUser,
          String appState) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppuserAndAppsmstate",
            YarnApplicationstate.class).setParameter("appuser", appUser).
            setParameter("appsmstate", appState);
    return query.getResultList();
  }

  public YarnApplicationstate findByAppId(String appId) {
    try {
      return em.createNamedQuery("YarnApplicationstate.findByApplicationid",
              YarnApplicationstate.class).setParameter(
                      "applicationid", appId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }

  }
}
