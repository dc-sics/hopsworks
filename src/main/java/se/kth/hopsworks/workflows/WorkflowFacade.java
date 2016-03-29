package se.kth.hopsworks.workflows;

import org.json.JSONObject;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateless
public class WorkflowFacade extends AbstractFacade<Workflow> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WorkflowFacade() {
        super(Workflow.class);
    }

    @Override
    public List<Workflow> findAll() {
        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.findAll",
                Workflow.class);
        return query.getResultList();
    }

    public Workflow findById(Integer id) {
        return em.find(Workflow.class, id);
    }

    public List<Workflow> findByName() {
        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.findByName",
                Workflow.class);
        return query.getResultList();
    }

    public void persist(Workflow workflow) {
        em.persist(workflow);

    }

    public Workflow merge(Workflow workflow) {
        return em.merge(workflow);

    }

    public void remove(Workflow workflow) {
        em.remove(em.merge(workflow));
    }

    public void flush() {
        em.flush();
    }

    public Workflow refresh(Workflow workflow) {
        Workflow w = findById(workflow.getId());
        em.refresh(w);
        return w;
    }

    public void update(Workflow workflow, JSONObject params) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<Workflow> update = cb.createCriteriaUpdate(Workflow.class);
        Root<Workflow> e = update.from(Workflow.class);

        update.where(cb.equal(e.get("id"), workflow.getId()));

        for (Object k: params.keySet()){
            String key = k.toString();
            update.set(key, params.get(key));
        }
        em.createQuery(update).executeUpdate();
    }
}
