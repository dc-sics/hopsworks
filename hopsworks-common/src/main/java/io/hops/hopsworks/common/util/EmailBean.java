package io.hops.hopsworks.common.util;

import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Stateless
public class EmailBean {

  @Resource(lookup = "mail/BBCMail")
  private Session mailSession;

  @Asynchronous
  public void sendEmail(String to, Message.RecipientType recipientType,
          String subject, String body) throws
          MessagingException, SendFailedException {

    MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
    InternetAddress[] address = {new InternetAddress(to)};
    message.setRecipients(recipientType, address);
    message.setSubject(subject);
    message.setContent(body, "text/html");

    // set the timestamp
    message.setSentDate(new Date());

    message.setText(body);
    Transport.send(message);
  }
}
