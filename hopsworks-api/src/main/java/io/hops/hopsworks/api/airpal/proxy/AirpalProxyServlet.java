package io.hops.hopsworks.api.airpal.proxy;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import java.io.IOException;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AirpalProxyServlet extends ProxyServlet {

  private static final Logger logger = Logger.getLogger(
    AirpalProxyServlet.class.
    getName());
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserManager userManager;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;

  @Override
  protected void service(HttpServletRequest servletRequest,
    HttpServletResponse servletResponse) throws ServletException,
    IOException {
    Cookie cookie = null;
    String projectID;
    Cookie[] cookies = servletRequest.getCookies();
    servletrequest.data
    for (int i = 0; i < cookies.length; i++) {

      cookie = cookies[i];
      logger.info("Cookie Name ======>" + cookie.getName());
      logger.info("Cookie Value ======>" + cookie.getValue());
      if(cookie.getName()== "projectId")
      {
        projectID=cookie.getValue();
      }
    }

    //super.service(servletRequest, servletResponse);
  }

}
