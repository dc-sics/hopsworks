package io.hops.integration;

import io.hops.model.Users;
import io.hops.services.Username;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Stateless
public class UserFacade extends AbstractFacade<Users> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFacade() {
        super(Users.class);
    }

    @Override
    public List<Users> findAll() {
        TypedQuery<Users> query = em.createNamedQuery("Users.findAll",
                Users.class);
        return query.getResultList();
    }

    public List<Users> findAllByName() {
        TypedQuery<Users> query = em.createNamedQuery("Users.findAllByName",
                Users.class);
        return query.getResultList();
    }

    public List<Users> findAllUsers() {
        Query query = em.createNativeQuery("SELECT * FROM USERS", Users.class);
        return query.getResultList();
    }

    public List<Users> filterUsersBasedOnStudy(String name) {

        Query query = em.createNativeQuery(
                "SELECT * FROM USERS WHERE email NOT IN (SELECT team_member FROM StudyTeam WHERE name=?)",
                Username.class).setParameter(1, name);
        return query.getResultList();
    }

    public void persist(Users user) {
        em.persist(user);
    }

    public void update(Users user) {
        em.merge(user);
    }

    public void removeByEmail(String email) {
        Users user = findByEmail(email);
        if (user != null) {
            em.remove(user);
        }
    }

    @Override
    public void remove(Users user) {
        if (user != null && user.getEmail() != null && em.contains(user)) {
            em.remove(user);
        }
    }

    public Users findByEmail(String email) {
        return em.find(Users.class, email);
    }

    public Users findByName(String name) {
        return em.find(Users.class, name);
    }

    public void detach(Users user) {
        em.detach(user);
    }

    /**
     * Get all users with STATUS = status.
     *
     * @param status
     * @return
     */
    public List<Users> findAllByStatus(int status) {
        TypedQuery<Users> query = em.createNamedQuery("Users.findByStatus",
                Users.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

}
