package io.hops.hopsworks.api.airpal.proxy;

import io.hops.hopsworks.api.kibana.MyRequestWrapper;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
//import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
//import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
//import org.apache.http.HttpEntityEnclosingRequest;
//import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class AirpalProxyServlet extends ProxyServlet {

  private static final Logger logger = Logger.getLogger(
    AirpalProxyServlet.class.
    getName());

  static String projectID = null;
  String projid = null;
  List<String> projidlist = new ArrayList<String>();
  JSONArray resultArray = new JSONArray();
  BasicHttpEntity basic = new BasicHttpEntity();
  String referrer;
  String appID = null;
  String email1 = null;
  int count = 0;
  String service = "Airpal";
  Object serviceobj = service;
  boolean inTeam = false;
  boolean projservicebool = false;
  boolean projidbool = false;
  boolean isAuth = false;
  String cmd = null;
  String someMessage = null;
  String userRole;

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

    String refererURI = servletRequest.getHeader("Referer");

    AirpalFilter airpalFilter = null;
    boolean x = false;
    if (servletRequest.getRequestURI().contains(
      "/api/table")) {

      if (servletRequest.getRequestURI().contains(
        "/api/table/")) {
        airpalFilter = airpalFilter.Airpal_Table_columns;
//        x = true;
      } else {

        airpalFilter = airpalFilter.Airpal_Table;
//        x = true;
      }
    } else if (servletRequest.getRequestURI().contains(
      "/api/execute")) {
      airpalFilter = airpalFilter.Airpal_execute;
    }
    logger.info("servletRequest.getRequestURI() ======>" + servletRequest.getRequestURI());
    logger.info("servletRequest.getRequestURL() ======>" + servletRequest.getRequestURL().toString());
    //initialize request attributes from caches if unset by a subclass by this point
    if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
    }
    if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
    }
    referrer = servletRequest.getHeader("referer");
    // Make the Request
    //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
    String method = servletRequest.getMethod();
    logger.info("request method called ==============" + method);
    String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
    HttpRequest proxyRequest;
    //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
    logger.info("proxyRequestUri ======>" + proxyRequestUri);

    if (!servletRequest.getRequestURI().contains("/app") && !servletRequest.getRequestURI().contains("/api/files")) {
//      logger.info("inside AIrpaltable first swtch stmt==============");
      logger.info("Requesturi of api requests ==============" + requestURI);

      logger.info("referer of api requests ==============" + refererURI);
      String[] param = refererURI.split("=");
      if (param != null && param.length > 1) {
        logger.info("refereuri of api requests ==============" + param[1]);
        projid = getProjid(param[1]);
        email1 = getemail(param[1]);
        logger.info("refereuri projid of api requests ==============" + projid);
        boolean isAuth = isAuthorized(projid, email1, user);
        if (!isAuth) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
            "You don't have the access right for this application");
          return;
        }

      }
      if (servletRequest.getRequestURI().contains(
        "/execute") && method.equalsIgnoreCase("put")) {
        JSONObject body = new JSONObject(myRequestWrapper.getBody());
        String query = (String) body.get("query");

        logger.info("execute request put method ======body 177========" + body);

        logger.info("execute request put method ======query 175========" + query);
        String[] queryArray = query.split(" ");
        if (queryArray.length > 1) {
          cmd = queryArray[0];
          Project project = projectFacade.find(Integer.parseInt(projid));
          userRole = projectTeamBean.findCurrentRole(project, email);
          if (userRole.equalsIgnoreCase(AllowedRoles.DATA_SCIENTIST)) {
            if (!(cmd.equalsIgnoreCase("select"))) {
              QueryDialog.infoBox("You dont have access to execute this query",
                "Access Denied");

            }
          }
        }

      }

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
        proxyRequest = newProxyRequestWithEntity(method, proxyRequestUri, servletRequest);
      }

      copyRequestHeaders(servletRequest, proxyRequest);

      super.setXForwardedForHeader(servletRequest, proxyRequest);

      HttpResponse proxyResponse = null;
      try {
        // Execute the request
        if (doLog) {
          log("proxy " + method + " uri: " + servletRequest.getRequestURI()
            + " -- " + proxyRequest.getRequestLine().getUri());
        }

        proxyResponse = super.proxyClient.execute(super.getTargetHost(
          myRequestWrapper), proxyRequest);

        // Process the response
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        logger.info("proxyResponse statuscode ==============" + statusCode);

        if (doResponseRedirectOrNotModifiedLogic(myRequestWrapper, servletResponse,
          proxyResponse, statusCode)) {
          log("Inside doResponseRedirectOrNotModifiedLogic======= ");
          //the response is already "committed" now without any body to send
          //TODO copy response headers?
          return;
        }
        log("Outside doResponseRedirectOrNotModifiedLogic======= ");
        // Pass the response code. This method with the "reason phrase" is 
        // deprecated but it's the only way to pass the reason along too.
        //noinspection deprecation
        servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().
          getReasonPhrase());

        copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
        logger.info("proxyResponse statuscode ==============" + statusCode);
        // Send the content to the client
        copyResponseEntity(proxyResponse, servletResponse, projid, email1, airpalFilter);

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

    } else {
      logger.info("else stamt request uri.contains(/app)=========================");

      if (proxyRequestUri.contains("?projectID")) {
//    logger.info("email ======>" + email);
        projectID = servletRequest.getParameter("projectID");
        projid = getProjid(projectID);
        logger.info("projectid in else 1st 251 if  ======>" + projectID);
        boolean isAuth = isAuthorized(projid, email, user);
        if (!isAuth) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
            "You don't have the access right for this application");
          return;
        }
      }

      super.service(servletRequest, servletResponse);
      log("End of Service method in  AirpalProxySerlet======= ");

    }

  }

  private void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse, String projectID,
    String email, AirpalFilter airpalFilter) throws
    IOException {
    if (airpalFilter == null) {
      super.copyResponseEntity(proxyResponse, servletResponse);
    } else {
      switch (airpalFilter) {
        case Airpal_Table:

          HttpEntity entity = proxyResponse.getEntity();

          if (entity != null) {

            GzipDecompressingEntity gzipEntity = new GzipDecompressingEntity(
              entity);

            String resp = EntityUtils.toString(gzipEntity);

            //list of project names
            List<String> projects = projectController.findProjectNamesByUser(
              email, true);
            String[] projArray = projects.toArray(new String[0]);
            // trying to get project id for each project
//            for (int i = 0; i < projArray.length; i++) {
//              logger.info("response ==============" + projArray[i]);
//              Project project = projectFacade.findByName(projArray[i]);
//              String id = project.getId().toString();
            JSONArray jsonarray = new JSONArray(resp);
            for (int j = 0; j < jsonarray.length() - 1; j++) {
              JSONObject jsonobject = jsonarray.getJSONObject(j);
              String tname = jsonobject.getString("tableName");
              String[] tnameArray = tname.split("_");
              String proj = tnameArray[1];
              if (projectID.equalsIgnoreCase(proj)) {
                resultArray.put(jsonobject);
                logger.info("resultArray ==============" + resultArray);
              }
            }
            //logger.info("resultArray ===finallllllll===========" + resultArray);
//            }
            InputStream in = IOUtils.toInputStream(resultArray.toString());
            OutputStream servletOutputStream = servletResponse.getOutputStream();
            basic.setContent(in);
            GzipCompressingEntity compress = new GzipCompressingEntity(basic);
            compress.writeTo(servletOutputStream);

          }
          break;
        default:
          super.copyResponseEntity(proxyResponse, servletResponse);
          break;
      }
    }
  }

  protected HttpRequest newProxyRequestWithEntity(
    String method, String proxyRequestUri, HttpServletRequest servletRequest) throws IOException {
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

      OUTER_LOOP:
      for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
        String name = nameIterator.next();

        // skip parameters from query string
        for (NameValuePair queryParam : queryParams) {
          if (name.equals(queryParam.getName())) {
            continue OUTER_LOOP;
          }
        }

        String[] value = form.get(name);
        if (value.length != 1) {
          throw new RuntimeException("expecting one value in post form");
        }
        params.add(new BasicNameValuePair(name, value[0]));
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
      logger.info("response ==============" + projectStr);
      Project project = projectFacade.findByName(projectStr);
      String id = project.getId().toString();
      if (id.equalsIgnoreCase(projid)) {
        projidbool = true;
        logger.info("project       ==============444444" + project);
        for (ProjectTeam pt : project.getProjectTeamCollection()) {
          if (pt.getUser().equals(user)) {
            inTeam = true;
            break;
          }
        }
        List<ProjectServices> projectServiceslist = projectServiceFacade.getAllProjectServicesForProject(project);
        logger.info("projectServiceslist        ==============444444" + projectServiceslist);
        for (ProjectServices projectServices : projectServiceslist) {

          ProjectServiceEnum projectServiceEnum = projectServices.getProjectServicesPK().getService();
          logger.info("projectServiceEnum continueeeeeeeeeeeeeeeeeeeeee" + projectServiceEnum);
          if (projectServiceEnum != null && projectServiceEnum.equals(ProjectServiceEnum.AIRPAL)) {
            projservicebool = true;
            isAuth = true;
            logger.info("he is valid user continueeeeeeeeeeeeeeeeeeeeee");
            return isAuth;
          }

        }
        break;
      }

    }

    logger.info("isAuth===============" + isAuth);
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

}
