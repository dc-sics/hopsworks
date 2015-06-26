package se.kth.bbc.security.ua;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;

/**
 * This class validates the user information upon registration.
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class UsernameValidator implements Validator {

  @EJB
  private UserManager mgr;

  // The pattern for email validation
  private static final String EMAIL_PATTERN
          = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
          + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";

  /**
   * Ensure the the username is available.
   *
   * @param context
   * @param component
   * @param value
   * @throws ValidatorException
   */
  @Override
  public void validate(FacesContext context, UIComponent component,
          Object value) throws ValidatorException {

    String uname = value.toString();

    if (!isValidEmail(uname)) {

      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.INVALID_EMAIL_FORMAT);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);

    }

    if (mgr.isUsernameTaken(uname)) {
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.EMAIL_TAKEN);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }

  }

  /**
   * Check if the email is a valid format.
   *
   * @param u
   * @return
   */
  public boolean isValidEmail(String u) {
    Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    Matcher matcher = pattern.matcher(u);
    return matcher.matches();
  }

}
