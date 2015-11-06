package se.kth.hopsworks.meta.wscomm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.meta.wscomm.message.Message;
import se.kth.hopsworks.meta.wscomm.message.TextMessage;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Vangelis
 */
@ServerEndpoint(value = "/wspoint/{projectID}",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class,
        configurator = ServletAwareConfig.class)
public class WebSocketEndpoint {

  private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.
          getName());

  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectFacade projectFacade;  
  private String sender;
  private Project project;
  private String userRole;
  private HttpSession httpSession;//this might be used to check the underlying http session
  @EJB
  private Protocol protocol;

  @OnOpen
  public void open(Session session, EndpointConfig config,
          @PathParam("projectID") String projectId) {

    this.sender = (String) config.getUserProperties().get("user");
    this.httpSession = (HttpSession) config.getUserProperties().get(
            "httpSession");

    this.project = this.getProject(projectId);

    if (this.project == null) {
      try {
        sendError(session, "Project does not exist.");
        session.close();
        return;
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }

    Users user = this.projectTeamFacade.findUserByEmail(this.sender);
    //returns the user role in project. Null if the user has no role in project
    this.userRole = this.projectTeamFacade.findCurrentRole(this.project, user);
    logger.log(Level.INFO, "User role in this project {0}", this.userRole);

    if (this.userRole == null) {
      try {
        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                "You do not have a role in this project."));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
    session.getUserProperties().put("projectID", this.project.getId());
  }

  @OnMessage
  public void message(Session session, Message msg) {
    //query string is the client I want to communicate with
    String receiver = session.getQueryString();

    logger.log(Level.INFO, "RECEIVED MESSAGE: {0}", msg.toString());
    Message response = this.protocol.GFR(msg);
    //broadcast the response back to everybody in the same project
    this.broadcast(response, session);
  }

  @OnClose
  public void onClose(Session session) {
    logger.log(Level.INFO,
            "HOPSWORKS: USER {0} SESSION DESTROYED sessions {1}",
            new Object[]{this.sender, session.getOpenSessions().size()});
    Message message = new TextMessage(this.sender, " Left");
    message.setStatus("INFO");
    this.broadcast(message, session);
  }

  //broadcast to every one connected to the same project
  private void broadcast(Message msg, Session session) {
    for (Session s : session.getOpenSessions()) {
      try {
        if (s.isOpen() && s.getUserProperties().get("projectID").equals(
                session.getUserProperties().get("projectID"))) {
          s.getBasicRemote().sendObject(msg);//RemoteEndpoint.Basic interface provides blocking methods to send
          //messages; the RemoteEndpoint.Async interface provides nonblocking methods.
          //logger.log(Level.INFO, "Sending >>> {0} to session: {1}",
                  //new Object[]{msg, s.getUserPrincipal()});
        }
      } catch (IOException | EncodeException ex) {
        this.sendError(session, ex.getMessage());
      }
    }
  }

  @OnError
  public void error(Session session, Throwable t) {
    logger.log(Level.SEVERE, t.getMessage(), t);
  }

  private void sendClient(Session session, Message message) {
    try {
      session.getBasicRemote().sendObject(message);
    } catch (IOException | EncodeException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private void sendError(Session session, String err) {
    String error = String.format("error: %s", err);
    Message message = new TextMessage("Server", error);
    message.setStatus("ERROR");
    this.sendClient(session, message);
  }

  private Project getProject(String projectId) {
    Integer pId;
    Project proj;
    try {
      pId = Integer.valueOf(projectId);
      proj = this.projectFacade.find(pId);
    } catch (NumberFormatException e) {
      return null;
    }
    return proj;
  }

}
