package io.hops.kmon.command;

import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class CommandController {

    @EJB
    private CommandEJB commandEJB;
    @ManagedProperty("#{param.hostid}")
    private String hostId;
    @ManagedProperty("#{param.role}")
    private String role;
    @ManagedProperty("#{param.service}")
    private String service;
    @ManagedProperty("#{param.cluster}")
    private String cluster;
    private static final Logger logger = Logger.getLogger(CommandController.class.getName());

    public CommandController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init CommandController");
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCluster() {
        return cluster;
    }

    public List<Command> getRecentCommandsByCluster() {
        List<Command> commands = commandEJB.findRecentByCluster(cluster);
        return commands;
    }

    public List<Command> getRunningCommandsByCluster() {
        List<Command> commands = commandEJB.findRunningByCluster(cluster);
        return commands;
    }

    public List<Command> getRecentCommandsByClusterService() {
        List<Command> commands = commandEJB.findRecentByClusterService(cluster, service);
        return commands;
    }

    public List<Command> getRunningCommandsByClusterService() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        List<Command> commands = commandEJB.findRunningByClusterService(cluster, service);
        return commands;
    }

    public List<Command> getRecentCommandsByInstance() {
        List<Command> commands = commandEJB.findRecentByClusterService(cluster, service);
        return commands;
    }
}
