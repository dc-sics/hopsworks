package io.hops.hopsworks.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * Simple servlet to dynamically set the Websocket and
 * the Rest api ports in the JavaScript sent to the client
 * <p/>
 */
public class AppScriptServlet extends HttpServlet {

  private final static Logger logger = Logger.getLogger(AppScriptServlet.class.
          getName());

  public AppScriptServlet() {
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    try (PrintWriter out = response.getWriter()) {

      ServletContext context = getServletContext();
      // Read the script file chunk by chunk
      InputStream is = context.getResourceAsStream(
              "/zeppelin/app.4ece19b57e6fb1511450.js");
      StringBuilder script = new StringBuilder();
      byte[] buffer = new byte[1024];
      while (is.available() > 0) {
        int numRead = is.read(buffer);
        if (numRead <= 0) {
          break;
        }
        script.append(new String(buffer, 0, numRead, "UTF-8"));
      }

      // Replace the string "function getRestApiBase(){...}" to return
      // the proper value 
      int startIndexRest = script.indexOf("this.getRestApiBase=function(){");
      int endIndexRest = script.indexOf("};", startIndexRest);

      if (startIndexRest >= 0 && endIndexRest >= 0) {
        String replaceStringRest
                = "this.getRestApiBase=function(){return location.protocol + '//'"
                + " + location.hostname + ':' + this.getPort() "
                + " + '/hopsworks-api/api/zeppelin/'+getCookie('projectID');}; "
                + "var getCookie = function (cname) { var name = cname + '=';" 
                + "var ca = document.cookie.split(';'); "
                + "for(var i = 0; i < ca.length; i++) { var c = ca[i]; "
                + "while (c.charAt(0) == ' ') { c = c.substring(1); } "
                + "if(c.indexOf(name)==0){return c.substring(name.length, c.length); "
                + "}} return ''; }";
        script.replace(startIndexRest, endIndexRest + 1, replaceStringRest);
      }

      // Replace the string "function getWebsocketUrl(){...}" to return
      // the proper value with project id
      int startIndexWs = script.indexOf("this.getWebsocketUrl=function(){");
      int endIndexWs = script.indexOf("},", startIndexWs);

      if (startIndexWs >= 0 && endIndexWs >= 0) {
        String replaceStringWs
                = "this.getWebsocketUrl=function(){var wsProtocol=location.protocol"
                + "==='https:'?'wss:':'ws:'; return wsProtocol+'//'+location.hostname+'"
                + ":'+this.getPort()+'/hopsworks-api/zeppelin/ws/'+getCookie('projectID');}";
        script.replace(startIndexWs, endIndexWs + 1, replaceStringWs);
      }
      out.println(script.toString());
    }
  }
}
