package io.hops.integration;

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
    
    
    public String findRoleByID(String email, String id){
        Users user = em.find(Users.class, email);

        ProjectUser pu = em.createNamedQuery("ProjectUser.findRoleByEmailAndID", ProjectUser.class)
                .setParameter("email", user)
                .setParameter("id", Integer.valueOf(id))
                .getSingleResult();
        
        return pu.getRole().getName();
    }
    
    
    
}
