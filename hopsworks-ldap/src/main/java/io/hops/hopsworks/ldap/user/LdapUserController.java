package io.hops.hopsworks.ldap.user;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class LdapUserController {

  @EJB
  private LdapRealm ldapRealm;
  
  public void login () {
    
  }

}
