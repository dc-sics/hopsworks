package io.hops.hopsworks.common.dao.device;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class DeviceFacade {

  private final static Logger logger = Logger.getLogger(DeviceFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void addProjectDevice(Integer projectId, String deviceUuid, String passUuid, String alias) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    ProjectDevice pd = new ProjectDevice(pdKey, passUuid, ProjectDevice.State.PENDING, alias);
    em.persist(pd);
  }

  public ProjectDevice getProjectDevice(Integer projectId, String deviceUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectDevicePK", ProjectDevice.class);
    query.setParameter("projectDevicePK", pdKey);
    return query.getSingleResult();
  }

  public List<ProjectDeviceDTO> getProjectDevices(Integer projectId) {
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectId", ProjectDevice.class);
    query.setParameter("projectId", projectId);
    List<ProjectDevice> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
              device.getProjectDevicePK().getProjectId(),
              device.getProjectDevicePK().getDeviceUuid(),
              device.getAlias(),
              device.getCreatedAt(),
              device.getState(),
              device.getLastProduced()));
    }
    return devicesDTO;
  }

  public List<ProjectDeviceDTO> getProjectDevices(Integer projectId, Integer state) {
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectIdAndState", ProjectDevice.class);
    query.setParameter("projectId", projectId);
    query.setParameter("state", state);
    List<ProjectDevice> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
        device.getProjectDevicePK().getProjectId(),
        device.getProjectDevicePK().getDeviceUuid(),
        device.getAlias(),
        device.getCreatedAt(),
        device.getState(),
        device.getLastProduced()));
    }
    return devicesDTO;
  }

  public void updateDeviceState(ProjectDeviceDTO projectDeviceDTO) {
    ProjectDevice projectDevice = em.find(ProjectDevice.class,
      new ProjectDevicePK(projectDeviceDTO.getProjectId(), projectDeviceDTO.getDeviceUuid()));
    projectDevice.setState(projectDeviceDTO.getState());
    em.persist(projectDevice);
  }

  public void updateDevicesState(List<ProjectDeviceDTO> projectDevicesDTO) {
    for (ProjectDeviceDTO projectDeviceDTO: projectDevicesDTO){
      ProjectDevice projectDevice = em.find(ProjectDevice.class,
        new ProjectDevicePK(projectDeviceDTO.getProjectId(), projectDeviceDTO.getDeviceUuid()));
      projectDevice.setState(projectDeviceDTO.getState());
      em.persist(projectDevice);
    }
  }


  public void addProjectSecret(Integer projectId, String projectSecret, Integer projectTokenDurationInHours) {
    em.persist(new ProjectSecret(projectId, projectSecret,
        projectTokenDurationInHours));
  }

  public ProjectSecret getProjectSecret(Integer projectId) {
    TypedQuery<ProjectSecret> query = em.createNamedQuery(
        "ProjectSecret.findByProjectId",
        ProjectSecret.class);
    query.setParameter("projectId", projectId);
    return query.getSingleResult();
  }

}
