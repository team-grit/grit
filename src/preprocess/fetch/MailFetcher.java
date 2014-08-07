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

package preprocess.fetch;

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
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import util.mailer.EncryptorDecryptor;

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
     * attachments.
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
            String mailServer, String loginUsername, String loginPassword,
            Date issuanceDate, Date dueDate, String courseName,
            String exerciseName) throws SubmissionFetchingException {
        SearchTerm term = null;
        Path submissionDirectory = null;
        Path returnDirectory = null;

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");

        try {
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

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // search terms
            ReceivedDateTerm minDateTerm = new ReceivedDateTerm(
                    ComparisonTerm.GE, issuanceDate);
            ReceivedDateTerm maxDateTerm = new ReceivedDateTerm(
                    ComparisonTerm.LE, dueDate);
            SubjectTerm subjectTerm = new SubjectTerm("[" + courseName + "-"
                    + exerciseName + "]");

            term = new AndTerm(minDateTerm, maxDateTerm);
            term = new AndTerm(term, subjectTerm);

            // search on the imap server
            Message[] foundMessages = inbox.search(term);

            List<StudentRepoData> studentData = new ArrayList<>();

            for (Message message : foundMessages) {

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

                    for (int i = 0; i < multiPart.getCount(); i++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart
                                .getBodyPart(i);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part
                                .getDisposition())) {

                            // Create directory named after the sender address
                            // for the attachment in the exercise directory.
                            submissionDirectory = targetDirectory.resolve(Paths
                                    .get(courseName + "/" + senderAddress));
                            returnDirectory = targetDirectory.resolve(Paths
                                    .get(courseName));

                            // Wrong path. Structure is course > email > file
                            if (!Files.exists(submissionDirectory)) {
                                Files.createDirectories(submissionDirectory);
                            }
                            if (!Files.exists(submissionDirectory
                                    .resolve(exerciseName))) {
                                Files.createDirectories(submissionDirectory
                                        .resolve(exerciseName));
                            }

                            // save the attachment to the specified
                            // directory.
                            String submissionFilename = part.getFileName();

                            String destFilePath = (submissionDirectory
                                    .resolve(exerciseName)).toString()
                                    + "/"
                                    + submissionFilename;

                            FileOutputStream output = new FileOutputStream(
                                    destFilePath);

                            InputStream input = part.getInputStream();

                            byte[] buffer = new byte[4096];

                            int byteRead;

                            while ((byteRead = input.read(buffer)) != -1) {
                                output.write(buffer, 0, byteRead);
                            }
                            output.close();

                            // get the student repodata
                            StudentRepoData s = new StudentRepoData(null, null,
                                    senderAddress, new Timestamp(message
                                            .getSentDate().getTime()),
                                    message.getSubject(), submissionFilename);
                            studentData.add(s);
                        }
                    }
                }
            }
            // disconnect
            inbox.close(false);
            store.close();

        } catch (NoSuchProviderException e) {
            LOGGER.severe("No provider.");
            e.printStackTrace();
        } catch (MessagingException e) {
            LOGGER.severe("Could not connect to the message store.");
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.severe("Could not get content of the message.");
            e.printStackTrace();
        }
        return returnDirectory;
    }
}
