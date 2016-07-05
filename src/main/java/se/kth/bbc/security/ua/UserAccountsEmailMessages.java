 
package se.kth.bbc.security.ua;

 
public class UserAccountsEmailMessages {

  /*
   * Subject of account request
   */
  public final static String ACCOUNT_REQUEST_SUBJECT = "Your HopsWorks account needs verification";

  /*
   * Subject of account activation email
   */
  public final static String ACCOUNT_CONFIRMATION_SUBJECT
          = "Welcome to HopsWorks!";

  /*
   * Subject of device lost
   */
  public final static String DEVICE_LOST_SUBJECT = "Login issue";

  /*
   * Subject of blocked acouunt
   */
  public final static String ACCOUNT_BLOCKED__SUBJECT = "Your account is locked";

  /*
   * Subject of blocked acouunt
   */
  public final static String BIOBANKCLOUD_SUPPORT_EMAIL
          = "support@hops.io";

  /*
   * Subject of profile update
   */
  public final static String ACCOUNT_PROFILE_UPDATE = "Your profile has been updated";

  /*
   * Subject of password rest
   */
  public final static String ACCOUNT_PASSWORD_RESET = "Your password has been reset";

  /*
   * Subject of rejected accounts
   */
  public final static String ACCOUNT_REJECT = "Your HopsWorks account request has been rejected";

  /*
   * Default accpount acitvation period
   */
  public final static int ACCOUNT_ACITVATION_PERIOD = 48;

  public final static String GREETINGS_HEADER = "Hello";

  /*
   * Account deactivation
   */
  public final static String ACCOUNT_DEACTIVATED = "Your HopsWorks account has expired";

  /**
   * Build an email message for Yubikey users upon registration.
   *
   * @param username
   * @param key
   * @return
   */
  public static String buildYubikeyRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We receieved your Yubikey account request for HopsWorks.\n\n";
    String l2
            = "Please click on the following link to verify your email address. Afterwards we will activate your account within 48 hours and send you a Yubikey stick to your address.\n\n\n";

    String url = path + "/security/validate_account.xhtml?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";

    String l4 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }

  /**
   * Build an email message for mobile users upon registration.
   *
   * @param username
   * @param key
   * @return
   */
  public static String buildMobileRequestMessage(String path, String key) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received a smartphone account request for HopsWorks on your behalf.\n\n";
    String l2 = "Please click on the following link to verify your email address. We will activate your account within "
            + ACCOUNT_ACITVATION_PERIOD
            + " hours after validating your email address.\n\n\n";

    String url = path + "/security/validate_account.xhtml?key=" + key;

    String l3 = "To confirm your email click " + url + " \n\n";
    String l4 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3 + l4;

    return message;
  }

  public static String accountBlockedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your HopsWorks account has been blocked.\n\n";
    String l2
            = "If you have any questions please visit www.hops.io or contact support@hops.io";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3;
    return message;
  }

  public static String buildPasswordResetMessage(String random_password) {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A password reset has been requested on your behalf.\n\nPlease use the temporary password"
            + " below. You will be required to change your passsword when you login first time.\n\n";

    String tmp_pass = "Password:" + random_password + "\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l3;
    return message;
  }

  public static String buildSecResetMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A security question change has been requested on your behalf.\n\n";
    String l2 = "Your security question has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3;
    return message;
  }

  public static String buildDeactMessage() {

    String message;
    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We receieved an account deactivation request and your HopsWorks account has been deactivated.\n\n";
    String l2 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2;
    return message;
  }

  /**
   * Construct message for profile password change
   *
   * @return
   */
  public static String buildResetMessage() {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A password reset has been requested on your behalf.\n\n";
    String l2 = "Your password has been changed successfully.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  public static String accountActivatedMessage(String username) {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your account request to access HopsWorks has been approved.\n\n";
    String l2 = "You can login with your username: " + username + " and other credentials you setup.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }

  
  public static String yubikeyAccountActivatedMessage(String username) {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your account request to access HopsWorks has been approved.\n\n";
   
    String l2 = "We sent a Yubikey device to your postal address. You can use that device in addition to usename/password to login to the platform. \n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2 + l3;

    return message;
  }
  
  public static String accountRejectedMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "Your HopsWorks account request has been rejected.\n\n";
    String l2 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;
    message = l1 + l2;

    return message;
  }

  public static String buildTempResetMessage(String random_password) {

    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "A mobile device reset has been requested on your behalf.\n\n"
            + "Please use the temporary password below."
            + "You need to validate the code to get a new setup.\n\n";

    String tmp_pass = "Code:" + random_password + "\n\n\n";
    String l2 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + tmp_pass + l2;

    return message;

  }

  public static String buildYubikeyResetMessage() {
    String message;

    String l1 = GREETINGS_HEADER + ",\n\n"
            + "We received an Yubikey request reset for HopsWorks in your behalf.\n\n";
    String l2 = "Your account will be reset within "
            + ACCOUNT_ACITVATION_PERIOD + " hours and a new device will be sent to your postal address.\n\n\n";
    String l3 = "If you have any questions please contact "
            + BIOBANKCLOUD_SUPPORT_EMAIL;

    message = l1 + l2 + l3;

    return message;
  }

}
