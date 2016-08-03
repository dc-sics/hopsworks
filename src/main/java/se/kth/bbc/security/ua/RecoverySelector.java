package se.kth.bbc.security.ua;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.bbc.security.auth.QRCodeGenerator;
import se.kth.hopsworks.user.model.Users;

@ManagedBean
@SessionScoped
public class RecoverySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager um;

  @EJB
  private EmailBean email;

  @EJB
  private AuditManager am;

  private Users people;

  private String console;

  // Quick response code URL
  private String qrUrl = "Pass";
  private StreamedContent qrCode = null;

  private String uname;
  private String tmpCode;
  private String passwd;

  private final int passwordLength = AuthenticationConstants.PASSWORD_MIN_LENGTH;

  private int qrEnabled = -1;

  public int getQrEnabled() {
    return qrEnabled;
  }

  public void setQrEnabled(int qrEnabled) {
    this.qrEnabled = qrEnabled;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }

  public StreamedContent getQrCode() {
    return qrCode;
  }

  public void setQrCode(StreamedContent qrCode) {
    this.qrCode = qrCode;
  }

  public String getUname() {
    return uname;
  }

  public void setUname(String uname) {
    this.uname = uname;
  }

  public String getTmpCode() {
    return tmpCode;
  }

  public void setTmpCode(String tmpCode) {
    this.tmpCode = tmpCode;
  }

  public String getPasswd() {
    return passwd;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  public String getConsole() {
    return console;
  }

  public void setConsole(String console) {
    this.console = console;
  }

  public String redirect() {

    if (console.equals("Password")) {
      return "sec_question";
    }

    if (console.equals("Mobile")) {
      return "mobile_recovery";
    }

    if (console.equals("Yubikey")) {
      return "yubikey_recovery";
    }

    return "";
  }

  /**
   * Register lost mobile lost device.
   * <p>
   * @return
   * @throws SocketException
   */
  public String sendQrCode() throws SocketException {

    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.BLOCKED_ACCOUNT.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);

      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.DEACTIVATED_ACCOUNT.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
      return "";
    }

    if (people.getMode() == PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

      return "";
    }

    try {

      if (people.getPassword().equals(DigestUtils.sha256Hex(passwd))) {

        // generate a randome secret of legth 6
        String random = SecurityUtils.getRandomPassword(passwordLength);
        um.updateSecret(people.getUid(), random);
        String message = UserAccountsEmailMessages.buildTempResetMessage(random);
        email.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.SUCCESS.name(), "Reset QR code.", people);

        return "validate_code";
      } else {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.FAILED.name(), "", people);

        return "";
      }
    } catch (MessagingException ex) {
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

    }

    return "";
  }

  /**
   * Validate the temp code sent to user to reset the account.
   * <p>
   * @return
   */
  public String validateTmpCode() {
    qrCode = null;
    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.BLOCKED_ACCOUNT.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.DEACTIVATED_ACCOUNT.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return "";
    }

    if (people.getSecret() == null ? tmpCode == null : people.getSecret().
            equals(this.tmpCode)) {

      try {
        String otpSecret = SecurityUtils.calculateSecretKey();

        um.updateSecret(people.getUid(), otpSecret);
        qrCode = QRCodeGenerator.getQRCode(people.getEmail(),
                AuthenticationConstants.ISSUER, otpSecret);
        qrEnabled = 1;
         am.registerAccountChange(people, AccountsAuditActions.QRCODE.name(),
              AccountsAuditActions.SUCCESS.name(), "QR code reset.", people);

        return "qrcode";

      } catch (IOException | WriterException | NoSuchAlgorithmException ex) {
        Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE,
                null, ex);
      }

    } else {
      int val = people.getFalseLogin();
      um.increaseLockNum(people.getUid(), val + 1);
      if (val > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
        um.changeAccountStatus(people.getUid(), "",
                PeopleAccountStatus.BLOCKED_ACCOUNT.getValue());
        try {
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.SUCCESS.name(), "Account bloecked due to many false attempts.", people);

          email.sendEmail(people.getEmail(),
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {
        
        }
      }

      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.INCCORCT_TMP_PIN);

      return "";
    }
    return "";
  }

  /**
   * Register lost Yubikey device.
   * <p>
   * @return
   * @throws java.net.SocketException
   */
  public String sendYubiReq() throws SocketException {

    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);

      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.BLOCKED_ACCOUNT.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
      return "";
    }

    if (people.getMode() != PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);

      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

      return "";
    }

    try {
      if (people.getPassword().equals(DigestUtils.sha256Hex(passwd))) {

        String message = UserAccountsEmailMessages.buildYubikeyResetMessage();
        people.
                setStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.
                        getValue());
        people.getYubikey().setStatus(PeopleAccountStatus.LOST_YUBIKEY.
                getValue());
        um.updatePeople(people);
        email.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.DEVICE_LOST_SUBJECT, message);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.SUCCESS.name(), "", people);
        return "yubico_reset";
      } else {

        int val = people.getFalseLogin();
        um.increaseLockNum(people.getUid(), val + 1);
        if (val > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
          um.changeAccountStatus(people.getUid(), "",
                  PeopleAccountStatus.BLOCKED_ACCOUNT.getValue());
           am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.SUCCESS.name(), "Account bloecked due to many false attempts.", people);

          try {
            email.sendEmail(people.getEmail(),
                    UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                    UserAccountsEmailMessages.accountBlockedMessage());
          } catch (MessagingException ex1) {
            
            am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                    AccountsAuditActions.FAILED.name(), "", people);

          }
        }

        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.FAILED.name(), "", people);

        return "";
      }
    } catch (MessagingException ex) {
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
    }
    return "";
  }

  public String returnMenu() {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = null;
    if (ctx != null) {
      sess = (HttpSession) ctx.getExternalContext().getSession(false);
    }

    if (sess != null) {
      sess.invalidate();
    }
    qrCode = null;
    return ("welcome");

  }
}
