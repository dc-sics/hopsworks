package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.util.AuditUtil;

@ManagedBean
@ViewScoped
public class PeopleAdministration implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @EJB
  private BbcGroupFacade bbcGroupFacade;

  @EJB
  private EmailBean emailBean;

  @ManagedProperty("#{clientSessionState}")
  private ClientSessionState sessionState;

  @Resource
  private UserTransaction userTransaction;

  private Users user;

  private String secAnswer;

  private List<Users> filteredUsers;
  private List<Users> selectedUsers;

  // All verified users
  private List<Users> allUsers;

  // Accounts waiting to be validated by the email owner
  private List<Users> spamUsers;

  // for modifying user roles and status
  private Users editingUser;

  // for mobile users activation
  private List<Users> requests;

  // for user activation
  private List<Users> yRequests;

  // to remove an existing group
  private String role;

  // to assign a new status
  private String selectedStatus;

  // to assign a new group
  private String nGroup;

  List<String> status;

  // all groups
  List<String> groups;

  // all existing groups belong tp
  List<String> cGroups;

  // all possible new groups user doesnt belong to
  List<String> nGroups;

  // current status of the editing user
  private String eStatus;

  // list of roles that can be activated for a user
  List<String> actGroups;

  public PeopleAdministration() {
    // Default no-arg constructor
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String geteStatus() {
    if (this.editingUser == null) {
      return "";
    }

    this.eStatus
            = PeopleAccountStatus.values()[this.editingUser.getStatus() - 1].
            name();
    return this.eStatus;
  }

  public void seteStatus(String eStatus) {
    this.eStatus = eStatus;
  }

  public String getnGroup() {
    return nGroup;
  }

  public void setnGroup(String nGroup) {
    this.nGroup = nGroup;
  }

  public Users getEditingUser() {
    return editingUser;
  }

  public void setEditingUser(Users editingUser) {
    this.editingUser = editingUser;
  }

  public List<Users> getyRequests() {
    return yRequests;
  }

  public void setyRequests(List<Users> yRequests) {
    this.yRequests = yRequests;
  }

  public List<String> getUserRole(Users p) {
    List<String> list = userManager.findGroups(p.getUid());
    return list;
  }

  public String getChanged_Status(Users p) {
    return PeopleAccountStatus.values()[userManager.findByEmail(p.getEmail()).
            getStatus() - 1].name();
  }

  public List<String> getActGroups() {
    return actGroups;
  }

  public void setActGroups(List<String> actGroups) {

    this.actGroups = actGroups;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  /**
   * Filter the current groups
   *
   * @return
   */
  public List<String> getcGroups() {
    List<String> list = userManager.findGroups(editingUser.getUid());
    return list;
  }

  public void setcGroups(List<String> cGroups) {
    this.cGroups = cGroups;
  }

  public List<String> getnGroups() {
    List<String> list = userManager.findGroups(editingUser.getUid());
    List<String> tmp = new ArrayList<>();

    for (BbcGroup b : bbcGroupFacade.findAll()) {
      if (!list.contains(b.getGroupName())) {
        tmp.add(b.getGroupName());
      }
    }
    return tmp;
  }

  public void setnGroups(List<String> nGroups) {
    this.nGroups = nGroups;
  }

  public String getSelectedStatus() {
    return selectedStatus;
  }

  public void setSelectedStatus(String selectedStatus) {
    this.selectedStatus = selectedStatus;
  }

  @PostConstruct
  public void initGroups() {
    groups = new ArrayList<>();
    status = getStatus();
    actGroups = new ArrayList<>();
    spamUsers = new ArrayList<>();
    for (BbcGroup b : bbcGroupFacade.findAll()) {
      groups.add(b.getGroupName());
      actGroups.add(b.getGroupName());
    }
  }

  public List<String> getStatus() {

    this.status = new ArrayList<>();

    for (PeopleAccountStatus p : PeopleAccountStatus.values()) {
      status.add(p.name());
    }

    return status;
  }

  public void setStatus(List<String> status) {
    this.status = status;
  }

  public void setFilteredUsers(List<Users> filteredUsers) {
    this.filteredUsers = filteredUsers;
  }

  public List<Users> getFilteredUsers() {
    return filteredUsers;
  }

  /*
   * Find all registered users
   */
  public List<Users> getAllUsers() {
    allUsers = userManager.findAllUsers();
    return allUsers;
  }

  public List<Users> getUsersNameList() {
    return userManager.findAllUsers();
  }

  public List<String> getGroups() {
    return groups;
  }

  public Users getSelectedUser() {
    return user;
  }

  public void setSelectedUser(Users user) {
    this.user = user;
  }

  /**
   * Reject users that are not validated.
   *
   * @param user1
   */
  public void rejectUser(Users user1) {

    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    if (user1 == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      userManager.changeAccountStatus(user1.getUid(), "",
              PeopleAccountStatus.SPAM_ACCOUNT.getValue());
      MessagesController.addInfoMessage(user1.getEmail() + " was rejected.");
      spamUsers.add(user1);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Rejection failed. " + ex.
              getMessage());
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not reject user.", ex);
    }
    try {
      // Send rejection email
      emailBean.sendEmail(user1.getEmail(), RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_REJECT,
              UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to "
              + user1.getEmail());
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not send email to {0}. {1}", new Object[]{user1.getEmail(),
                e});
    }
  }

  /**
   * Removes a user from the db
   * Only works for new not yet activated users
   *
   * @param user
   */
  public void deleteUser(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      userManager.deleteUserRequest(user);
      MessagesController.addInfoMessage(user.getEmail() + " was removed.");
      spamUsers.remove(user);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.
              getMessage());
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not remove user.", ex);
    }
  }

  /**
   * Remove a user from spam list and set the users status to new mobile user.
   *
   * @param user
   */
  public void removeFromSpam(Users user) {
    if (user == null) {
      MessagesController.addErrorMessage("Error", "No user found!");
      return;
    }
    try {
      userManager.changeAccountStatus(user.getUid(), "",
              PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue());
      MessagesController.addInfoMessage(user.getEmail()
              + " was removed from spam list.");
      spamUsers.remove(user);
    } catch (RuntimeException ex) {
      MessagesController.addSecurityErrorMessage("Remove failed. " + ex.
              getMessage());
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not remove user from spam list.", ex);
    }
  }

  public void confirmMessage(ActionEvent actionEvent) {

    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Deletion Successful!", null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public String getLoginName() throws IOException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    Principal principal = request.getUserPrincipal();

    try {
      Users p = userManager.findByEmail(principal.getName());
      if (p != null) {
        return p.getFname() + " " + p.getLname();
      } else {
        return principal.getName();
      }
    } catch (Exception ex) {
      ExternalContext extContext = FacesContext.getCurrentInstance().
              getExternalContext();
      extContext.redirect(extContext.getRequestContextPath());
      return null;
    }
  }

  /**
   * Get all open user requests (mobile or simple accounts).
   *
   * @return
   */
  public List<Users> getAllRequests() {
    if (requests == null) {
      requests = userManager.findMobileRequests();
    }
    return requests;
  }

  /**
   * Get all Yubikey requests
   *
   * @return
   */
  public List<Users> getAllYubikeyRequests() {
    if (yRequests == null) {
      yRequests = userManager.findYubikeyRequests();
    }
    return yRequests;
  }

  public List<Users> getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(List<Users> users) {
    this.selectedUsers = users;
  }

  public void activateUser(Users user1) {
    if (user1 == null) {
      MessagesController.addSecurityErrorMessage("User is null.");
      return;
    }
    if (this.role == null || this.role.isEmpty()) {
      this.role = "HOPS_USER";
    }

    try {

      BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(this.role);

      userTransaction.begin();

      if (bbcGroup != null) {
        userManager.registerGroup(user1, bbcGroup.getGid());
        auditManager.registerRoleChange(sessionState.getLoggedInUser(),
                RolesAuditActions.ADDROLE.name(),
                RolesAuditActions.SUCCESS.name(), bbcGroup.getGroupName(),
                user1);
      } else {
        auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                RolesAuditActions.FAILED.name(), "Role could not be granted.",
                user1);
        MessagesController.addSecurityErrorMessage("Role could not be granted.");
        return;
      }

      try {
        userManager.updateStatus(user1, PeopleAccountStatus.ACTIVATED_ACCOUNT.
                getValue());
        auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                UserAuditActions.SUCCESS.name(), "", user1);
      } catch (ApplicationException | IllegalArgumentException ex) {
        auditManager.registerAccountChange(sessionState.getLoggedInUser(),
                PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
                RolesAuditActions.FAILED.name(), "User could not be activated.",
                user1);
        MessagesController.addSecurityErrorMessage(
                "Account activation problem not be granted.");
      }

      userTransaction.commit();

    } catch (NotSupportedException | SystemException |
            RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException e) {
      MessagesController.addSecurityErrorMessage("Could not activate user. "
              + e.getMessage());
      auditManager.registerAccountChange(sessionState.getLoggedInUser(),
              PeopleAccountStatus.ACTIVATED_ACCOUNT.name(),
              UserAuditActions.FAILED.name(), "", user1);
      return;
    }

    try {
      //send confirmation email
      emailBean.sendEmail(user1.getEmail(), RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
              UserAccountsEmailMessages.
              accountActivatedMessage(user1.getEmail()));
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Could not send email to "
              + user1.getEmail() + ". " + e.getMessage());
      Logger.getLogger(PeopleAdministration.class.getName()).log(Level.SEVERE,
              "Could not send email to {0}. {1}", new Object[]{user1.getEmail(),
                e});
    }

    requests.remove(user1);
  }

  public boolean notVerified(Users user) {
    if (user == null || user.getBbcGroupCollection() == null || user.
            getBbcGroupCollection().isEmpty() == false) {
      return false;
    }
    if (user.getStatus() == PeopleAccountStatus.VERIFIED_ACCOUNT.getValue()) {
      return false;
    }
    return true;
  }

  public void resendAccountVerificationEmail(Users user) throws
          MessagingException {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.
            getExternalContext().getRequest();

    String activationKey = SecurityUtils.getRandomPassword(64);
    emailBean.sendEmail(user.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessage(
                    AuditUtil.getUserURL(request), user.getUsername()
                    + activationKey));
    user.setValidationKey(activationKey);
    userManager.updatePeople(user);

  }

  public String modifyUser(Users user1) {
    // Get the latest status
    Users newStatus = userManager.getUserByEmail(user1.getEmail());
    FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser", newStatus);

    Userlogins login = auditManager.getLastUserLogin(user1.getUid());
    FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("editinguser_logins", login);

    MessagesController.addInfoMessage("User successfully modified for " + user1.getEmail());

    return "admin_profile";
  }

  public List<Users> getSpamUsers() {
    return spamUsers = userManager.findSPAMAccounts();
  }

  public void setSpamUsers(List<Users> spamUsers) {
    this.spamUsers = spamUsers;
  }

  public String getSecAnswer() {
    return secAnswer;
  }

  public void setSecAnswer(String secAnswer) {
    this.secAnswer = secAnswer;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public String activateYubikeyUser(Users u) {
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(
            "yUser", u);
    return "activate_yubikey";
  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

}
