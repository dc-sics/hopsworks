package io.hops.hopsworks.cluster.controller;

import io.hops.hopsworks.cluster.ClusterDTO;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.util.EmailBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterController {

  private final static Logger LOGGER = Logger.getLogger(ClusterController.class.getName());
  private final static String DATE_FORMAT = "yyMMddHHmmssZ";//010704120856-0700
  @EJB
  private UserFacade userBean;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private AuditManager am;

  public void register(ClusterDTO cluster, HttpServletRequest req) {
    isValidNewCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent != null) {
      throw new IllegalArgumentException("Cluster alrady registerd.");
    }
    String validationKey = getNewKey();
    Users agentUser = new Users();
    agentUser.setUsername("agent");
    agentUser.setEmail(cluster.getEmail());
    agentUser.setStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue());
    agentUser.setMode(PeopleAccountStatus.M_ACCOUNT_TYPE.getValue());
    agentUser.setPassword(DigestUtils.sha256Hex(cluster.getChosenPassword()));
    agentUser.setValidationKey(validationKey);
    agentUser.setMaxNumProjects(0);

    BbcGroup group = groupFacade.findByGroupName("CLUSTER_AGENT");
    if (group == null) {
      group = new BbcGroup(1008, "CLUSTER_AGENT");//get id from table
      groupFacade.save(group);
    }

    List<BbcGroup> groups = new ArrayList<>();
    groups.add(group);
    agentUser.setBbcGroupCollection(groups);

    boolean emailSent = sendEmail(cluster, req, validationKey);
    if (emailSent) {
      userBean.persist(agentUser);
      am.registerAccountChange(agentUser, AccountsAuditActions.REGISTRATION.name(), AccountsAuditActions.SUCCESS.name(),
              "", agentUser, req);
    } else {
      am.registerAccountChange(agentUser, AccountsAuditActions.REGISTRATION.name(), AccountsAuditActions.FAILED.name(),
              "", agentUser, req);
    }
  }

  public void unregister(ClusterDTO cluster, HttpServletRequest req) {
    isValidCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    String password = DigestUtils.sha256Hex(cluster.getChosenPassword());
    if (!password.equals(clusterAgent.getPassword())) {
      throw new SecurityException("Password not correct.");
    }
    String validationKey = SecurityUtils.getRandomPassword(64);
    boolean emailSent = sendEmail(cluster, req, validationKey);
    if (emailSent) {
      clusterAgent.setValidationKey(validationKey);
      userBean.update(clusterAgent);
    }

  }

  private void isValidNewCluster(ClusterDTO cluster) {
    isValidCluster(cluster);
    if (!cluster.getChosenPassword().equals(cluster.getRepeatedPassword())) {
      throw new IllegalArgumentException("Cluster password does not match.");
    }
    if (!cluster.isToS()) {
      throw new IllegalStateException("Cluster ToS not signed");
    }
  }

  private void isValidCluster(ClusterDTO cluster) {
    if (cluster == null) {
      throw new NullPointerException("Cluster not assigned.");
    }
    if (cluster.getEmail() == null || cluster.getEmail().isEmpty()) {
      throw new IllegalArgumentException("Cluster email not set.");
    }
    if (cluster.getChosenPassword() == null || cluster.getChosenPassword().isEmpty()) {
      throw new IllegalArgumentException("Cluster password not set.");
    }
  }

  private boolean sendEmail(ClusterDTO cluster, HttpServletRequest req, String validationKey) {
    try {
      emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT, UserAccountsEmailMessages.buildMobileRequestMessage(
                      AuditUtil.getUserURL(req), cluster.getEmail() + validationKey));
      return true;
    } catch (MessagingException ex) {
      return false;
    }
  }

  private String getNewKey() {
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date();
    return SecurityUtils.getRandomPassword(64) + dateFormat.format(date);
  }

}
