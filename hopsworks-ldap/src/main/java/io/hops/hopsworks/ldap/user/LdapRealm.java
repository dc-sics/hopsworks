package io.hops.hopsworks.ldap.user;

import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.login.LoginException;

@Singleton
public class LdapRealm {

  private final static Logger LOGGER = Logger.getLogger(LdapRealm.class.getName());
  
  private static final String PARAM_DIRURL = "ldap-directory";
  private static final String PARAM_USERDN = "ldap-base-dn";
  private static final String PARAM_SEARCH_USERDN = "ldap-search-bind-dn";
  private static final String PARAM_SEARCH_USERDN_PWD = "ldap-search-bind-password";
  private static final String PARAM_UNIQUE_FIELD = "ldap-user-uniqueField";
  private static final String PARAM_USERNAME_FIELD = "ldap-user-usernameField";
  private static final String PARAM_GIVEN_NAME_FIELD = "ldap-user-givenNameField";
  private static final String PARAM_SN_FIELD = "ldap-user-surNameField";
  private static final String PARAM_EMAIL_FIELD = "ldap-user-emailField";
  private static final String PARAM_POOLSIZE = "pool-size";
 
  private static final String JNDICF_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";
  private static final String LDAP_SOCKET_FACTORY = "java.naming.ldap.factory.socket";
  private static final String SSL_LDAP_SOCKET_FACTORY
      = "com.sun.enterprise.security.auth.realm.ldap.CustomSocketFactory";
  private static final String LDAPS_URL = "ldaps://";
  private static final String DEFAULT_POOL_PROTOCOL = "plain ssl";
  private static final String[] DN_ONLY = {"dn"};
  private static final String SEARCH_FILTER_DEFAULT = "uid=%s";
  private static final String JNDI_POOL = "com.sun.jndi.ldap.connect.pool";
  private static final String JNDI_POOL_PROTOCOL = "com.sun.jndi.ldap.connect.pool.protocol";
  private static final String JNDI_POOL_MAXSIZE = "com.sun.jndi.ldap.connect.pool.maxsize";
  private static final Integer POOLSIZE_DEFAULT = 5;

  private String url;
  private String baseDN;
  private String uniqueField;
  private String usernameField;
  private String givenNameField;
  private String surNameField;
  private String emailField;
  private String searchBindDn;
  private String searchBindPassword;

  private Properties ldapBindProps;

  @PostConstruct
  public void init() {
    Properties props = System.getProperties();
    url = "ldap://193.10.66.104:1389";
    baseDN = "dc=example,dc=com";
    ldapBindProps = new Properties();
    ldapBindProps.put(Context.INITIAL_CONTEXT_FACTORY, JNDICF_DEFAULT);
    ldapBindProps.put(Context.PROVIDER_URL, url);
    searchBindDn = "";
    searchBindPassword = "";
    if (searchBindDn != null && !searchBindDn.isEmpty()) {
      ldapBindProps.setProperty(Context.SECURITY_PRINCIPAL, searchBindDn);
    }
    if (searchBindPassword != null && !searchBindPassword.isEmpty()) {
      ldapBindProps.setProperty(Context.SECURITY_CREDENTIALS, searchBindPassword);
    }
    String usePool = "true";
    ldapBindProps.setProperty(JNDI_POOL, usePool);
    if ("true".equals(usePool)) {
      String poolSize = Integer.getInteger(PARAM_POOLSIZE, POOLSIZE_DEFAULT).toString();
      String sysPoolSize = props.getProperty(JNDI_POOL_MAXSIZE, poolSize);
      try {
        sysPoolSize = Integer.valueOf(sysPoolSize).toString();
      } catch (Exception ex) {
        sysPoolSize = poolSize;
      }
      if (System.getProperty(JNDI_POOL_MAXSIZE) == null) {
        System.setProperty(JNDI_POOL_MAXSIZE, sysPoolSize);
      }
    }
    if (url != null && url.startsWith(LDAPS_URL)) {
      ldapBindProps.put(LDAP_SOCKET_FACTORY, SSL_LDAP_SOCKET_FACTORY);
      if ("true".equals(usePool) && System.getProperty(JNDI_POOL_PROTOCOL) == null) {
        System.setProperty(JNDI_POOL_PROTOCOL, DEFAULT_POOL_PROTOCOL);
      }
    }
    if (!pingLDAP()) {
      throw new IllegalStateException("Could not reach ldap server at: " + url);
    }

    uniqueField = "uidNumber";
    usernameField = "uid";
    givenNameField = "givenName";
    surNameField = "sn";
    emailField = "email";
  }

  public boolean pingLDAP() {
    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(ldapBindProps);
      LOGGER.log(Level.INFO, "Ldap ok: {0}", url);
      return true;
    } catch (AuthenticationNotSupportedException anse) {
      LOGGER.log(Level.INFO, "Ldap ok: {0}", url);
      return true;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldap not ok: {0} {1} {2}", new Object[]{url, e.getClass().getName(), e.getMessage()});
      LOGGER.log(Level.WARNING, "Error binding to directory as: {0}", baseDN);
      LOGGER.log(Level.WARNING, "Exception from JNDI: {0}", e.toString());
      return false;
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception ex) {
        }
      }
    }
  }

  public LdapUserDTO findAndBind(String username, String password) throws LoginException {
    LdapUserDTO ldapUserDTO = null;
    String userid = String.format(SEARCH_FILTER_DEFAULT, username);

    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(getLdapBindProps());
      String userDN = userSearch(ctx, baseDN, userid);
      if (userDN == null) {
        throw new LoginException("Ldap realm user not found. " + userDN);
      }
      boolean bindSuccessful = bindAsUser(userDN, password);
      if (bindSuccessful == false) {
        throw new LoginException("Ldap realm bind failed. " + userDN);
      }
      ldapUserDTO = createLdapUser(ctx, baseDN, userid);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Ldap realm exception: {0}", e.getMessage());
      throw new LoginException(e.toString());
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception e) {
        }
      }
    }

    return ldapUserDTO;
  }

  private String userSearch(DirContext ctx, String baseDN, String filter) {
    String foundDN = null;
    NamingEnumeration answer = null;

    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(DN_ONLY);
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setCountLimit(1);

    try {
      answer = ctx.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        StringBuilder sb = new StringBuilder();
        CompositeName compDN = new CompositeName(res.getName());
        String ldapDN = compDN.get(0);
        sb.append(ldapDN);
        if (res.isRelative()) {
          sb.append(",");
          sb.append(baseDN);
        }
        foundDN = sb.toString();
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", e.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {

        }
      }
    }
    return foundDN;
  }

  private LdapUserDTO createLdapUser(DirContext ctx, String baseDN, String filter) {
    NamingEnumeration answer = null;
    String uidNumber;
    String uid;
    String givenName;
    String sn;
    List<String> email = new ArrayList<>();
    LdapUserDTO ldapUserDTO = null;
    SearchControls ctls = new SearchControls();
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setCountLimit(1);
    try {
      answer = ctx.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        if (answer.hasMore()) {
          Attributes attr = res.getAttributes();
          NamingEnumeration a;
          uidNumber = (String) attr.remove(uniqueField).get();
          uid = (String) attr.remove(usernameField).get();
          givenName = (String) attr.remove(givenNameField).get();
          sn = (String) attr.remove(surNameField).get();
          a = attr.remove(emailField).getAll();
          while (a.hasMore()) {
            email.add((String) a.next());
          }
          ldapUserDTO = new LdapUserDTO(uidNumber, uid, givenName, sn, email);
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", e.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {
        }
      }
    }
    return ldapUserDTO;
  }

  private boolean bindAsUser(String bindDN, String password) {
    boolean bindSuccessful = false;
    Properties p = getLdapBindProps();
    p.put(Context.SECURITY_PRINCIPAL, bindDN);
    p.put(Context.SECURITY_CREDENTIALS, password);

    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(p);
      bindSuccessful = true;
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error binding to directory as: {0}", bindDN);
      LOGGER.log(Level.INFO, "Exception from JNDI: {0}", e.toString());
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception e) {
        }
      }
    }
    return bindSuccessful;
  }

  private Properties getLdapBindProps() {
    return (Properties) ldapBindProps.clone();
  }
}
