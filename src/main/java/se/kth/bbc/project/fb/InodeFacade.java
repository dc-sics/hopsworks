package se.kth.bbc.project.fb;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.hopsworks.util.Settings;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public InodeFacade() {
    super(Inode.class);
  }
  
  /**
   * Find all the Inodes that have <i>parent</i> as parent.
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> findByParent(Inode parent) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParentId",
            Inode.class);
    query.setParameter("parentId", parent.getId());
    return query.getResultList();
  }

  /**
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p/>
   * @param parent
   * @return
   */
  public List<Inode> getChildren(Inode parent) {
    return findByParent(parent);
  }

  /**
   * Get a list of the names of the child files (so no directories) of the given
   * path.
   * <p/>
   * @param path
   * @return A list of filenames, empty if the given path does not have
   * children.
   */
  public List<String> getChildNames(String path) {
    Inode inode = getInodeAtPath(path);
    if (inode.isDir()) {
      List<Inode> inodekids = getChildren(inode);
      ArrayList<String> retList = new ArrayList<>(inodekids.size());
      for (Inode i : inodekids) {
        if (!i.isDir()) {
          retList.add(i.getInodePK().getName());
        }
      }
      return retList;
    } else {
      return Collections.EMPTY_LIST;
    }
  }
  /**
   * Find the parent of the given Inode. If the Inode has no parent, null is
   * returned.
   * <p/>
   * @param i
   * @return The parent, or null if no parent.
   */
  public Inode findParent(Inode i) {
    int id = i.getInodePK().getParentId();
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * find inode by id
   * <p/>
   * @param id
   * @return
   */
  public Inode findById(Integer id) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   *
   * @param path
   * @return null if no such Inode found
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private Inode getInode(String path) {
    // Get the path components
    String[] p;
    if (path.charAt(0) == '/') {
      p = path.substring(1).split("/");
    } else {
      p = path.split("/");
    }

    if (p.length < 1) {
      return null;
    }

    //Get the right root node
    Inode curr = getRootNode(p[0]);
    if (curr == null) {
      return null;
    }
    //Move down the path
    for (int i = 1; i < p.length; i++) {
      Inode next = findByParentAndName(curr, p[i]);
      if (next == null) {
        return null;
      } else {
        curr = next;
      }
    }
    return curr;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Inode getRootNode(String name) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findRootByName", Inode.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult(); //Sure to give a single result because all children of same parent "null" so name is unique
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Check whether the given path exists.
   * <p/>
   * @param path The path to search for.
   * @return True if the path exist (i.e. there is an Inode on this path), false
   * otherwise.
   */
  public boolean existsPath(String path) {
    return getInode(path) != null;
  }

  /**
   * Get the Inode at the specified path.
   * <p/>
   * @param path
   * @return Null if path does not exist.
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Inode getInodeAtPath(String path) {
    return getInode(path);
  }

  /**
   * Get the Inode representing the project root directory of the project with
   * given name.
   * <p/>
   * @param name
   * @return The sought for Inode, or null if this Inode does not exist.
   */
  public Inode getProjectRoot(String name) {
    return getInode("/" + Settings.DIR_ROOT + "/" + name);
  }

  /**
   * Find an Inode by its parent Inode and its name (i.e. its primary key).
   * <p/>
   * @param parent
   * @param name
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Inode findByParentAndName(Inode parent, String name) {

    TypedQuery<Inode> q = em.createNamedQuery("Inode.findByPrimaryKey",
            Inode.class);
    q.setParameter("inodePk", new InodePK(parent.getId(), name));
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the project base directory of which the given Inode is a descendant.
   * <p/>
   * @param i
   * @return The Inode representing the project root directory.
   * @throws IllegalStateException when the given Inode is not under a project
   * root directory.
   */
  public Inode getProjectRootForInode(Inode i) throws IllegalStateException {
    if (isProjectRoot(i)) {
      return i;
    } else {
      Inode parent = findParent(i);
      if (parent == null) {
        throw new IllegalStateException(
                "Transversing the path from folder did not encounter project root folder.");
      }
      return getProjectRootForInode(parent);
    }
  }

  /**
   * Find out if an Inode is a project root directory.
   * <p/>
   * @param i
   * @return
   */
  public boolean isProjectRoot(Inode i) {
    Inode parent = findParent(i);
    if (!parent.getInodePK().getName().equals(
            Settings.DIR_ROOT)) {
      return false;
    } else {
      //A node is the project root if its parent has the name $DIR_ROOT and its grandparent is the root node
      return parent.getInodePK().getParentId() == 1;
    }
  }

  /**
   * Get the name of the project of which this Inode is a descendant.
   * <p/>
   * @param i
   * @return
   * @throws IllegalStateException When the given Inode is not a descendant of
   * any project.
   */
  public String getProjectNameForInode(Inode i) throws IllegalStateException {
    Inode projectRoot = getProjectRootForInode(i);
    return projectRoot.getInodePK().getName();
  }

  /**
   * Get a list of NavigationPath objects representing the project-relative path
   * to the given Inode. The first element in the list is the project root
   * directory.
   * <p/>
   * @param i
   * @return
   */
  public List<NavigationPath> getConstituentsPath(Inode i) {
    if (isProjectRoot(i)) {
      List<NavigationPath> p = new ArrayList<>();
      p.add(new NavigationPath(i.getInodePK().getName(), i.getInodePK().
              getName() + "/"));
      return p;
    } else {
      List<NavigationPath> p = getConstituentsPath(findParent(i));
      NavigationPath a;
      if (i.isDir()) {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName() + "/");
      } else {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName());
      }
      p.add(a);
      return p;
    }
  }

  /**
   * Get the path to the given Inode.
   * <p/>
   * @param i
   * @return
   */
  public String getPath(Inode i) {
    List<String> pathComponents = new ArrayList<>();
    Inode parent = i;
    while (parent.getId() != 1) {
      pathComponents.add(parent.getInodePK().getName());
      parent = findParent(parent);
    }
    StringBuilder path = new StringBuilder();
    for (int j = pathComponents.size() - 1; j >= 0; j--) {
      path.append("/").append(pathComponents.get(j));
    }
    return path.toString();
  }

  /**
   * Get the inodes in the directory pointed to by the given absolute HDFS path.
   * <p/>
   * @param path
   * @return
   * @throws IllegalArgumentException If the path does not point to a directory.
   * @throws java.io.FileNotFoundException If the path does not exist.
   */
  public List<Inode> getChildren(String path) throws 
          FileNotFoundException {
    Inode parent = getInode(path);
    if (parent == null) {
      throw new FileNotFoundException("Path not found : " + path);
    } else if (!parent.isDir()) {
      throw new FileNotFoundException("Path is not a directory.");
    }
    return getChildren(parent);
  }

}
