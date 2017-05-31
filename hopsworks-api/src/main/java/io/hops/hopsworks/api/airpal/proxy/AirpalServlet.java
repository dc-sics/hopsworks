package io.hops.hopsworks.api.airpal.proxy;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AirpalServlet extends ProxyServlet {

  private static final Logger logger = Logger.getLogger(
    AirpalServlet.class.
    getName());

  @Override
  protected void service(HttpServletRequest servletRequest,
    HttpServletResponse servletResponse) throws ServletException,
    IOException {
    logger.info("this is second servlet=========================");

//    if (servletRequest.getUserPrincipal() == null) {
//      servletResponse.sendError(403, "User is not logged in");
//      return;
//    }

    String projectID = null;
    String email = servletRequest.getUserPrincipal().getName();
    logger.info("email ======>" + email);
    String projectid = servletRequest.getParameter("projectID");
    logger.info("projectid ======>" + projectid);
    String teamRole1 = (String) servletRequest.getAttribute("projectID");
    logger.info("teamRole1 ======>" + teamRole1);
    /*
     * if in case change the projectid and try to access
     */

    servletRequest.setAttribute("teamRole", "DataOWner");

//   
  }

}
