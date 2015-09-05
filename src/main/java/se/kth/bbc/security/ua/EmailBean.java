package se.kth.bbc.security.ua;

import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class EmailBean {

  @Resource(lookup = "mail/BBCMail")
  private Session mailSession;

  @Asynchronous
  public void sendEmail(String to, String subject, String body) throws
          MessagingException {

    MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
    InternetAddress[] address = {new InternetAddress(to)};
    message.setRecipients(Message.RecipientType.TO, address);
    message.setSubject(subject);
    message.setContent(body, "text/html");

    // set the timestamp
    message.setSentDate(new Date());

    message.setText(body);
    Transport.send(message);
  }
}
