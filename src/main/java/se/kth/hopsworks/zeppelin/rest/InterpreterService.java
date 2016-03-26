package se.kth.hopsworks.zeppelin.rest;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfigFactory;
import se.kth.hopsworks.zeppelin.util.ZeppelinResource;

@Path("/interpreter")
@Produces("application/json")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
public class InterpreterService {
  Logger logger = LoggerFactory.getLogger(InterpreterService.class);
  
  @EJB
  private ZeppelinResource zeppelinResource;
  @EJB
  private ZeppelinConfigFactory zeppelinConfFactory;
  @EJB
  private UserFacade userBean;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @Inject
  private InterpreterRestApi interpreterRestApi;

  @Path("/")
  public InterpreterRestApi interpreter(@Context HttpServletRequest httpReq)
          throws AppException {
    Project project = zeppelinResource.getProjectNameFromCookies(httpReq);
    ZeppelinConfig zeppelinConf = zeppelinConfFactory.getZeppelinConfig(project.
            getName());
    Users user = userBean.findByEmail(httpReq.getRemoteUser());
    String userRole = projectTeamBean.findCurrentRole(project, user);

    if (userRole == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
                            "You curently have no role in this project!");
    }
    interpreterRestApi.setParms(project, userRole, zeppelinConf);
    return interpreterRestApi;
  }

}
