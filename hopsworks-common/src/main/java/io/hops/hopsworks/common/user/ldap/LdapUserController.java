package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserFacade;
import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.user.UsersController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.security.auth.login.LoginException;

@Stateless
public class LdapUserController {

  private final static Logger LOGGER = Logger.getLogger(LdapUserController.class.getName());
  @EJB
  private LdapRealm ldapRealm;
  @EJB
  private LdapUserFacade ldapUserFacade;
  @EJB
  private UsersController userController;

  public LdapUserState login(String username, String password, boolean consent, String chosenEmail) throws
      LoginException {
    LdapUserDTO userDTO = ldapRealm.findAndBind(username, password);// login user
    if (userDTO == null) {
      LOGGER.log(Level.WARNING, "User not found, or wrong LDAP configuration.");
      throw new LoginException("User not found.");
    }
    LdapUser ladpUser = ldapUserFacade.findByLdapUid(userDTO.getEntryUUID());
    LdapUserState ldapUserState;
    if (ladpUser == null) {
      ladpUser = createNewLdapUser(userDTO, chosenEmail); //ask the user if it is ok to save this info about them.
      ldapUserState = new LdapUserState(false, ladpUser, userDTO);
      if (consent) {
        persistLdapUser(ladpUser);
        ldapUserState.setSaved(true);
      }
      return ldapUserState;
    }
    ldapUserState = new LdapUserState(true, ladpUser, userDTO);
    if (ldapUserUpdated(userDTO, ladpUser.getUid())) {
      ladpUser = updateLdapUser(userDTO, ladpUser);//do we need to ask again?
      ldapUserState.setLdapUser(ladpUser);
      return ldapUserState;
    }
    return ldapUserState;
  }

  private LdapUser createNewLdapUser(LdapUserDTO userDTO, String chosenEmail) throws LoginException {
    LOGGER.log(Level.INFO, "Creating new ldap user.");
    if (userDTO.getEmail().size() != 1 && (chosenEmail == null || chosenEmail.isEmpty())) {
      LOGGER.log(Level.WARNING, "Could not register user. Email not chosen.");
      throw new LoginException("Could not register user. Email not chosen.");
    }
    String email = userDTO.getEmail().size() == 1 ? userDTO.getEmail().get(0) : chosenEmail;
    String authKey = SecurityUtils.getRandomPassword(16);
    Users user = userController.createNewLdapUser(email, userDTO.getGivenName(), userDTO.getSn(), authKey);
    return new LdapUser(userDTO.getEntryUUID(), user, authKey);
  }

  private boolean ldapUserUpdated(LdapUserDTO user, Users uid) {
    return !uid.getFname().equals(user.getGivenName()) || !uid.getLname().equals(user.getSn());
  }

  private LdapUser updateLdapUser(LdapUserDTO user, LdapUser ldapUser) {
    if (!ldapUser.getUid().getFname().equals(user.getGivenName())) {
      ldapUser.getUid().setFname(user.getGivenName());
    }
    if (!ldapUser.getUid().getLname().equals(user.getSn())) {
      ldapUser.getUid().setLname(user.getSn());
    }
    return ldapUserFacade.update(ldapUser);
  }

  private void persistLdapUser(LdapUser ladpUser) {
    ldapUserFacade.save(ladpUser);
  }

}
