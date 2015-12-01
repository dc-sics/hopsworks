/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.authz;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.ejb.EJB;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.YubikeyActivator;
import se.kth.hopsworks.user.model.Users;

public class PolicyAdministrationPoint implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(YubikeyActivator.class.
          getName());

  @EJB
  private UserManager userPolicMgr;

  public boolean isInAdminRole(Users user) {
    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInDataProviderRole(Users user) {
    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInAuditorRole(Users user) {
    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInResearcherRole(Users user) {
    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_RESEARCHER.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInGuestRole(Users user) {
    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_GUEST.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInAdminRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    if (userPolicMgr.findGroups(user.getUid()) == null) {
      return false;
    }

    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInResearcherRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    if (userPolicMgr.findGroups(user.getUid()) == null) {
      return false;
    }

    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_RESEARCHER.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInDataProviderRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    if (userPolicMgr.findGroups(user.getUid()) == null) {
      return false;
    }

    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInAuditorRole(String username) {

    Users user = userPolicMgr.getUserByUsername(username);
    if (userPolicMgr.findGroups(user.getUid()) == null) {
      return false;
    }

    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.
            name())) {
      return true;
    }
    return false;
  }

  public boolean isInGuestRole(String username) {
    Users user = userPolicMgr.getUserByUsername(username);
    if (userPolicMgr.findGroups(user.getUid()) == null) {
      return false;
    }

    if (userPolicMgr.findGroups(user.getUid()).contains(BBCGroup.BBC_GUEST.
            name())) {
      return true;
    }
    return false;
  }

  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "auditIndex";
    } else if (isInDataProviderRole(user) || isInResearcherRole(user)) {
      return "home";
    }

    return "home";
  }
}
