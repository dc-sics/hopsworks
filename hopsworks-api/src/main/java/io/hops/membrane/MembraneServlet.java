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
package io.hops.membrane;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.predic8.membrane.core.Router;
import io.hops.membrane.HopsRouter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
public class MembraneServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(MembraneServlet.class);

  private Router router;

  protected static final Pattern TEMPLATE_PATTERN = Pattern.compile(
          "\\{(.+?)\\}");
  private static final String ATTR_QUERY_STRING = MembraneServlet.class.
          getSimpleName() + ".queryString";

  protected static final String P_TARGET_URI = "targetUri";
  protected static final String ATTR_TARGET_URI = MembraneServlet.class.
          getSimpleName() + ".targetUri";
  protected static final String ATTR_TARGET_HOST
          = MembraneServlet.class.
          getSimpleName() + ".targetHost";

  protected String targetUriTemplate;//has {name} parts
  protected String port;

  protected String targetUri;
  protected URI targetUriObj;//new URI(targetUri)
  protected HttpHost targetHost;//URIUtils.extractHost(targetUriObj);  

  @Override
  public void init(ServletConfig config) throws ServletException {
    targetUriTemplate = config.getInitParameter(P_TARGET_URI);
    if (targetUriTemplate == null) {
      throw new ServletException(P_TARGET_URI + " is required.");
    }

    targetUri = config.getInitParameter(P_TARGET_URI);
    if (targetUri == null) {
      throw new ServletException(P_TARGET_URI + " is required.");
    }
  }

  @Override
  public void destroy() {
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {

    String queryString = "?" + req.getQueryString();//no "?" but might have "#"
    int hash = queryString.indexOf('#');
    if (hash >= 0) {
      queryString = queryString.substring(0, hash);
    }
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

    //Now rewrite the URL
    StringBuffer urlBuf = new StringBuffer();//note: StringBuilder isn't supported by Matcher
    Matcher matcher = TEMPLATE_PATTERN.matcher(targetUriTemplate);
    while (matcher.find()) {
      String arg = matcher.group(1);
      String replacement = params.remove(arg);//note we remove
      if (replacement != null) {
        matcher.appendReplacement(urlBuf, replacement);
        port = replacement;
      } else if (port != null) {
        matcher.appendReplacement(urlBuf, port);
      } else {
        throw new ServletException("Missing HTTP parameter " + arg
                + " to fill the template");
      }
    }
    matcher.appendTail(urlBuf);
    String newTargetUri = urlBuf.toString();
    req.setAttribute(ATTR_TARGET_URI, newTargetUri);
    try {
      targetUriObj = new URI(newTargetUri);
    } catch (Exception e) {
      throw new ServletException("Rewritten targetUri is invalid: "
              + newTargetUri, e);
    }
    req.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(
            targetUriObj));

    //Determine the new query string based on removing the used names
    StringBuilder newQueryBuf = new StringBuilder(queryString.length());
    for (Map.Entry<String, String> nameVal : params.entrySet()) {
      if (newQueryBuf.length() > 0) {
        newQueryBuf.append('&');
      }
      newQueryBuf.append(nameVal.getKey()).append('=');
      if (nameVal.getValue() != null) {
        newQueryBuf.append(nameVal.getValue());
      }
    }
    req.setAttribute(ATTR_QUERY_STRING, newQueryBuf.toString());

//    HttpClient.read(req.getInputStream(), true);
//    req.getReader()
    Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String h = headerNames.nextElement();
      String header = req.getHeader(h);
    }
    router = new HopsRouter(targetUriObj);

//    new HttpServletHandler(req, resp, router.getTransport()).run();
    new HopsServletHandler(req, resp, router.getTransport(),
            targetUriObj).run();
  }

}
