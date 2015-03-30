/*
 */
package io.hops.integration;

import io.hops.model.Groups;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Ermias
 */
public class GroupFacade extends AbstractFacade<Groups>{
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public GroupFacade() {
        super(Groups.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public Groups findByGroupName(String name){
        return em.createNamedQuery("Groups.findByGroupName", Groups.class)
                         .setParameter("groupName", name).getSingleResult();    
    }
}
