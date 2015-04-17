/*
 */
package io.hops.filters;

import io.hops.annotations.LogOperation;
import io.hops.integration.ProjectFacade;
import io.hops.integration.ProjectHistoryFacade;
import io.hops.integration.UserFacade;
import io.hops.model.Project;
import io.hops.model.ProjectHistory;
import io.hops.model.Users;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 * 
 * This intercepter intercepts method invocations that are annotated with the 
 */
@LogOperation
@Interceptor
public class OperationInterceptor implements Serializable {

    private final static Logger logger = Logger.getLogger(OperationInterceptor.class.getName());

    @EJB
    private ProjectHistoryFacade historyBean;
    @EJB
    private UserFacade userBean;
    @EJB
    private ProjectFacade projectBean;
    @Inject
    private HttpServletRequest req;

    public OperationInterceptor() {
    }

    @AroundInvoke
    public Object logInvocation(InvocationContext ctx) throws Exception {
        Method targetMethod = ctx.getMethod();
        LogOperation operation = targetMethod.getAnnotation(LogOperation.class);

        String[] pathParts = req.getPathInfo().split("/");
        Users user;
        Project project;

        logger.log(Level.INFO, "Intercepted method: {0} "
                + "with annotation type: {1} "
                + "and description: {2}", new Object[]{targetMethod.getName(),
                    operation.type(),
                    operation.description()});

        Object returnValue;
        returnValue = ctx.proceed();

        //intercepted method must be a project operations on a specific project
        //with an id (/project/id/...). Project creation will have time stamp so
        //we do not need to sotre that here
        if (pathParts[1].equalsIgnoreCase("project") && pathParts.length > 2) {
            try {
                Integer projectID = Integer.valueOf(pathParts[2]);
                user = userBean.findByEmail(req.getRemoteUser());
                project = projectBean.findByProjectID(projectID);
                ProjectHistory history = new ProjectHistory();//operation.type(),new Date(), operation.description(), user, project
                history.setDatestamp(new Date());
                history.setDescription(operation.description());
                history.setEmail(user);
                history.setOp(operation.type());
                history.setProjectID(project);
                historyBean.persist(history);
            } catch (Exception ex) {
                //we should not bother the user with this exception, the user need not
                //be aware of the history collection, if this fails we should quietly
                //go on with the intercepted operation.
                logger.log(Level.WARNING,">>>>Exception in saving history");
            }
        }
        return returnValue;
    }
}
