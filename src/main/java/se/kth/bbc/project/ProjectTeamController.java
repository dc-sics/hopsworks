package se.kth.bbc.project;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class ProjectTeamController implements Serializable {

  private static final Logger logger = Logger.getLogger(
          ProjectTeamController.class.getName());

  private String toRemoveEmail;
  private String toRemoveName;

  @EJB
  private ProjectTeamFacade teamFacade;

  @EJB
  private ActivityFacade activityFacade;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public void setToRemove(String email, String name) {
    this.toRemoveEmail = email;
    this.toRemoveName = name;
  }

  public void clearToRemove() {
    this.toRemoveEmail = null;
    this.toRemoveName = null;
  }

  public String getToRemoveEmail() {
    return toRemoveEmail;
  }

  public String getToRemoveName() {
    return toRemoveName;
  }

  public synchronized void deleteMemberFromTeam() {
    try {
      Users user = this.teamFacade.findUserByEmail(toRemoveEmail);
      teamFacade.removeProjectTeam(sessionState.getActiveProject(),
              user);
      activityFacade.persistActivity(ActivityFacade.REMOVED_MEMBER
              + toRemoveEmail, sessionState.getActiveProject(), sessionState.
              getLoggedInUsername());
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Deleting team member failed.");
      logger.log(Level.WARNING, "Failed to remove team member " + toRemoveEmail
              + "from project " + sessionState.getActiveProjectname(), ejb);
      return;
    }
    MessagesController.addInfoMessage("Member removed", "Team member "
            + toRemoveEmail
            + " deleted from project " + sessionState.getActiveProjectname());
    clearToRemove();
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }
}
