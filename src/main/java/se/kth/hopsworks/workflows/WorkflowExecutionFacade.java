package se.kth.hopsworks.workflows;

import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class WorkflowExecutionFacade extends AbstractFacade<WorkflowExecution> {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WorkflowExecutionFacade() {
        super(WorkflowExecution.class);
    }

    public void flush() {
        em.flush();
    }

}
