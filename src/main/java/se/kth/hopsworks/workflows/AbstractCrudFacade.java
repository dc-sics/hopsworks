package se.kth.hopsworks.workflows;

import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONObject;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Iterator;


public abstract class AbstractCrudFacade<T> extends AbstractFacade<T> {
    private Class<T> entityClass;

    public AbstractCrudFacade(Class<T> entityClass) {
        super(entityClass);
        this.entityClass = entityClass;
    }

    public void update(T entity, ObjectNode params){

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityClass);
        Root<T> root = update.from(entityClass);
        Iterator<String> fields = params.getFieldNames();
        for (String k; fields.hasNext();){
            String key = fields.next();
            try{
                entityClass.getDeclaredField(key);
                if(params.get(key).isTextual()){
                    update.set(key, params.get(key).getValueAsText());
                    return;
                }
                if(params.get(key).isBoolean()){
                    update.set(key, params.get(key).getValueAsBoolean());
                    return;
                }
                if(params.get(key).isContainerNode()) {
                    update.set(key, params.get(key));
                }
            }catch(NoSuchFieldException ex){
                continue;
            }
        }
        update.where(updateWhere(entity, root));
        getEntityManager().createQuery(update).executeUpdate();
    }
    public abstract Predicate updateWhere(T entity, Root<T> root);

}
