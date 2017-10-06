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
import io.hops.hopsworks.common.util.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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
  private final static String CERT_INDEX_FILE = "index.txt";
  private final static String DATE_FORMAT = "yyMMddHHmmssZ";//010704120856-0700
  private final static String CLUSTER_NAME_PREFIX = "Agent";
  private final static long VALIDATION_KEY_EXPIRY_DATE = 24l;
  private final static int REG_RANDOM_KEY_LEN = 32;
  private final static int UNREG_RANDOM_KEY_LEN = 64;

  public static enum OP_TYPE {
    REGISTER,
    UNREGISTER
  }
  @EJB
  private UserFacade userBean;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private AuditManager am;
  @EJB
  private Settings settings;

  public void register(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidNewCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent != null) {
      throw new IllegalArgumentException("Cluster alrady registerd.");
    }
    String agentName = getAgentName();
    String validationKey = getNewKey(REG_RANDOM_KEY_LEN);
    Users agentUser = new Users();
    agentUser.setUsername(agentName);
    agentUser.setEmail(cluster.getEmail());
    agentUser.setFname(CLUSTER_NAME_PREFIX);
    agentUser.setLname("008");
    agentUser.setStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue());
    agentUser.setMode(PeopleAccountStatus.M_ACCOUNT_TYPE.getValue());
    agentUser.setPassword(DigestUtils.sha256Hex(cluster.getChosenPassword()));
    agentUser.setValidationKey(validationKey);
    agentUser.setMaxNumProjects(0);

    BbcGroup group = groupFacade.findByGroupName("CLUSTER_AGENT");
    if (group == null) {
      group = new BbcGroup(1008, "CLUSTER_AGENT");//get id from table or do this in chef
      groupFacade.save(group);
    }

    List<BbcGroup> groups = new ArrayList<>();
    groups.add(group);
    agentUser.setBbcGroupCollection(groups);

    sendEmail(cluster, req, agentName + agentUser.getValidationKey(), agentUser, AccountsAuditActions.REGISTRATION.
        name());
    userBean.persist(agentUser);
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", new Object[]{agentUser.getEmail(),
      agentUser.getUsername()});

  }

  public void unregister(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    String password = DigestUtils.sha256Hex(cluster.getChosenPassword());
    if (!password.equals(clusterAgent.getPassword())) {
      throw new SecurityException("Incorrect password.");
    }
    String validationKey = getNewKey(UNREG_RANDOM_KEY_LEN);
    sendEmail(cluster, req, clusterAgent.getUsername() + validationKey, clusterAgent,
        AccountsAuditActions.UNREGISTRATION.name());
    clusterAgent.setValidationKey(validationKey);
    userBean.update(clusterAgent);
  }

  public void validateRequest(String key, HttpServletRequest req, OP_TYPE type) throws ParseException {
    String agentName = extractUsername(key);
    int keyLen = (type.equals(OP_TYPE.REGISTER) ? REG_RANDOM_KEY_LEN : UNREG_RANDOM_KEY_LEN);
    Date date = extractDate(key, keyLen);
    long diff = getDateDiffHours(date);
    String validationKey = extractValidationKey(key);
    Users agent = userBean.findByUsername(agentName);
    if (agent == null) {
      throw new IllegalStateException("Agent not found.");
    }
    if (!agent.getValidationKey().equals(validationKey)) {
      throw new IllegalStateException("Validation key not found.");
    }
    if (diff > VALIDATION_KEY_EXPIRY_DATE) {
      throw new IllegalStateException("Expired valdation key.");
    }
    if (type.equals(OP_TYPE.REGISTER)) {
      agent.setValidationKey(null);
      agent.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT.getValue());
      userBean.update(agent);
    } else {
      removeCluster(agent);
      userBean.remove(agent);
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

  private void sendEmail(ClusterDTO cluster, HttpServletRequest req, String validationKey, Users u, String type) throws
      MessagingException {
    if (type == null || type.isEmpty()) {
      throw new IllegalArgumentException("No type set.");
    }
    try {
      if (type.equals(AccountsAuditActions.REGISTRATION.name())) {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
                buildClusterRegisterRequestMessage(AuditUtil.getUserURL(req), validationKey));
      } else {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
                buildClusterUnregisterRequestMessage(AuditUtil.getUserURL(req), validationKey));
      }

      am.registerAccountChange(u, type, AccountsAuditActions.SUCCESS.name(), "", u, req);
    } catch (MessagingException ex) {
      LOGGER.log(Level.SEVERE, "Could not send email to ", u.getEmail());
      am.registerAccountChange(u, type, AccountsAuditActions.FAILED.name(), "", u, req);
      throw new MessagingException(ex.getMessage());
    }
  }

  private String getNewKey(int len) {
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date();
    return SecurityUtils.getRandomPassword(len) + dateFormat.format(date);
  }

  private String getAgentName() {
    String sufix = "" + (userBean.lastUserID() + 1);
    int end = Settings.USERNAME_LEN - sufix.length();
    String name = CLUSTER_NAME_PREFIX.toLowerCase().substring(0, end) + (userBean.lastUserID() + 1);
    return name;
  }

  private String extractUsername(String key) {
    return key.substring(0, Settings.USERNAME_LEN);
  }

  private String extractValidationKey(String key) {
    return key.substring(Settings.USERNAME_LEN);
  }

  private Date extractDate(String key, int keyLen) throws ParseException {
    int start = Settings.USERNAME_LEN + keyLen;
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
    String date = key.substring(start);
    return format.parse(date);
  }

  private long getDateDiffHours(Date start) {
    Date now = new Date();
    long diff = now.getTime() - start.getTime();
    return TimeUnit.MILLISECONDS.toHours(diff);
  }

  private void removeCluster(Users agent) {

  }

  private void revokeCert(Users agent) throws FileNotFoundException, IOException, InterruptedException,
      CertificateException {
    File newcertsDir = new File("certs-dir/newcerts/");
    File[] listOfFiles = newcertsDir.listFiles();
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
       
      } 
    }
  }

  /**
   * Retrieve email from line:
   *
   * @param line
   * @return
   */
  private String getEmailFromLine(String line) {
    String email = "emailAddress=";
    int start = line.indexOf(email);
    String tmpName, name = "";
    if (start > -1) {
      tmpName = line.substring(start + email.length());
      int end = tmpName.indexOf("/");
      if (end > 0) {
        name = tmpName.substring(0, end);
      } else {
        name = tmpName;
      }
    }
    return name;
  }

}
