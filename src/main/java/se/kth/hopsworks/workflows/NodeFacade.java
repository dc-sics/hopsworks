package se.kth.hopsworks.workflows;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

@Stateless
public class NodeFacade extends AbstractCrudFacade<Node> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Node> findAll() {
        TypedQuery<Node> query = em.createNamedQuery("Node.findAll",
                Node.class);
        return query.getResultList();
    }

    public Node findById(NodePK id) {
        TypedQuery<Node> query =  em.createNamedQuery("Node.findById", Node.class);
        query.setParameter("nodePK", id);
        return query.getSingleResult();
    }

    public NodeFacade() {
        super(Node.class);
    }

    public void persist(Node node) throws ClassNotFoundException{
        String className = LOWER_HYPHEN.to(UPPER_CAMEL, node.getType());
        Class nodeClass = Class.forName("se.kth.hopsworks.workflows.nodes." + className);
        em.persist(nodeClass.cast(node));
    }

    public void flush() {
        em.flush();
    }

    public Node merge(Node node) {
        return em.merge(node);

    }

    public Predicate updateWhere(Node node, Root<Node> root){
        return em.getCriteriaBuilder().equal(root.get("nodePK"), node.getNodePK());
    }

    public void remove(Node node) {
        em.remove(em.merge(node));
    }

    public Node refresh(Node node) {
        Node n = findById(node.getNodePK());
        em.refresh(n);
        return n;
    }
}
