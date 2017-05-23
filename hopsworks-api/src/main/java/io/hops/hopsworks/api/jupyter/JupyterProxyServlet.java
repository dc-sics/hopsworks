package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import java.io.IOException;
import java.util.Enumeration;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

/**
 * http://localhost:8080/hopsworks-api/jupyter/?token=44952b2c4d196aeb794b87395abe3b19ecd46e58ba976231?_port=8888
 * <p>
 * http://10.0.2.15:8888/?token=44952b2c4d196aeb794b87395abe3b19ecd46e58ba976231
 * 
 * For Jupyterhub, for any URL spaces /(user/[^/]*)/(api/kernels/[^/]+/channels|terminals/websocket)/?, forward to 
 * ws(s)://servername:port_number, all other standard spaces, 
 * forward to http(s)://servername:port_number, that will do the trick!
 * 
 * 
 * <p>
 */
public class JupyterProxyServlet extends URITemplateProxyServlet {

  @EJB
  private UserManager userManager;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;

  /**
   * These are the "hop-by-hop" headers that should not be copied.
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
   * I use an HttpClient HeaderGroup class instead of Set<String> because this
   * approach does case insensitive lookup faster.
   */
  protected static final HeaderGroup wsHopByHopHeaders;

  static {
    // Allow 'Upgrade' hop-by-hop header to pass through
    // Also allow "Keep-Alive" to pass through
    // Also "Connection"
    wsHopByHopHeaders = new HeaderGroup();
    String[] headers = new String[]{
      "Proxy-Authenticate", "Keep-Alive", "Proxy-Authorization",
      "TE", "Trailers", "Transfer-Encoding"};
    for (String header : headers) {
      wsHopByHopHeaders.addHeader(new BasicHeader(header, null));
    }
  }

  @Override
  protected void service(HttpServletRequest servletRequest,
          HttpServletResponse servletResponse)
          throws ServletException, IOException {

//    if ("websocket".equals(servletRequest.getHeader("Upgrade")) == false) {
//      // error
//    }
//    if ("upgrade".equals(servletRequest.getHeader("Connection")) == false) {
//      // error
//    }
//	case "websocket":
//       proxyHeaders.Add("Connection", "{>Connection}")
//			proxyHeaders.Add("Upgrade", "{>Upgrade}")    
    super.service(servletRequest, servletResponse);

  }

  @Override
  protected void copyRequestHeaders(HttpServletRequest servletRequest,
          HttpRequest proxyRequest) {
    // Get an Enumeration of all of the header names sent by the client
    Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
    while (enumerationOfHeaderNames.hasMoreElements()) {
      String headerName = (String) enumerationOfHeaderNames.nextElement();
      //Instead the content-length is effectively set via InputStreamEntity
      if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
        continue;
      }
      if (wsHopByHopHeaders.containsHeader(headerName)) {
        continue;
      }

      Enumeration headers = servletRequest.getHeaders(headerName);
      while (headers.hasMoreElements()) {//sometimes more than one value
        String headerValue = (String) headers.nextElement();
        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
          HttpHost host = getTargetHost(servletRequest);
          headerValue = host.getHostName();
          if (host.getPort() != -1) {
            headerValue += ":" + host.getPort();
          }
        } else if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
          headerValue = getRealCookie(headerValue);
        }
        proxyRequest.addHeader(headerName, headerValue);
      }
    }
  }

  /**
   * Copy proxied response headers back to the servlet client.
   *
   * @param proxyResponse
   * @param servletRequest
   * @param servletResponse
   */
  @Override
  protected void copyResponseHeaders(HttpResponse proxyResponse,
          HttpServletRequest servletRequest,
          HttpServletResponse servletResponse) {
    for (Header header : proxyResponse.getAllHeaders()) {
      if (wsHopByHopHeaders.containsHeader(header.getName())) {
        continue;
      }
      if (header.getName().
              equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) || header.
              getName().equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
        copyProxyCookie(servletRequest, servletResponse, header);
      } else {
        servletResponse.addHeader(header.getName(), header.getValue());
      }
    }
  }

}
