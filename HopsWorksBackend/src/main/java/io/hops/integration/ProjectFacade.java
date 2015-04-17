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
 * @author André & Ermias
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

    public void removeByProjectID(int projectID) {
        Project proj = em.find(Project.class, projectID);
        if (proj != null) {
            em.remove(proj);
        }
    }

    public void updateByProject(Project project) {
        em.merge(project);
    }
    
    public void createProject(ProjectUser projUser, Project project) {
        em.persist(projUser);
        em.persist(project);
    }

    public void detach(Project project) {
        em.detach(project);
    }

}
