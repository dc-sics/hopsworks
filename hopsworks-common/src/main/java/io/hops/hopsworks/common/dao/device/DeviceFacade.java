package io.hops.hopsworks.common.dao.device;

import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class DeviceFacade {

  private final static Logger LOG = Logger.getLogger(DeviceFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void registerDevice(Integer projectId, String deviceUuid, String passUuid, String projectUserUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    ProjectDevice pd = new ProjectDevice(pdKey, passUuid, projectUserUuid);
    em.persist(pd);	
  }
  
  public void addProjectUserUuid(Integer projectId, String userEmail, String projectUserUuid) {
    ProjectUserUuidPK puKey = new ProjectUserUuidPK(projectId, userEmail);
    ProjectUserUuid pu = new ProjectUserUuid(puKey, projectUserUuid);
    em.persist(pu); 
  }

  public void removeProjectUserUuid(Integer projectId, String userEmail) {
    ProjectUserUuidPK puKey = new ProjectUserUuidPK(projectId, userEmail);
    ProjectUserUuid pu = null;
    //TODO find pu
    em.remove(pu);
    
  }

}
