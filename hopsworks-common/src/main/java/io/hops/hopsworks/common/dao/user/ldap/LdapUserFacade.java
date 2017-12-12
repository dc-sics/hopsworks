package io.hops.hopsworks.common.dao.user.ldap;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Stateless
public class LdapUserFacade extends AbstractFacade<LdapUser> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public LdapUserFacade() {
    super(LdapUser.class);
  }
  
  public LdapUser findByLdapUid(Integer uidNumber) {
    return em.find(LdapUser.class, uidNumber);
  }

  public LdapUser findByUsers(Users user) {
    try {
      return em.createNamedQuery("Users.findByUid", LdapUser.class).setParameter("uid", user).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
