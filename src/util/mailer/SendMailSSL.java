/*
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

package util.mailer;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This class provides methods for sending mail. Another method shall provide
 * sending email using a config file.
 * 
 * @author <a href="mailto:fabian.marquart@uni-konstanz.de">Fabian Marquart</a>
 * 
 */
public class SendMailSSL {

    /** Get the logger. */
    private final static Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * This method uses a mail object to send email.
     * 
     * @param mailObject
     * @throws MessagingException
     */
    public static void sendMail(final MailObjectWithConnection mailObject)
            throws MessagingException {
        // configure properties for smtp session
        Properties props = new Properties();
        props.put("mail.smtp.host", mailObject.getSmtpHost());
        props.put("mail.smtp.socketFactory.port", mailObject.getPort());
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", mailObject.getSmtpAuth());
        props.put("mail.smtp.port", mailObject.getSmtpPort());

        // create the session
        Session session = Session.getDefaultInstance(props,
        // authenticate to the mail server by decrypting the password
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        try {
                            EncryptorDecryptor ed = new EncryptorDecryptor();

                            return new PasswordAuthentication(mailObject
                                    .getSenderAddress(), mailObject
                                    .getEncryptedPassword());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // When the password decryption failed.
                        return null;
                    }
                });

        try {
            // create a message and set senderAddress, recipientAddress,
            // subject
            // and message body
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailObject.getSenderAddress()));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(mailObject.getRecipientAddress()));
            message.setSubject(mailObject.getMailSubject());
            message.setText(mailObject.getMailMessage());

            Transport.send(message);

        } catch (MessagingException e) {
            throw new MessagingException(e.getMessage());
        }
    }

}