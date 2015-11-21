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

package de.teamgrit.grit.preprocess.fetch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import de.teamgrit.grit.entities.Exercise;
import de.teamgrit.grit.util.mailer.EncryptorDecryptor;

/**
 * This class implements a mail fetcher to fetch submissions from a mail
 * account.
 * 
 * @author <a href="mailto:fabian.marquart@uni-konstanz.de">Fabian Marquart</a>
 */

public class MailFetcher {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private static final String EMAILREGEX = "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";

    /**
     * This method should fetch submissions and return a Path to the downloaded
     * attachments. It will also mark all messages whose attachments have been
     * downloaded as "seen". The paths to the downloaded files (as well as the
     * files themselves) are guaranteed to not contain any whitespace.
     * <p/>
     * Remember to set targetDirectory, mailServer, loginUsername,
     * loginPassword (encrypt using EncryptorDecryptor), issuanceDate, dueDate
     * (you can parse them using formatter class), courseName and
     * assignmentNumber correctly.
     * 
     * @param targetDirectory
     *            the directory the submissions will be downloaded into
     * @param mailServer
     *            the server the mail account belongs to
     * @param protocol
     *            the mail protocol used (eg. imap(s), pop3(s))
     * @param loginUsername
     *            the username of the mail account
     * @param loginPassword
     *            the password of the mail account
     * @param issuanceDate
     *            the start time of the exercise
     * @param dueDate
     *            the deadline of the exercise
     * @param courseName
     *            the name of the course the exercise belongs to
     * @param exerciseName
     *            the name of exercise
     * 
     * @return: the Path.
     */
    public static Path fetchSubmissions(Path targetDirectory,
            String mailServer, String protocol, String loginUsername,
            String loginPassword, Date issuanceDate, Date dueDate, 
            String courseName, String exerciseName, final String allowedDomain)
                throws SubmissionFetchingException {
        SearchTerm term = null;
        Path submissionDirectory = null;
        Path returnDirectory = null;

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail.imap.timeout", "5000");
        props.setProperty("mail.imap.starttls.enable", "true");
        props.setProperty("mail.imap.connectiontimeout", "5000");
        props.setProperty("mail.imaps.timeout", "5000");
        props.setProperty("mail.imaps.connectiontimeout", "5000");
        props.setProperty("mail.pop3.timeout", "5000");
        props.setProperty("mail.pop3.starttls.enable", "true");
        props.setProperty("mail.pop3.connectiontimeout", "5000");

        try {
          LOGGER.fine("Preparing and establishing connection.");
            // specified mailbox is searched
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            try {
                EncryptorDecryptor ed = new EncryptorDecryptor();
                store.connect(mailServer, loginUsername,
                        ed.decrypt(loginPassword));
            } catch (InvalidKeyException e) {
                LOGGER.severe("Invalid key.");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                LOGGER.severe("No such algorithm.");
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                LOGGER.severe("No such padding.");
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                LOGGER.severe("Illegal block size.");
                e.printStackTrace();
            } catch (BadPaddingException e) {
                LOGGER.severe("Bad padding.");
                e.printStackTrace();
            }
            LOGGER.finer("Connection successfull! Opening"
                + "folder, now");
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            LOGGER.finest("Opened successfully");

            // search terms
            LOGGER.fine("Defining search terms");
            ReceivedDateTerm minDateTerm = new ReceivedDateTerm(
                    ComparisonTerm.GE, issuanceDate);
            ReceivedDateTerm maxDateTerm = new ReceivedDateTerm(
                    ComparisonTerm.LE, dueDate);
            SubjectTerm subjectTerm = new SubjectTerm("[" + courseName + "-"
                    + exerciseName + "]");
            Flags seen = new Flags(Flags.Flag.SEEN);
            SearchTerm senderFromAllowedDomain = new SearchTerm() {

              /**
               * The generated serialVersionUID;
               */
              private static final long serialVersionUID = -304248369679388733L;

              @Override
              public boolean match(Message msg) {
                try {
                  /* 
                   * The mailaddress, by convention, will look like
                   * Name <Address@doma.in>. This convention forbids 
                   * more than one "@" in the from. Thus we can split
                   * by @ and for convenience (regarding comparison)
                   * add a ">" to the allowed domain.
                   */
                  if (InternetAddress.toString(
                      msg.getFrom()).split("@")[1].equals(allowedDomain+">"))
                    return true;
                } catch (MessagingException e) {
                  e.printStackTrace();
                }
                return false;
              }
            };
            FlagTerm unseenFlagTerm = new FlagTerm(seen, false);

            term = new AndTerm(minDateTerm, maxDateTerm);
            term = new AndTerm(term, subjectTerm);
            term = new AndTerm(term, senderFromAllowedDomain);
            term = new AndTerm(term, unseenFlagTerm);
            
            SearchTerm oppositeSubjectTerm = new AndTerm (new AndTerm(
                new AndTerm (minDateTerm, maxDateTerm), 
                new NotTerm(subjectTerm)), unseenFlagTerm);

            /*
             * Search by date and time. Differentiates between imap and
             * pop3. Unless specified by a server-side implementation
             * of the search algorithm, imap is not capable of comparing
             * timestamps by invoking Folder.search(). Its granularity is
             * daywise.
             * This can be bypassed by checking the sentDate of each 
             * message and comparing it to the desired time. The 
             * Folder.search() invocation for pop3 works as intended, 
             * because all files are located locally.
             * 
             * For IMAP, we take advantage of that by outsourcing part 
             * of the search to the server. The server returns 
             * all messages containing the above-mentioned subject and 
             * being sent between (including) minDateTerm and maxDateTerm.
             * These remaining messages will be handled "traditionally". 
             */
            LOGGER.fine("Searching messages");

            Message[] foundMessages = inbox.search(term);
            Message[] messagesWithWrongSubject = 
                inbox.search(oppositeSubjectTerm);

            if (props.getProperty("mail.store.protocol").equals("imap") |
                props.getProperty("mail.store.protocol").equals("imaps")) {
              List<Message> tempMessages = new ArrayList<Message>();
              for (Message message : foundMessages) {
                if (message.getSentDate().compareTo(issuanceDate) >= 0 &&
                    message.getSentDate().compareTo(dueDate) <= 0) {
                  tempMessages.add(message);
                }
              }
              LOGGER.finer("Mails with proper subject fetched. Starting"
                  + "to fetch mails with improper subject.");
              foundMessages = 
                  tempMessages.toArray(new Message[tempMessages.size()]);

              /* In order to notify students about a wrong formated
               * (or invalid) subject in their mail, we need to 
               * perform a search again.
               */
              tempMessages = new ArrayList<Message>();
              for (Message message : messagesWithWrongSubject) {
                if (message.getSentDate().compareTo(issuanceDate) >= 0 &&
                    message.getSentDate().compareTo(dueDate) <= 0) {
                  tempMessages.add(message);
                }
              }
              LOGGER.finer("Mails with improper subject fetched.");
            }

            List<StudentRepoData> studentData = new ArrayList<>();
            returnDirectory = targetDirectory.resolve(Paths
                .get(courseName.replaceAll("\\s", "_")));

            LOGGER.fine("Processing messages with proper subject.");
            for (Message message : foundMessages) {
              LOGGER.finer(
                  "Processing message by "
                  + InternetAddress.toString(message.getFrom()));
              // this gets something like: Paul Power <paulpower@uni.kn>
              String from = InternetAddress.toString(message.getFrom());
              String senderAddress = "noAddress";

              // this discards everything besides the email address, e.g.
              // paulpower@uni.kn
              Matcher m = Pattern.compile(EMAILREGEX).matcher(from);
              if (m.find()) {
                  senderAddress = m.group();
              }

              String contentType = message.getContentType();

              if (contentType.contains("multipart")) {
                  // this message may contain an attachment
                  Multipart multiPart = (Multipart) message.getContent();

                  submissionDirectory = targetDirectory.resolve(Paths
                      .get(courseName.replaceAll("\\s", "_") + "/"
                          + senderAddress));
                  String destFilePath = (submissionDirectory
                    .resolve(exerciseName.replaceAll("\\s", "_"))).toString()
                      + "/";

                  // In case the directories do not exist already,
                  // create them
                  if (!Files.exists(submissionDirectory)) {
                    Files.createDirectories(submissionDirectory);
                  }
                  if (!Files.exists(submissionDirectory
                        .resolve(exerciseName.replaceAll("\\s", "_")))) {
                    Files.createDirectories(submissionDirectory
                            .resolve(exerciseName.replaceAll("\\s", "_")));
                  }
                  LOGGER.finer("Processing attchments");
                  List<StudentRepoData> tempData =
                      writeAttachmentsToPath(
                      destFilePath, multiPart, message.getSentDate(),
                      from, message.getSubject());
                  
                  LOGGER.finest("Marking message as seen");
                  message.setFlag(Flags.Flag.SEEN, true);
                  studentData.addAll(tempData);
                }
            }
            /*
             * Separately, process all mails without a proper subject.
             * These people can then be contacted later.
             */
            LOGGER.fine("Processing messages with improper subject");
            List<String> invalidMailSubjects = new ArrayList<String>();
            for (Message message : messagesWithWrongSubject) {
              Address[] from = message.getFrom();
              for (Address address : from) {
                invalidMailSubjects.add(address.toString());
              }
              // finally, mark message as seen so it does not appear
              // in future searches.
              message.setFlag(Flags.Flag.SEEN, true);
            }
            // disconnect eventually
            LOGGER.fine(
                "All processing done. Closing connection to mailstore");
            inbox.close(false);
            store.close();

            Exercise.notifyStudentsWithInvalidMailSubject(invalidMailSubjects);

        } catch (NoSuchProviderException e) {
            LOGGER.severe("No provider.");
        } catch (MessagingException e) {
            LOGGER.severe(
                  "Could not connect to the message store."
                + "Reason: "+e.toString());
        } catch (IOException e) {
            LOGGER.severe(
                  "Could not get content of the message."
                + "Reason: "+e.toString());
        }
        return returnDirectory;
    }

    /**
     * This method provides a convenient (and recursive) way of
     * dealing with email-attachments. It searches through all
     * parts of the mail and collects any attachment found and
     * and writes their content to the given path.
     * 
     * @param path
     *        the output-path of the files
     * @param mp 
     *        the mail-content
     * @param date
     *        the timestamp, relevant for {@link StudentRepoData}
     * @param sender
     *        The submitter
     * @param subject
     *        the mail-subject
     * @return
     *        A list of {@link StudentRepoData}
     */
    private static List<StudentRepoData> writeAttachmentsToPath(
        String path, Multipart mp, Date date,
        String sender, String subject) {
      List<StudentRepoData> studentData = new ArrayList<StudentRepoData>();
      try {
        for (int i = 0; i < mp.getCount(); i++) {
          MimeBodyPart part = (MimeBodyPart) mp.getBodyPart(i);

          if (Part.ATTACHMENT.equalsIgnoreCase(
              part.getDisposition())) {
            String submissionFilename = 
                part.getFileName().replaceAll("\\s", "_");
            FileOutputStream output = new FileOutputStream(
                path+"/"+submissionFilename);
            InputStream input = part.getInputStream();

            byte[] buffer = new byte[4096];

            int byteRead;

            while ((byteRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, byteRead);
            }

            output.close();

            // get the student repodata
            StudentRepoData s = new StudentRepoData(null, null,
                    sender,  new Timestamp(date.getTime()),
                    submissionFilename, submissionFilename);
            studentData.add(s);
          
          }
          if (part.getContentType().contains("multipart")) { 
            studentData.addAll(
                writeAttachmentsToPath(
                    path, (Multipart) part.getContent(),
                    date, sender, subject));
          }
        }
      } catch (MessagingException | IOException e) {
        LOGGER.severe(
            "An error occured while writing the attachments into "
            +path+" Error: "+e.toString());
      }
      return studentData;
    }  
}
        