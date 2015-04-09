package io.hops.integration;

import io.hops.model.ProjectRole;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author André & Ermias
 */
@Stateless
public class ProjectRoleFacade  extends AbstractFacade<ProjectRole> {
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProjectRoleFacade() {
        super(ProjectRole.class);
    }
    
    public ProjectRole getUserRoleByName(String roleName ){
        return em.find(ProjectRole.class, roleName);
    }    
    
}
