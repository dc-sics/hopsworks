package io.hops.hopsworks.api.airpal.proxy;

import io.hops.hopsworks.api.kibana.MyRequestWrapper;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;

import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.List;
import java.util.Map;

import java.util.logging.Logger;
import javax.ejb.EJB;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.http.Consts;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

public class AirpalProxyServlet extends ProxyServlet {

  private static final Logger logger = Logger.getLogger(
    AirpalProxyServlet.class.
    getName());

  String projectID = null;
  String projid = null;
  List<String> projidlist = new ArrayList<String>();
//  BasicHttpEntity basic = new BasicHttpEntity();
  String referer;
  String email1 = null;
  int count = 0;
  boolean isAuth = false;
  String cmd = null;
  String userRole;
//  String proxyRequestUri;
  String projname;

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamBean;
  @EJB
  private ProjectServiceFacade projectServiceFacade;

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

    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(403, "User is not logged in");
      return;
    }
    String email = servletRequest.getUserPrincipal().getName();
    Users user = userManager.getUserByEmail(email);

    String requestURI = servletRequest.getRequestURI();

    MyRequestWrapper myRequestWrapper = new MyRequestWrapper(
      (HttpServletRequest) servletRequest);

    referer = servletRequest.getHeader("Referer");

    //initialize request attributes from caches if unset by a subclass by this point
    if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
    }
    if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
    }

    // Make the Request
    //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
    HttpRequest proxyRequest;

    //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
    /*
     * Here in this airpal project first it fetches the UI code to show airpal gui
     * Gui requests contains /app so those http requests will go to else loop
     * example: http://bbc2.sics.se:44830/hopsworks-api/airpal/app/stylesheets/airpal.css
     * the below if loop allows only REST requests so its does contain /app
     * example: http://bbc2.sics.se:44830/hopsworks-api/airpal/api/users/admin@kth.se/queries
     */
    if (!servletRequest.getRequestURI().contains("/app") && !servletRequest.getRequestURI().contains("/api/files")) {
//      logger.info("inside AIrpaltable first swtch stmt==============");

      String method = servletRequest.getMethod();
      String[] param = referer.split("=");
      if (param != null && param.length > 1) {

        projid = getProjid(param[1]);
        email1 = getemail(param[1]);
        Project project = projectFacade.find(Integer.parseInt(projid));
        userRole = projectTeamBean.findCurrentRole(project, email).replace(" ", "-");

        boolean isAuth = isAuthorized(projid, email1, user);
        if (!isAuth) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
            "You don't have the access right for this application");
          return;
        }

        /**
         * if the request contains /execute and user is data scientist
         * need to check that query has valid access its happened with below list
         * else through error
         */
        if (servletRequest.getRequestURI().contains(
          "/execute") && method.equalsIgnoreCase("put")) {
          JSONObject body = new JSONObject(myRequestWrapper.getBody());
          String query = (String) body.get("query");
          String[] queryArray = query.split(" ");
          if (queryArray.length > 1) {
            cmd = queryArray[0];
          }

          Object cmdis = cmd.toUpperCase();
          List<String> querycmd = Arrays.asList("SELECT", "COMMIT", "DESCRIBE", "EXPLAIN", "RESET", "ROLLBACK", "SET",
            "SHOW", "VALUES");
          boolean queryin = querycmd.contains(cmd.toUpperCase());

          if (userRole.equalsIgnoreCase("Data-scientist")) {

            if (!queryin) {

              servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                "You want Dataowner previliges to run this query ");
              return;

            }

          }

        }
      }

      String proxyRequestUri = rewriteUrlFromRequest(servletRequest);

      if (!(method.equalsIgnoreCase("post"))) {
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
          || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
          HttpEntityEnclosingRequest eProxyRequest
            = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
          // Add the input entity (streamed)
          //  note: we don't bother ensuring we close the servletInputStream since the container handles it
          eProxyRequest.setEntity(new InputStreamEntity(myRequestWrapper.
            getInputStream(), servletRequest.getContentLength()));
          proxyRequest = eProxyRequest;
        } else {
          proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
        }
      } else if (servletRequest.getHeader("Content-Length") == null && servletRequest.getHeader("Transfer-Encoding")
        == null) {
        proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
      } else {

        String body = myRequestWrapper.getBody();

        proxyRequest = newProxyRequestWithEntity(method, proxyRequestUri, servletRequest, body);
      }

      commonProcess(method, servletRequest, proxyRequest, myRequestWrapper, servletResponse, true);

    } else {

      String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
      String method = servletRequest.getMethod();

      if (proxyRequestUri.contains("?projectID")) {
//    logger.info("email ======>" + email);
        projectID = servletRequest.getParameter("projectID");
        projid = getProjid(projectID);

        Project project = projectFacade.find(Integer.parseInt(projid));
        userRole = projectTeamBean.findCurrentRole(project, email).replace(" ", "-");

        String projname = null;
        List<String> projects = projectController.findProjectNamesByUser(email, true);
        for (String projectStr : projects) {
          if (projectStr != null) {
            String id = project.getId().toString();
            if (id.equalsIgnoreCase(projid)) {
              projname = project.getName();
            }
          }
        }

        boolean isAuth = isAuthorized(projid, email, user);
        if (!isAuth) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
            "You don't have the access right for this application");
          return;
        }

        proxyRequestUri = proxyRequestUri + "_" + projname + "_" + userRole;

        log("proxy request uri===********==== " + proxyRequestUri);
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
          || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
          HttpEntityEnclosingRequest eProxyRequest
            = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
          // Add the input entity (streamed)
          // note: we don't bother ensuring we close the servletInputStream since 
          // the container handles it
          eProxyRequest.setEntity(new InputStreamEntity(servletRequest.
            getInputStream(), servletRequest.getContentLength()));
          proxyRequest = eProxyRequest;
        } else {

          proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
        }

        commonProcess(method, servletRequest, proxyRequest, myRequestWrapper, servletResponse, false);

      } else {
        super.service(servletRequest, servletResponse);
      }
    }

  }

  private void commonProcess(String method, HttpServletRequest servletRequest, HttpRequest proxyRequest,
    MyRequestWrapper myRequestWrapper, HttpServletResponse servletResponse, boolean flag) throws ServletException,
    RuntimeException,
    IOException {

    copyRequestHeaders(servletRequest, proxyRequest);

    super.setXForwardedForHeader(servletRequest, proxyRequest);

    HttpResponse proxyResponse = null;

    int statusCode = 0;
    try {
      // Execute the request
      if (doLog) {
        log("proxy " + method + " uri: " + servletRequest.getRequestURI()
          + " -- " + proxyRequest.getRequestLine().getUri());
      }
      if (flag) {

        proxyResponse = super.proxyClient.execute(super.getTargetHost(
          myRequestWrapper), proxyRequest);

        // Process the response
        statusCode = proxyResponse.getStatusLine().getStatusCode();
//        logger.info("proxyResponse statuscode ==============" + statusCode);

        if (doResponseRedirectOrNotModifiedLogic(myRequestWrapper, servletResponse,
          proxyResponse, statusCode)) {
//          log("Inside doResponseRedirectOrNotModifiedLogic======= ");
          //the response is already "committed" now without any body to send
          //TODO copy response headers?
          return;
        }
      } else {
        proxyResponse = super.proxyClient.execute(super.getTargetHost(
          servletRequest), proxyRequest);

        // Process the response
        statusCode = proxyResponse.getStatusLine().getStatusCode();
        logger.info("proxyResponse statuscode ==============" + statusCode);

        if (doResponseRedirectOrNotModifiedLogic(servletRequest, servletResponse,
          proxyResponse, statusCode)) {
//          log("Inside doResponseRedirectOrNotModifiedLogic======= ");
          //the response is already "committed" now without any body to send
          //TODO copy response headers?
          return;
        }
      }
//      log("Outside doResponseRedirectOrNotModifiedLogic======= ");
      // Pass the response code. This method with the "reason phrase" is
      // deprecated but it's the only way to pass the reason along too.
      //noinspection deprecation
      servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().
        getReasonPhrase());

      copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
//        logger.info("proxyResponse statuscode ==============" + statusCode);
// Send the content to the client
      copyResponseEntity(proxyResponse, servletResponse);

    } catch (Exception e) {
      //abort request, according to best practice with HttpClient
      if (proxyRequest instanceof AbortableHttpRequest) {
        AbortableHttpRequest abortableHttpRequest
          = (AbortableHttpRequest) proxyRequest;
        abortableHttpRequest.abort();
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      if (e instanceof ServletException) {
        throw (ServletException) e;
      }
      //noinspection ConstantConditions
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new RuntimeException(e);

    } finally {
      // make sure the entire entity was consumed, so the connection is released
      if (proxyResponse != null) {
        consumeQuietly(proxyResponse.getEntity());
      }
      //Note: Don't need to close servlet outputStream:
      // http://stackoverflow.com/questions/1159168/should-one-call-close-on-
      //httpservletresponse-getoutputstream-getwriter
    }
  }


  /*
   * to handle post request had content type is application/x-www-form-urlencoded
   */
  protected HttpRequest newProxyRequestWithEntity(
    String method, String proxyRequestUri, HttpServletRequest servletRequest, String body) throws
    IOException {

    HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);

    String contentType = servletRequest.getContentType();

    boolean isFormPost = (contentType != null
      && contentType.contains("application/x-www-form-urlencoded")
      && "POST".equalsIgnoreCase(servletRequest.getMethod()));

    if (isFormPost) {
      List<NameValuePair> queryParams = Collections.emptyList();
      String queryString = servletRequest.getQueryString();
      if (queryString != null) {
        queryParams = URLEncodedUtils.parse(queryString, Consts.UTF_8);
      }

      Map<String, String[]> form = servletRequest.getParameterMap();
      List<NameValuePair> params = new ArrayList<>();

      String s1;
      if (body.charAt(0) == '&') {
        s1 = body.replaceAll("%20", " ").substring(1);
      } else {
        s1 = body.replaceAll("%20", " ");
      }

      String[] s2 = s1.split("&");

      for (String s : s2) {
        String[] s3 = s.split("=");

        if (s3.length <= 2) {
          params.add(new BasicNameValuePair(s3[0], s3[1]));
        }

      }

      eProxyRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    } else {
      eProxyRequest.setEntity(
        new InputStreamEntity(servletRequest.getInputStream(), servletRequest.getContentLength()));
    }
    return eProxyRequest;
  }

  private boolean isAuthorized(String projid, String email, Users user) {
    /*
     * if in case change the projectid and try to access
     */
    List<String> projects = projectController.findProjectNamesByUser(
      email, true);
    // trying to get project id for each project
    for (String projectStr : projects) {
//      logger.info("response ==============" + projectStr);
      Project project = projectFacade.findByName(projectStr);
      String id = project.getId().toString();
      if (id.equalsIgnoreCase(projid)) {

//        logger.info("project       ==============444444" + project);
        for (ProjectTeam pt : project.getProjectTeamCollection()) {
          if (pt.getUser().equals(user)) {
//            inTeam = true;
            break;
          }
        }
        List<ProjectServices> projectServiceslist = projectServiceFacade.getAllProjectServicesForProject(project);
//        logger.info("projectServiceslist        ==============444444" + projectServiceslist);
        for (ProjectServices projectServices : projectServiceslist) {

          ProjectServiceEnum projectServiceEnum = projectServices.getProjectServicesPK().getService();
//          logger.info("projectServiceEnum continueeeeeeeeeeeeeeeeeeeeee" + projectServiceEnum);
          if (projectServiceEnum != null && projectServiceEnum.equals(ProjectServiceEnum.AIRPAL)) {
//            projservicebool = true;
            isAuth = true;
            logger.info("he is valid user continueeeeeeeeeeeeeeeeeeeeee");
            return isAuth;
          }

        }
        break;
      }

    }

    return isAuth = false;
  }

  private String getProjid(String projectID) {
    String[] refparam = projectID.split("_");
    if (refparam != null && refparam.length > 0) {
      return refparam[0];
    }
    return null;
  }

  private String getemail(String projectID) {
    String[] refparam = projectID.split("_");
    if (refparam != null && refparam.length > 1) {
      return refparam[1];
    }
    return null;
  }

  private String getProjectName(String projid, String email) {

    List<String> projects = projectController.findProjectNamesByUser(
      email, true);

    if (!(projects.isEmpty()) && projects.size() > 1) {
      for (String projectStr : projects) {
        if (projectStr != null) {
          Project project = projectFacade.findByName(projectStr);
          String id = project.getId().toString();
          if (id.equalsIgnoreCase(projid)) {
            return project.getName();
          }
        }
      }
    } else {
      logger.info("getprojectname==projects is null===");
    }
    return null;
  }

}
