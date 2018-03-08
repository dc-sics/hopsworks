package io.hops.hopsworks.common.dao.project.pia;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import java.util.Collection;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PiaFacade extends AbstractFacade<Pia> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public PiaFacade() {
    super(Pia.class);
  }

  public Pia findByProject(Project project) {
    Collection<Pia> allPias = project.getPiaCollection();
    if (allPias == null || allPias.isEmpty()) {
      Pia p = new Pia();
      p.setProjectId(project);
    }
    return allPias.iterator().next();
  }
  
}
