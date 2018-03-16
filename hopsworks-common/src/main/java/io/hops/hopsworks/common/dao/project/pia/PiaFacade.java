package io.hops.hopsworks.common.dao.project.pia;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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

  public Pia findByProject(int projectId) {
    TypedQuery<Pia> q = em.createNamedQuery("Pia.findByProjectId", Pia.class);
    q.setParameter("projectId", projectId);
    List<Pia> allPias = q.getResultList();
    if (allPias == null || allPias.isEmpty()) {
      Pia p = new Pia();
      p.setProjectId(projectId);
      allPias = new ArrayList<>();
      allPias.add(p);
    }
    return allPias.iterator().next();
  }  
  
//  public Pia findByProject(Project project) {
//    Collection<Pia> allPias = project.getPiaCollection();
//    if (allPias == null || allPias.isEmpty()) {
//      Pia p = new Pia();
//      p.setProjectId(project);
//      allPias = new ArrayList<>();
//      allPias.add(p);
//    }
//    return allPias.iterator().next();
//  }
  public Pia mergeUpdate(Pia pia, int projectId) {
    Pia piaManaged = findByProject(projectId);
    pia.setProjectId(projectId);
    piaManaged.deepCopy(pia);
    return update(piaManaged);
  }
}
