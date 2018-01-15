package io.hops.hopsworks.kmon.communication;

import io.hops.hopsworks.common.util.WebCommunication;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.util.NodesTableItem;
import io.hops.hopsworks.kmon.service.ServiceInstancesController;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class CommunicationController {

  @EJB
  private HostsFacade hostEJB;
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private WebCommunication web;
  
  @ManagedProperty(value="#{serviceInstancesController}")
  private ServiceInstancesController serviceInstancesController;
  
  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;

  private List<InstanceInfo> instances;
  
  private static final Logger logger = Logger.getLogger(
          CommunicationController.class.getName());

  public CommunicationController() {
    logger.info("CommunicationController");
  }

  @PostConstruct
  public void init() {
  }

  public String getService() {
    return service;
  }

  public void setService(String role) {
    this.service = role;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public void setServiceInstancesController(ServiceInstancesController serviceInstancesController) {
    this.serviceInstancesController = serviceInstancesController;
  }

  private Hosts findHostById(String hostId) throws Exception {
    try {
      Hosts host = hostEJB.findByHostname(hostId);
      return host;
    } catch (Exception ex) {
      throw new RuntimeException("HostId " + hostId + " not found.");
    }
  }

  private Hosts findHostByService(String cluster, String group, String service)
          throws Exception {
    String id = hostServicesFacade.findRoles(cluster, group, service).get(0).getHost().getHostname();
    return findHostById(id);
  }


  public String mySqlClusterConfig() throws Exception {
    // Finds hostId of mgmserver
    // Role=mgmserver , Service=MySQLCluster, Cluster=cluster
    String mgmserverRole = "ndb_mgmd";
    Hosts h = findHostByService(cluster, group, mgmserverRole);
    String ip = h.getPublicOrPrivateIp();
    String agentPassword = h.getAgentPassword();
    return web.getConfig(ip, agentPassword, cluster, group, mgmserverRole);
  }

  public String getServiceLog(int lines) {
    try {
      Hosts h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.getServiceLog(ip, agentPassword, cluster, group, service, lines);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  private void uiMsg(String res) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage msg = null;
    if (res.contains("Error")) {
      msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, res,
              "There was a problem when executing the operation.");
    } else {
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, res,
              "Successfully executed the operation.");
    }
    context.addMessage(null, msg);
  }

  public void serviceStart() {
    uiMsg(serviceOperation("startRole"));

  }

  public void serviceStartAll() {
    uiMsg(serviceOperationAll("startRole"));
  }
  
  public void serviceRestart() {
    uiMsg(serviceOperation("restartRole"));
  }

  public void serviceRestartAll() {
    uiMsg(serviceOperationAll("restartRole"));
  }
  
  public void serviceStop() {
    uiMsg(serviceOperation("stopRole"));
  }

  public void serviceStopAll() {
    logger.log(Level.SEVERE, "serviceStopAll 1");
    uiMsg(serviceOperationAll("stopRole"));
  }
  
  private String serviceOperationAll(String operation) {
    instances = serviceInstancesController.getInstances();
    List<Future<String>> results = new ArrayList<>();
    String result = "";
    for (InstanceInfo instance : instances) {
      if (instance.getRole().equals(service)) {
        try {
          Hosts h = findHostById(instance.getHost());
          String ip = h.getPublicOrPrivateIp();
          String agentPassword = h.getAgentPassword();
          results.add(web.asyncServiceOp(operation, ip, agentPassword, cluster, group, service));
        } catch (Exception ex) {
          result = result + ex.getMessage() + "\n";
        }
      }
    }
    for(Future<String> r: results){
      try {
        result = result + r.get() + "\n";
      } catch (Exception ex) {
        result = result + ex.getMessage() + "\n";
      }
    }
    return result;
  }
  
  private String serviceOperation(String operation) {
    try {
      Hosts h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.serviceOp(operation, ip, agentPassword, cluster, group, service);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  public String getAgentLog(int lines) {
    try {
      Hosts h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.getAgentLog(ip, agentPassword, lines);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  public List<NodesTableItem> getNdbinfoNodesTable() throws Exception {

    // Finds host of mysqld
    // Role=mysqld , Service=MySQLCluster, Cluster=cluster
    final String ROLE = "mysqld";
    List<NodesTableItem> results;
    try {
      String id = hostServicesFacade.findRoles(cluster, group, ROLE).get(0).getHost().getHostname();
      Hosts h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      results = web.getNdbinfoNodesTable(ip, agentPassword);
    } catch (Exception ex) {
      results = new ArrayList<>();
    }
    return results;
  }

}
