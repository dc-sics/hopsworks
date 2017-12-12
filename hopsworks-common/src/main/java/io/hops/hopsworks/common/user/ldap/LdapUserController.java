package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserFacade;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class LdapUserController {

  private final static Logger LOGGER = Logger.getLogger(LdapUserController.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private LdapUserFacade ldapUserFacade;

  public Users getUser(Integer uidNumber) {
    LdapUser ldapUser = ldapUserFacade.findByLdapUid(uidNumber);
    return ldapUser != null ? ldapUser.getUid() : null;
  }

  public void createLdapUser() {

  }
}
