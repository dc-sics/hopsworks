/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hdfs;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class HdfsLeDescriptorsFacade extends AbstractFacade<HdfsLeDescriptors> {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public HdfsLeDescriptorsFacade() {
        super(HdfsLeDescriptors.class);
    }

    /**
     * HdfsLeDescriptors.hostname returns the hostname + port for the Leader NN (e.g., "127.0.0.1:8020")
     * @return 
     */
    public HdfsLeDescriptors findEndpoint() {
        try {
//            return em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getSingleResult();
            List<HdfsLeDescriptors> res = em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getResultList();
            if (res.isEmpty()) {
              return null;
            } else {
              return res.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
    }
    /**
     * 
     * @return "ip:port" for the first namenode found in the table.
     */
    public String getSingleEndpoint() {
      HdfsLeDescriptors hdfs = findEndpoint();
      if (hdfs == null) {
        return "";
      }
      return hdfs.getHostname();
    }
    
}
