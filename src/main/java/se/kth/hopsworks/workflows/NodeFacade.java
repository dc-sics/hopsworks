package se.kth.hopsworks.workflows;

import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

@Stateless
public class NodeFacade extends AbstractFacade<Node> {

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

    public void persist(Node node){
        Date date = new Date();
        Workflow workflow = node.getWorkflow();
        workflow.setUpdatedAt(date);
        node.setCreatedAt(date);
        em.persist(node);
        em.merge(workflow);
    }

    public void flush() {
        em.flush();
    }

    public Node merge(Node node) {
        Date date = new Date();
        Workflow workflow = node.getWorkflow();
        node.setUpdatedAt(date);
        workflow.setUpdatedAt(date);
        em.merge(workflow);
        return em.merge(node);

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
