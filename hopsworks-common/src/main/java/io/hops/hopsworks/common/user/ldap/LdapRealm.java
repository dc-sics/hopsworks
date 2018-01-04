package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.login.LoginException;

@Singleton
public class LdapRealm {

  private final static Logger LOGGER = Logger.getLogger(LdapRealm.class.getName());
  private static final String[] DN_ONLY = {"dn"};
  private static final String LOGIN_NAME_FIELD_DEFAULT = "uid";
  private static final String GIVEN_NAME_FIELD_DEFAULT = "givenName";
  private static final String SURNAME_FIELD_DEFAULT = "sn";
  private static final String EMAIL_FIELD_DEFAULT = "mail";
  private static final String SEARCH_FILTER_DEFAULT = LOGIN_NAME_FIELD_DEFAULT + "=%s";
  private static final String JNDICF_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";
  private static final String LDAP_ATTR_BINARY = "java.naming.ldap.attributes.binary";

  private String entryUUIDField;
  private String usernameField;
  private String givenNameField;
  private String surnameField;
  private String emailField;
  private String searchFilter;
  private String[] returningAttrs;
  private Hashtable ldapProperties;

  @Resource(name = "ldap/LdapResource")
  private DirContext dirContext;

  @PostConstruct
  public void init() {
    ldapProperties = getLdapBindProps();
    entryUUIDField = (String) ldapProperties.get(LDAP_ATTR_BINARY);
    if (entryUUIDField == null || entryUUIDField.isEmpty()) {
      throw new IllegalStateException("No UUID set for resource. Set java.naming.ldap.attributes.binary property.");
    }
    usernameField = LOGIN_NAME_FIELD_DEFAULT;
    givenNameField = GIVEN_NAME_FIELD_DEFAULT;
    surnameField = SURNAME_FIELD_DEFAULT;
    emailField = EMAIL_FIELD_DEFAULT;
    searchFilter = SEARCH_FILTER_DEFAULT;
    String[] attrs = {entryUUIDField, usernameField, givenNameField, surnameField, emailField};
    returningAttrs = attrs;
  }

  public void test() throws NamingException, LoginException {
    LdapUserDTO user = findAndBind("ermias", "ermiasldap");
    LOGGER.log(Level.INFO, "user: {0}", user);
  }

  public LdapUserDTO findAndBind(String username, String password) throws LoginException {
    String userid = String.format(searchFilter, username);
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, password); // try login
    LdapUserDTO user = createLdapUser(userid);
    return user;
  }

  public void authenticateLdapUser(String username, String password) throws LoginException {
    String userid = String.format(searchFilter, username);
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, password); // try login
  }

  private String userDNSearch(String filter) {
    String distinguishedName = null;
    NamingEnumeration answer = null;

    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(DN_ONLY);
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setCountLimit(1);

    try {
      answer = dirContext.search("", filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        CompositeName compDN = new CompositeName(res.getNameInNamespace());
        distinguishedName = compDN.get(0);
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
    return distinguishedName;
  }

  private LdapUserDTO createLdapUser(String filter) {
    NamingEnumeration answer = null;
    String uuid;
    String uid;
    String givenName;
    String sn;
    List<String> email;
    LdapUserDTO ldapUserDTO = null;
    SearchControls ctls = new SearchControls();
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setReturningAttributes(returningAttrs);
    ctls.setCountLimit(1);
    try {
      answer = dirContext.search("", filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        Attributes attrs = res.getAttributes();
        uuid = getUUIDAttribute(attrs, entryUUIDField);
        uid = getAttribute(attrs, usernameField);
        givenName = getAttribute(attrs, givenNameField);
        sn = getAttribute(attrs, surnameField);
        email = getAttrList(attrs, emailField);
        ldapUserDTO = new LdapUserDTO(uuid, uid, givenName, sn, email);
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

  private boolean bindAsUser(String bindDN, String password) throws LoginException {
    boolean bindSuccessful = false;
    Hashtable p = getLdapBindProps();
    p.put(Context.INITIAL_CONTEXT_FACTORY, JNDICF_DEFAULT);
    p.put(Context.SECURITY_PRINCIPAL, bindDN);
    p.put(Context.SECURITY_CREDENTIALS, password);
    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(p);
      bindSuccessful = true;
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error binding to directory as: {0}", bindDN);
      LOGGER.log(Level.INFO, "Exception from JNDI: {0}", e.toString());
      throw new LoginException(e.getMessage());
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

  private String getUUIDAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    byte[] guid = attr != null ? (byte[]) attr.get() : "".getBytes();
    return new String(guid);
  }

  private String getAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    return attr != null ? (String) attr.get() : "";
  }

  private List<String> getAttrList(Attributes attrs, String key) throws NamingException {
    List<String> vals = new ArrayList<>();
    Attribute attr = attrs.remove(key);
    if (attr == null) {
      return vals;
    }
    NamingEnumeration a = attr.getAll();
    while (a.hasMore()) {
      vals.add((String) a.next());
    }
    return vals;
  }

  private Hashtable getLdapBindProps() {
    Hashtable ldapProperties = new Hashtable();
    try {
      ldapProperties = (Hashtable) dirContext.getEnvironment().clone();
    } catch (NamingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    for (Object key : ldapProperties.keySet()) {
      LOGGER.log(Level.INFO, "{0}:{1}", new Object[]{key, ldapProperties.get(key)});
    }
    return ldapProperties;
  }
}
