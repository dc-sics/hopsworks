/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.integration;

import io.hops.model.ProjectType;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author AMore
 */
@Stateless
public class ProjectTypeFacade extends AbstractFacade<ProjectType> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProjectTypeFacade() {
        super(ProjectType.class);
    }
    
    public void persist(ProjectType projType){
        em.persist(projType);
    }
}
