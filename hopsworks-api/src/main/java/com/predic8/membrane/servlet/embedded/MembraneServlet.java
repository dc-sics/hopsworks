/*
 * Copyright 2012 predic8 GmbH, www.predic8.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.predic8.membrane.servlet.embedded;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.RuleManager;
import com.predic8.membrane.core.rules.ProxyRule;
import com.predic8.membrane.core.rules.ProxyRuleKey;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
public class MembraneServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(MembraneServlet.class);

//  protected static final Pattern TEMPLATE_PATTERN = Pattern.compile(
//          //          "\\{(.+?)\\}");
//          "\\/([0-9]+)\\/");
//  private static final String ATTR_QUERY_STRING = MembraneServlet.class.
//          getSimpleName() + ".queryString";
//  protected static final String P_TARGET_URI = "targetUri";
//  protected static final String ATTR_TARGET_URI = MembraneServlet.class.
//          getSimpleName() + ".targetUri";
//  protected static final String ATTR_TARGET_HOST
//          = MembraneServlet.class.
//          getSimpleName() + ".targetHost";
//  protected String targetUriTemplate;//has {name} parts
//  protected String port;
//  protected String targetUri;
  @Override
  public void init(ServletConfig config) throws ServletException {
//    targetUriTemplate = config.getInitParameter(P_TARGET_URI);
//    if (targetUriTemplate == null) {
//      throw new ServletException(P_TARGET_URI + " is required.");
//    }
//
//    targetUri = config.getInitParameter(P_TARGET_URI);
//    if (targetUri == null) {
//      throw new ServletException(P_TARGET_URI + " is required.");
//    }
  }

  @Override
  public void destroy() {
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {
    String queryString = req.getQueryString() == null ? "" : "?" + req.
            getQueryString();

    Router router;
//    int hash = queryString.indexOf('#');
//    if (hash >= 0) {
//      queryString = queryString.substring(0, hash);
//    }
    List<NameValuePair> pairs;
    try {
      //note: HttpClient 4.2 lets you parse the string without building the URI
      pairs = URLEncodedUtils.parse(new URI(queryString), "UTF-8");
    } catch (URISyntaxException e) {
      throw new ServletException("Unexpected URI parsing error on "
              + queryString, e);
    }
    LinkedHashMap<String, String> params = new LinkedHashMap<>();
    for (NameValuePair pair : pairs) {
      params.put(pair.getName(), pair.getValue());
    }

    StringBuffer urlBuf = new StringBuffer("http://127.0.0.1:");//note: StringBuilder isn't supported by Matcher
//    Matcher matcher = TEMPLATE_PATTERN.matcher(targetUriTemplate);

//    while (matcher.find()) {
//      String arg = matcher.group(1);
//      String replacement = params.remove(arg);//note we remove
//      if (replacement != null) {
//        matcher.appendReplacement(urlBuf, replacement);
//        port = replacement;
//      } else if (port != null) {
//        matcher.appendReplacement(urlBuf, port);
//      } else {
//        throw new ServletException("Missing HTTP parameter " + arg
//                + " to fill the template");
//      }
//    }
    String ctxPath = req.getRequestURI();
    int x = ctxPath.indexOf("/jupyter");
    int firstSlash = ctxPath.indexOf('/', x + 1);
    int secondSlash = ctxPath.indexOf('/', firstSlash + 1);
    String portString = ctxPath.substring(firstSlash + 1, secondSlash);
    urlBuf.append(portString);

//    if (matcher.find()) {
//      String arg = matcher.group(1);
//      String replacement = params.remove(arg);
//      matcher.appendReplacement(urlBuf, portString);
//      matcher.appendTail(urlBuf);
//    }
    String newTargetUri = urlBuf.toString() + req.getRequestURI();
//    req.setAttribute(ATTR_TARGET_URI, newTargetUri);
//    try {
//      targetUriObj = new URI(newTargetUri);
//    } catch (Exception e) {
//      throw new ServletException("Rewritten targetUri is invalid: "
//              + newTargetUri, e);
//    }
//    req.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(
//            targetUriObj));

    //Determine the new query string based on removing the used names
//    StringBuilder newQueryBuf = new StringBuilder(queryString.length());
    StringBuilder newQueryBuf = new StringBuilder();
    newQueryBuf.append(newTargetUri);
//    for (Map.Entry<String, String> nameVal : params.entrySet()) {
//      if (nameVal.getKey().compareToIgnoreCase("token")==0) {
//        newQueryBuf.append('?');
//      } else if (newQueryBuf.length() > 0) {
//        newQueryBuf.append('&');
//      }
//      newQueryBuf.append(nameVal.getKey()).append('=');
//      if (nameVal.getValue() != null) {
//        newQueryBuf.append(nameVal.getValue());
//      }
//    }
//    req.setAttribute(ATTR_QUERY_STRING, newQueryBuf.toString());
    newQueryBuf.append(queryString);

//    Enumeration<String> headerNames = req.getHeaderNames();
//    while (headerNames.hasMoreElements()) {
//      String h = headerNames.nextElement();
//      String header = req.getHeader(h);
//    }
    URI targetUriObj = null;
    try {
      targetUriObj = new URI(newQueryBuf.toString());
    } catch (Exception e) {
      throw new ServletException("Rewritten targetUri is invalid: "
              + newTargetUri, e);
    }
//    req.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));

    ServiceProxy sp = new ServiceProxy(
            new ServiceProxyKey("localhost", "*", "*", -1), "localhost", 8888);
    sp.setTargetURL(newQueryBuf.toString());
    try {
      router = new HopsRouter(targetUriObj);
//      router = new HopsRouter();
      ProxyRule proxy = new ProxyRule(new ProxyRuleKey(-1));
      router.getRuleManager().addProxy(proxy,
              RuleManager.RuleDefinitionSource.MANUAL);
      router.getRuleManager().addProxy(sp,
              RuleManager.RuleDefinitionSource.MANUAL);
      new HopsServletHandler(req, resp, router.getTransport(),
              targetUriObj).run();
    } catch (Exception ex) {
      Logger.getLogger(MembraneServlet.class.getName()).log(Level.SEVERE, null,
              ex);
    }

//    new HttpServletHandler(req, resp, router.getTransport()).run();
  }

}
