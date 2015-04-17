/*
 */
package io.hops.integration;

import io.hops.model.Project;
import io.hops.model.ProjectHistory;
import io.hops.model.Users;
import java.sql.SQLException;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class ProjectHistoryFacade extends AbstractFacade<ProjectHistory> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public ProjectHistoryFacade() {
        super(ProjectHistory.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<ProjectHistory> findAll() {
        TypedQuery<ProjectHistory> query = em.createNamedQuery("ProjectHistory.findAll",
                ProjectHistory.class);
        return query.getResultList();
    }

    public List<ProjectHistory> findAllByUser(Users user) {
        TypedQuery<ProjectHistory> query = em.createNamedQuery("ProjectHistory.findHistoryByEmail",
                ProjectHistory.class).setParameter("email", user.getEmail());
        return query.getResultList();
    }

    public List<ProjectHistory> findAllByProject(Project project) {
        TypedQuery<ProjectHistory> query = em.createNamedQuery("ProjectHistory.findHistoryByProject",
                ProjectHistory.class).setParameter("email", project.getId());
        return query.getResultList();
    }

    public void persist(ProjectHistory history){
        em.persist(history);
        em.flush();
    }

    public void update(ProjectHistory history) {
        em.merge(history);
    }

    @Override
    public void remove(ProjectHistory history) {
        if (history != null && em.contains(history)) {
            em.remove(history);
        }
    }

    public void detach(ProjectHistory history) {
        em.detach(history);
    }

}
