package se.kth.hopsworks.workflows;

import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.internal.jpa.JPAQuery;
import org.json.JSONObject;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.Map;

@Stateless
public class EdgeFacade extends AbstractCrudFacade<Edge> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Edge> findAll() {
        TypedQuery<Edge> query = em.createNamedQuery("Edge.findAll",
                Edge.class);
        return query.getResultList();
    }

    public DynamicEntity findByIdDynamic(EdgePK id){
        TypedQuery<DynamicEntity> query =  em.createNamedQuery("Edge.findById", DynamicEntity.class);
        query.setParameter("edgePK", id);
        return query.getSingleResult();
    }

    public Edge findById(EdgePK id) {
        TypedQuery<Edge> query =  em.createNamedQuery("Edge.findById", Edge.class);
        query.setParameter("edgePK", id);
        return query.getSingleResult();
    }

    public EdgeFacade() {
        super(Edge.class);
    }

    public void persist(Edge edge) {
        em.persist(edge);
    }

    public void flush() {
        em.flush();
    }

    public Predicate updateWhere(Edge edge, Root<Edge> root){
        return em.getCriteriaBuilder().equal(root.get("edgePK"), edge.getEdgePK());
    }

    public Edge merge(Edge edge) {
        return em.merge(edge);

    }

    public void remove(Edge edge) {
        em.remove(em.merge(edge));
    }

    public Edge refresh(Edge edge) {
        Edge e = findById(edge.getEdgePK());
        em.refresh(e);
        return e;
    }
}
