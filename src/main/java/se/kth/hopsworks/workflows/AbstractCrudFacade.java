package se.kth.hopsworks.workflows;

import org.json.JSONObject;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;


public abstract class AbstractCrudFacade<T> extends AbstractFacade<T> {
    private Class<T> entityClass;

    public AbstractCrudFacade(Class<T> entityClass) {
        super(entityClass);
        this.entityClass = entityClass;
    }

    public void update(T entity, JSONObject params){

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityClass);
        Root<T> root = update.from(entityClass);

        for (Object k: params.keySet()){
            String key = k.toString();
            try{
                entityClass.getDeclaredField(key);
                update.set(key, params.get(key));
            }catch(NoSuchFieldException ex){
                continue;
            }
        }
        update.where(updateWhere(entity, root));
        getEntityManager().createQuery(update).executeUpdate();
    }
    public abstract Predicate updateWhere(T entity, Root<T> root);

}
