package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.WebCommunication;
import java.util.Arrays;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
import javax.ws.rs.core.Response;

@Stateless
public class HostServicesFacade {

  @EJB
  private WebCommunication web;
  @EJB
  private HostsFacade hostEJB;

  final static Logger logger = Logger.getLogger(HostServicesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HostServicesFacade() {
  }

  public List<HostServices> findAll() {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findAll", HostServices.class);
    return query.getResultList();
  }

  public List<HostServices> findGroupServices(String group) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Group", HostServices.class).
        setParameter("group", group);
    return query.getResultList();
  }

  public List<HostServices> findGroups(String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Group-Service", HostServices.class)
        .setParameter("group", group).setParameter("service", service);
    return query.getResultList();
  }

  public List<String> findClusters() {
    TypedQuery<String> query = em.createNamedQuery("HostServices.findClusters",
        String.class);
    return query.getResultList();
  }

  public List<String> findGroups(String cluster) {
    TypedQuery<String> query = em.
        createNamedQuery("HostServices.findGroupsBy-Cluster", String.class)
        .setParameter("cluster", cluster);
    return query.getResultList();
  }

//  public List<String> findServices() {
//    TypedQuery<String> query = em.createNamedQuery("HostServices.findServices",
//        String.class);
//    return query.getResultList();
//  }

  public List<HostServices> findServiceOnHost(String hostname, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findOnHost", HostServices.class)
        .setParameter("hostname", hostname).setParameter("group", group).setParameter("service", service);
    return query.getResultList();
  }

  public HostServices find(String hostname, String cluster, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
        .setParameter("hostname", hostname).setParameter("cluster", cluster)
        .setParameter("group", group).setParameter("service", service);
    List results = query.getResultList();
    if (results.isEmpty()) {
      return null;
    } else if (results.size() == 1) {
      return (HostServices) results.get(0);
    }
    throw new NonUniqueResultException();
  }

  public List<HostServices> findHostServiceByHostname(String hostname) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Hostname",
        HostServices.class)
        .setParameter("hostname", hostname);
    return query.getResultList();
  }

  public List<HostServices> findServices(String cluster, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery(
        "HostServices.findBy-Cluster-Group-Service", HostServices.class)
        .setParameter("cluster", cluster).setParameter("group", group).
        setParameter("service", service);
    return query.getResultList();
  }

  public List<HostServices> findServices(String service) {
    TypedQuery<HostServices> query = em.createNamedQuery(
        "HostServices.findBy-Service", HostServices.class)
        .setParameter("service", service);
    return query.getResultList();
  }

  public Long count(String cluster, String group, String service) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count", Long.class)
        .setParameter("cluster", cluster).setParameter("group", group)
        .setParameter("service", service);
    return query.getSingleResult();
  }

  public Long countHosts(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count-hosts", Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long countServices(String cluster, String service) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count-services", Long.class)
        .setParameter("cluster", cluster).setParameter("service", service);
    return query.getSingleResult();
  }

  public Long totalCores(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalCores",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalMemoryCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalMemoryCapacity",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalDiskCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalDiskCapacity",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public List<HostServicesInfo> findHostServicesByCluster(String cluster) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster", HostServicesInfo.class)
        .setParameter("cluster", cluster);
    return query.getResultList();
  }

  public List<HostServicesInfo> findHostServicesByGroup(String cluster, String group) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster-Group", HostServicesInfo.class)
        .setParameter("cluster", cluster).setParameter("group", group);
    return query.getResultList();
  }

  public List<HostServicesInfo> findHostServices(String cluster, String group,
      String service) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster-Group-Service", HostServicesInfo.class)
        .setParameter("cluster", cluster).setParameter("group", group)
        .setParameter("service", service);
    return query.getResultList();
  }

  public HostServicesInfo findHostServices(String cluster, String group, String service,
      String hostname) throws Exception {
    TypedQuery<HostServicesInfo> query = em.
        createNamedQuery("HostServices.findHostServicesBy-Cluster-Group-Service-Host",
            HostServicesInfo.class)
        .setParameter("cluster", cluster).setParameter("group", group)
        .setParameter("service", service).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      throw new Exception("NoResultException");
    }
  }

  public String findCluster(String ip, int webPort) {
    TypedQuery<String> query = em.createNamedQuery(
        "HostServices.find.ClusterBy-Ip.WebPort", String.class)
        .setParameter("ip", ip);
    return query.getSingleResult();
  }

  public String findPrivateIp(String cluster, String hostname, int webPort) {
    TypedQuery<String> query = em.createNamedQuery(
        "HostServices.find.PrivateIpBy-Cluster.Hostname.WebPort", String.class)
        .setParameter("cluster", cluster).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public void persist(HostServices hostService) {
    em.persist(hostService);
  }

  public void store(HostServices service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
        .setParameter("hostname", service.getHost().getHostname()).setParameter("cluster",
        service.getCluster())
        .setParameter("group", service.getGroup()).setParameter("service",
        service.getService());
    List<HostServices> s = query.getResultList();

    if (s.size() > 0) {
      service.setId(s.get(0).getId());
      em.merge(service);
    } else {
      em.persist(service);
    }
  }

  public void deleteServicesByHostname(String hostname) {
    em.createNamedQuery("HostServices.DeleteBy-HostId").setParameter("hostname", hostname).
        executeUpdate();
  }

  public String serviceOp(String group, String serviceName, Action action) throws AppException {
    return webOp(action, findGroups(group, serviceName));
  }

  public String serviceOp(String service, Action action) throws AppException {
    return webOp(action, findGroupServices(service));
  }

  public String serviceOnHostOp(String group, String serviceName, String hostname,
      Action action) throws AppException {
    return webOp(action, findServiceOnHost(hostname, group, serviceName));
  }

  private String webOp(Action operation, List<HostServices> services) throws AppException {
    if (operation == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "The action is not valid, valid action are " + Arrays.toString(
              Action.values()));
    }
    if (services == null || services.isEmpty()) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
          "service not found");
    }
    String result = "";
    boolean success = false;
    int exception = Response.Status.BAD_REQUEST.getStatusCode();
    for (HostServices service : services) {
      Hosts h = service.getHost();
      if (h != null) {
        String ip = h.getPublicOrPrivateIp();
        String agentPassword = h.getAgentPassword();
        try {
          result += service.toString() + " " + web.serviceOp(operation.value(), ip, agentPassword,
              service.getCluster(), service.getGroup(), service.getService());
          success = true;
        } catch (AppException ex) {
          if (services.size() == 1) {
            throw ex;
          } else {
            exception = ex.getStatus();
            result += service.toString() + " " + ex.getStatus() + " " + ex.getMessage();
          }
        }
      } else {
        result += service.toString() + " " + "host not found: " + service.getHost();
      }
      result += "\n";
    }
    if (!success) {
      throw new AppException(exception, result);
    }
    return result;
  }

  private Hosts findHostById(String hostname) {
    Hosts host = hostEJB.findByHostname(hostname);
    return host;
  }

}
