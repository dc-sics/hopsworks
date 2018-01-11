package io.hops.hopsworks.common.dao.ndb;

import java.io.Serializable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NdbBackupFacade implements Serializable {

  private static final Logger logger = Logger.getLogger(
          NdbBackupFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public NdbBackup find(Integer id) {
    return em.find(NdbBackup.class, id);
  }

  public List<NdbBackup> findAll() {
    TypedQuery<NdbBackup> query = this.em.
            createNamedQuery("NdbBackup.findAll", NdbBackup.class);

    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persistBackup(NdbBackup backup) {
    em.persist(backup);
  }

  public void removeBackup(int id) {
    NdbBackup backup = find(id);
    if (backup != null) {
      em.remove(backup);
    }
  }

  public NdbBackup findHighestBackupId() {
    TypedQuery<NdbBackup> query = this.em.
            createNamedQuery("NdbBackup.findHighestBackupId", NdbBackup.class);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
