package io.hops.integration;

import io.hops.model.Project;
import io.hops.model.ProjectUser;
import io.hops.model.Users;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author André & Ermias
 */
@Stateless
public class ProjectUserFacade extends AbstractFacade<ProjectUser>{

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProjectUserFacade() {
        super(ProjectUser.class);
    }
    
    
    public String findRoleByID(Users user, Project projectID){
        
        ProjectUser pu = em.createNamedQuery("ProjectUser.findRoleByEmailAndID", ProjectUser.class)
                .setParameter("email", user)
                .setParameter("projectId", projectID)
                .getSingleResult();
        
        return pu.getRole().getName();
    }
    
    
    
}
