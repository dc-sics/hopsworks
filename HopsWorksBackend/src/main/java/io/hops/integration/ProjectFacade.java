package io.hops.integration;

import io.hops.model.Project;
import io.hops.model.ProjectUser;
import io.hops.model.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author AMore
 */
@Stateless
public class ProjectFacade extends AbstractFacade<Project> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProjectFacade() {
        super(Project.class);
    }

public List<Project> findAllByUser(Users user) {
        List<Project> query = em.createNamedQuery("Project.findAllByUser", Project.class).setParameter("email", user).getResultList();
        return query;
    }

    public Project findByProjectID(int projectID) {
        Project proj = em.find(Project.class, projectID);
        return proj;
    }
     
    public boolean removeByProjectID(int projectID){
        try {
            Project proj = em.find(Project.class, projectID);
            em.remove(proj);        
            return true;
        } catch (Exception e) {
            return false;
        }        
    }
    
    public boolean updateByProject(Project project){
        try {
            em.merge(project);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean createProject(ProjectUser projUser, Project project){
        try {            
            em.persist(projUser);
            em.persist(project);
            return true; 
        } catch (Exception e) {
            return false;
        }
    }
    
    
    
    public void detach(Project project){
        em.detach(project);
    }

}
