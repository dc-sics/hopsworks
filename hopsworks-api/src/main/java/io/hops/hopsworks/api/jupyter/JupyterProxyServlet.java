package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.kibana.*;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import java.io.IOException;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JupyterProxyServlet extends ProxyServlet {

  @EJB
  private UserManager userManager;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;

  @Override
  protected void service(HttpServletRequest servletRequest,
          HttpServletResponse servletResponse)
          throws ServletException, IOException {
    
    if ("websocket".equals(servletRequest.getHeader("Upgrade")) == false) {
      // error
    }
    if ("upgrade".equals(servletRequest.getHeader("Connection")) == false) {
      // error
    }
    
//	case "websocket":
//       proxyHeaders.Add("Connection", "{>Connection}")
//			proxyHeaders.Add("Upgrade", "{>Upgrade}")    
    
//    super.service(servletRequest, servletResponse);

  }
}
