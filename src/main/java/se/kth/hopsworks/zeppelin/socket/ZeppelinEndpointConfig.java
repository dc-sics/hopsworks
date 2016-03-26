package se.kth.hopsworks.zeppelin.socket;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ZeppelinEndpointConfig extends ServerEndpointConfig.Configurator {

  private static final Logger logger = Logger.getLogger(
          ZeppelinEndpointConfig.class.
          getName());

  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

    HttpSession httpSession = (HttpSession) request.getHttpSession();
    Map<String, String> cookies = new HashMap<>();
    String cookie = request.getHeaders().get("Cookie").get(0);
    String[] cookieParts = cookie.split("; ");//should have space after ;
    for (String c : cookieParts) {
      cookies.put(c.substring(0, c.indexOf("=")), c.
              substring(c.indexOf("=") + 1, c.length()));
    }
    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", request.getUserPrincipal().getName());
    config.getUserProperties().put("projectID", cookies.get("projectID"));
    logger.log(Level.FINE, "Connecting to zeppelin: User={0} Project id={1}",
            new Object[]{cookies.get("email"), cookies.get("projectID")});
  }

//  @Override
//  public boolean checkOrigin(String originHeaderValue) {
//    try {
//      return SecurityUtils.isValidOrigin(originHeaderValue, zeppelin.getConf());
//    } catch (UnknownHostException | URISyntaxException e) {
//      logger.log(Level.INFO, "{0}", e.getMessage());
//    }
//
//    return false;
//  } 
}
