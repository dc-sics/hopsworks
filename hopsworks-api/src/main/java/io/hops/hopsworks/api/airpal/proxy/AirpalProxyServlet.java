package io.hops.hopsworks.api.airpal.proxy;

import io.hops.hopsworks.api.kibana.MyRequestWrapper;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
//import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
//import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
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
  @EJB
  private ProjectFacade projectFacade;
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
    Cookie cookie = null;

    String requestURI = servletRequest.getRequestURI();

    String email = servletRequest.getUserPrincipal().getName();
//    logger.info("email ======>" + email);
    projectID = servletRequest.getParameter("projectID");
    logger.info("projectid ======>" + projectID);
    String teamRole1 = (String) servletRequest.getAttribute("projectID");
    /*
     * if in case change the projectid and try to access
     */
    MyRequestWrapper myRequestWrapper = new MyRequestWrapper(
      (HttpServletRequest) servletRequest);

    AirpalFilter airpalFilter = null;
    boolean x = false;
    if (servletRequest.getRequestURI().contains(
      "/api/table")) {
      airpalFilter = airpalFilter.Airpal_Table;
      x = true;
    }
    if (x) {
      logger.info("inside AIrpaltable first swtch stmt==============");
      logger.info("Requesturi of api requests ==============1111" + requestURI);
      String refererURI = servletRequest.getHeader("Referer");
      logger.info("refereuri of api requests ==============222222" + refererURI);
      String[] param = refererURI.split("=");
      logger.info("refereuri of api requests ==============33333" + param[1]);
      String[] refparam = param[1].split("_");
      projid = refparam[0];
      String email1 = refparam[1];
      logger.info("refereuri projid of api requests ==============444444" + projid);

      //initialize request attributes from caches if unset by a subclass by this point
      if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
        servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
      }
      if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
        servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
      }

      // Make the Request
      //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
      String method = servletRequest.getMethod();
      String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
      HttpRequest proxyRequest;
      //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
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
      logger.info("default stmt in first switch=========================");
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
//          logger.info("inside AIrpal filtee  second swtch stmt==============");
          logger.info("inside AIrpal filtee  second swtch stmt==============" + projectID + email);
          HttpEntity entity = proxyResponse.getEntity();
          logger.info("entity ==============+" + entity);
          if (entity != null) {
            logger.info("entity inside ==============+" + entity);
            GzipDecompressingEntity gzipEntity = new GzipDecompressingEntity(
              entity);
            logger.info("above   response ==============+");
            String resp = EntityUtils.toString(gzipEntity);
            logger.info("response ==============+" + resp);
            //list of project names
            List<String> projects = projectController.findProjectNamesByUser(
              email, true);
            String[] projArray = projects.toArray(new String[0]);
            // trying to get project id for each project
            for (int i = 0; i < projArray.length; i++) {
              logger.info("response ==============" + projArray[i]);
              Project project = projectFacade.findByName(projArray[i]);
              String id = project.getId().toString();
              projidlist.add(id);
            }
            String[] projidArray = projidlist.toArray(new String[0]);
            logger.info("projects ==============+" + projects);
            logger.info("projidlist ==============+" + projidArray);
            BasicHttpEntity basic = new BasicHttpEntity();
            JSONArray jsonarray = new JSONArray(resp);
            for (int i = 0; i < jsonarray.length(); i++) {
              JSONObject jsonobject = jsonarray.getJSONObject(i);
              String tname = jsonobject.getString("tableName");
              String[] tnameArray = tname.split("_");
              String proj = tnameArray[1];
//              Project project=projectFacade.find(proj);
//              String name = project.getName();

              for (int j = 0; j < projidArray.length; j++) {
                if ((projidArray[j].equalsIgnoreCase(proj))) {
                  jsonarray.remove(i);
                }
              }

            }
            logger.info("projects ==============+" + jsonarray);

//            JSONObject indices = new JSONObject(resp);
//            logger.info("inside AIrpaltable first swtch stmt JSONObject==============+" + indices);
//            //Remove all projects other than the current one and check
//            //if user is authorizer to access it
//            List<String> projects = projectController.findProjectNamesByUser(
//              email, true);
//            logger.info("projects ==============+" + projects);
//            JSONArray hits = indices.getJSONObject("hits").getJSONArray("hits");
//
//            for (int i = hits.length() - 1; i >= 0; i--) {
//              String tableName = hits.getJSONObject(i).getString("tableName");
//
//              String[] table = tableName.split("_");
//              int projid = Integer.parseInt(table[1]);
//              logger.info("inside AIrpaltable first swtch stmt==============+" + projid);
//              if (!projects.contains(projid)) {
//                hits.remove(i);
//              }
//            }
            InputStream in = IOUtils.toInputStream(jsonarray.toString());
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

}
