package io.hops.hopsworks.common.dao.device;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class DeviceFacade3 {

  private final static Logger logger = Logger.getLogger(DeviceFacade3.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void createProjectDevicesSettings(ProjectDevicesSettings projectDevicesSettings){
    if (projectDevicesSettings != null){
      em.persist(projectDevicesSettings);
    }
  }

  public void createProjectDevicesSettings(
    Integer projectId, String projectSecret, Integer projectTokenDurationInHours) {
    em.persist(new ProjectDevicesSettings(projectId, projectSecret, projectTokenDurationInHours));
  }

  public ProjectDevicesSettings readProjectDevicesSettings(Integer projectId) {
    TypedQuery<ProjectDevicesSettings> query = em.createNamedQuery(
      "ProjectDevicesSettings.findByProjectId", ProjectDevicesSettings.class);
    query.setParameter("projectId", projectId);
    return query.getSingleResult();
  }

  public void updateProjectDevicesSettings(
    Integer projectId, String projectSecret, Integer projectTokenDurationInHours) {
    //TODO: Implementation
  }

  public void deleteProjectDevicesSettings(Integer projectId) {
    ProjectDevicesSettings projectDevicesSettings = readProjectDevicesSettings(projectId);
    if (projectDevicesSettings != null){
      em.remove(projectDevicesSettings);
    }
  }

  public void createProjectDevice(ProjectDevice2 projectDevice) {
    if (projectDevice != null){
      em.persist(projectDevice);
    }
  }

  public void createProjectDevice(AuthProjectDeviceDTO authDTO) {
    if (authDTO != null){
      ProjectDevicePK pdKey = new ProjectDevicePK(authDTO.getProjectId(), authDTO.getDeviceUuid());
      em.persist(new ProjectDevice2(pdKey, authDTO.getPassword(), ProjectDevice2.State.Pending, authDTO.getAlias()));
    }
  }

  public ProjectDevice2 readProjectDevice(Integer projectId, String deviceUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    TypedQuery<ProjectDevice2> query = em.createNamedQuery(
      "ProjectDevice.findByProjectDevicePK", ProjectDevice2.class);
    query.setParameter("projectDevicePK", pdKey);
    return query.getSingleResult();
  }

  public List<ProjectDeviceDTO> readProjectDevices(Integer projectId) {
    TypedQuery<ProjectDevice2> query = em.createNamedQuery(
      "ProjectDevice.findByProjectId", ProjectDevice2.class);
    query.setParameter("projectId", projectId);
    List<ProjectDevice2> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice2 device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
              device.getProjectDevicePK().getProjectId(),
              device.getProjectDevicePK().getDeviceUuid(),
              device.getAlias(),
              device.getCreatedAt(),
              device.getState().name(),
              device.getLastLoggedIn()));
    }
    return devicesDTO;
  }

  public List<ProjectDeviceDTO> readProjectDevices(Integer projectId, Integer state) {
    TypedQuery<ProjectDevice2> query = em.createNamedQuery(
      "ProjectDevice.findByProjectIdAndState", ProjectDevice2.class);
    query.setParameter("projectId", projectId);
    query.setParameter("state", state);
    List<ProjectDevice2> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice2 device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
        device.getProjectDevicePK().getProjectId(),
        device.getProjectDevicePK().getDeviceUuid(),
        device.getAlias(),
        device.getCreatedAt(),
        device.getState().name(),
        device.getLastLoggedIn()));
    }
    return devicesDTO;
  }

  public void updateProjectDevice(ProjectDeviceDTO projectDeviceDTO) {
    ProjectDevice2 projectDevice = em.find(ProjectDevice2.class,
      new ProjectDevicePK(projectDeviceDTO.getProjectId(), projectDeviceDTO.getDeviceUuid()));
    projectDevice.setState(ProjectDevice2.State.valueOf(projectDeviceDTO.getState()));
    projectDevice.setAlias(projectDeviceDTO.getAlias());
    em.persist(projectDevice);
  }

  public void updateProjectDeviceLastLoggedIn(ProjectDevice2 projectDevice) {
    projectDevice.setLastLoggedIn(new Date());
    em.persist(projectDevice);
  }

  public void updateProjectDevices(List<ProjectDeviceDTO> projectDevicesDTO) {
    for (ProjectDeviceDTO projectDeviceDTO: projectDevicesDTO){
      updateProjectDevice(projectDeviceDTO);
    }
  }

  public void deleteProjectDevice(ProjectDeviceDTO projectDeviceDTO) {
    if (projectDeviceDTO!= null){
      ProjectDevice2 projectDevice = readProjectDevice(
        projectDeviceDTO.getProjectId(), projectDeviceDTO.getDeviceUuid());
      if (projectDevice != null){
        em.remove(projectDevice);
      }
    }
  }





}
