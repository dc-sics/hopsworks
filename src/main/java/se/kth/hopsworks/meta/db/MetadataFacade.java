package se.kth.hopsworks.meta.db;

import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.entity.MetadataPK;
import se.kth.hopsworks.meta.exception.DatabaseException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;

/**
 * @author vangelis
 */
@Stateless
public class MetadataFacade extends AbstractFacade<Metadata> {

  private static final Logger logger = Logger.getLogger(MetadataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MetadataFacade() {
    super(Metadata.class);
  }

  public Metadata getMetadata(MetadataPK metadataPK) throws DatabaseException {

    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findByPrimaryKey",
            Metadata.class);
    q.setParameter("metadataPK", metadataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Metadata getMetadataById(int id) {

    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findById", Metadata.class);
    q.setParameter("id", id);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'meta_data' table. MetaData is the object that's
   * going to be persisted/updated in the database
   * <p/>
   *
   * @param metadata
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void addMetadata(Metadata metadata) throws DatabaseException {

    try {
      Metadata m = this.contains(metadata) ? metadata : this.getMetadata(
              metadata.getMetadataPK());

      if (m != null && m.getMetadataPK().getTupleid() != -1
              && m.getMetadataPK().getFieldid() != -1) {
        /*
         * if the row exists just update it.
         */
        m.copy(metadata);
        this.em.merge(m);
      } else {
        /*
         * if the row is new then just persist it
         */
        m = metadata;
        this.em.persist(m);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(e.getMessage(), e);
    }
  }
  
   /**
   * Delete a record from 'meta_data' table. 
   * <p/>
   *
   * @param metadata
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void removeMetadata(Metadata metadata) throws DatabaseException {
    try {
      Metadata m = this.contains(metadata) ? metadata : this.getMetadata(
              metadata.getMetadataPK());

      if (m != null && m.getMetadataPK().getTupleid() != -1
              && m.getMetadataPK().getFieldid() != -1) {
        /*
         * if the row exists just delete it.
         */
        m.copy(metadata);
        this.em.remove(m);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(e.getMessage(), e);
    }
  }
  

  /**
   * Checks if a raw data instance is a managed entity
   * <p/>
   *
   * @param metadata
   * @return
   */
  public boolean contains(Metadata metadata) {
    return this.em.contains(metadata);
  }
}
