/*
 * Copyright (C) 2015 VARCID
 * Copyright (C) 2014 Team GRIT
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.teamgrit.grit.entities;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * This class provides a convenient wrapper for sending an email, optionally 
 * including one attachment. All smtp-related settings
 * (such as mail-port, smtp-host and alike) are directly
 * extracted off the config available via {@link Controller#getConfig()}.
 * <br></br>
 * Simply call {@link #sendEMail}
 *
 * @author <a href="mailto:cedric.sehrer@uni-konstanz.de">Cedric Sehrer</a>
 */
public class SendMail {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private String userName = Controller.getController().getConfig().getSenderMailAdress();
    private String password = Controller.getController().getConfig().getMailPassword();
    private String sendingHost = Controller.getController().getConfig().getSmtpHost();
    private String mailPort = Controller.getController().getConfig().getMailPort();
    private String from;
    private String to;
    private String subject;

    /**
     * Provides a convenient way to send a mail. 
     * Optionally, with an attachment. Otherwise, leave null.
     * 
     * @param from 
     *      the from-address
     * @param to
     *      the receiver
     * @param subject
     *      the mail-subject
     * @param text
     *      the content
     * @param attachment
     *      (optionally) one attachment. Can be null.
     */
    public void sendEMail(String from, String to, String subject, 
            String text, File attachment) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        
        LOGGER.finer(
            "Preparing mail for "+to+" regarding \""
                + subject+"\".");

        Properties props = new Properties();

          props.put("mail.smtp.host", this.sendingHost);
          props.put("mail.smtp.user", this.userName);
          props.put("mail.smtp.password", this.password);
          props.put("mail.smtp.starttls.enable", "true");
          props.put("mail.smtp.auth", "true");
          props.put("mail.smtp.timeout", "5000");
          props.put("mail.smtp.connectiontimeout", "5000");

        Session session = Session.getInstance(props);

        Message message = new MimeMessage(session);

        try {

            if (this.from.equals("")) {
              return;
            }

            InternetAddress fromAddress = new InternetAddress(this.from);
            InternetAddress toAddress = new InternetAddress(this.to);

            message.setFrom(fromAddress);
            message.setRecipient(Message.RecipientType.TO, toAddress);
            //to add CC or BCC use:
            //message.setRecipient(RecipientType.CC, new InternetAddress("CC_Recipient@someMail.com"));
            //message.setRecipient(RecipientType.BCC, new InternetAddress("BCC_Recipient@someMail.com"));
            message.setSubject(this.subject);

            // Attachment-handling
            if (attachment != null) {
              LOGGER.finest(
                  "Attachment found: "+attachment.getName()
                  +". Adding it.");
              MimeBodyPart messageBody = new MimeBodyPart();
              messageBody.setText(text);

              MimeBodyPart attachmentBody = new MimeBodyPart();
              attachmentBody.attachFile(attachment);
              Multipart mp = new MimeMultipart();
              mp.addBodyPart(messageBody);
              mp.addBodyPart(attachmentBody);
              message.setContent(mp);
            } else {
              message.setText(text);
              LOGGER.finest("No attachment found.");
            }

            // Transportation-handling
            Transport transport = null;
            if (mailPort.equals("25") || mailPort.equals("587")) {
              transport = session.getTransport("smtp");
              LOGGER.finest("Transport recognized as smtp");
            }
            if (mailPort.equals("465")) {
              transport = session.getTransport("smtps");
              LOGGER.finest("Transport recognized as smtps");
            }
            LOGGER.finer("Establishing connection to "+sendingHost
                + " on port "+Integer.valueOf(mailPort)
                + "with username "+userName+" and a password");
            transport.connect(this.sendingHost, Integer.valueOf(mailPort), this.userName, this.password);
            LOGGER.finest("Connection successfull. Sending email.");
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            LOGGER.fine("Email sent successfully!");
        } catch (AddressException e) {
            LOGGER.severe("AddressException during SendMail to "+to+". Reason:"+ e.toString());
        } catch (MessagingException e) {
            LOGGER.severe("MessagingException during SendMail to "+to+". Reason:"+ e.toString());
        } catch (NullPointerException e) {
            LOGGER.severe("NullPointerException during SendMail to "+to+". Reason:"+ e.toString());
        } catch (IOException e) {
            LOGGER.severe("IOException during SendMail to "+to+". Reason:"+ e.toString());
        }
    }
}