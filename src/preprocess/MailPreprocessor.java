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

package preprocess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import preprocess.fetch.MailFetcher;
import preprocess.fetch.SubmissionFetchingException;
import preprocess.tokenize.GeneralTokenizer;
import preprocess.tokenize.InvalidStructureException;
import preprocess.tokenize.MaximumDirectoryDepthExceededException;
import preprocess.tokenize.Submission;
import preprocess.tokenize.SubmissionStructure;
import util.hashing.SHA1Generator;

/**
 * @author <a href="mailto:fabian.maquart@uni-konstanz.de">Fabian Marquart</a>
 */
public class MailPreprocessor {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private static final String EMAILREGEX =
            "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";

    // As the submissionStructure is always the same when using the
    // mailFetcher,
    // we can use this information.

    /**
     * This method is used to process a targetDirectory's files to a list of
     * submission objects. Does not use Tokenizer.
     *
     *
     * @return : the list of submissions.
     * @throws SubmissionFetchingException
     *             : is thrown when the list is empty.
     */
    public static PreprocessingResult preprocess(
            Connection connection, Path targetDirectory, String fileRegex,
            String archiveRegex, Date startTime, Date deadline,
            String courseName, String exerciseName)
            throws SubmissionFetchingException {

        String loginUsername = connection.getUsername();
        String loginPassword = connection.getPassword();
        String mailServer = connection.getLocation();

        List<Submission> submissions = null;

        List<String> structureList = new LinkedList<>();
        structureList.add("TOPLEVEL");
        structureList.add(".*@.*");
        structureList.add(exerciseName.toString());
        structureList.add("SUBMISSION");

        Path submissionDirectory =
                MailFetcher.fetchSubmissions(targetDirectory, mailServer,
                        loginUsername, loginPassword, startTime, deadline,
                        courseName, exerciseName);

        GeneralTokenizer tokenizer =
                new GeneralTokenizer(fileRegex, archiveRegex);

        try {
            SubmissionStructure structure =
                    new SubmissionStructure(structureList);
            submissions =
                    tokenizer.exploreSubmissionDirectory(structure,
                            submissionDirectory);
        } catch (MaximumDirectoryDepthExceededException e) {
            LOGGER.severe("Maximum directory depth exceeded.");
            throw new SubmissionFetchingException(e);
        } catch (InvalidStructureException e) {
            throw new SubmissionFetchingException(e);
        }

        // Now, iterate through the submissions list and extract the students
        // email address from the path and save it to the student object in the
        // submission.

        Map<Student, Submission> map = new HashMap<>();

        for (Submission currentSubmission : submissions) {
            // set the student email by reading it from the path
            String studentEmail =
                    currentSubmission.getSourceCodeLocation().toString();
            String studentName = "";

            // use a regex to get student email address
            Pattern pattern = Pattern.compile(EMAILREGEX);

            // use regex to get student name
            Matcher matcher = pattern.matcher(studentEmail);
            if (matcher.find()) {
                studentEmail = matcher.group();
                studentName =
                        studentEmail.substring(0, studentEmail.indexOf("@"));
            }

            currentSubmission.getStudent().setEmail(studentEmail);
            currentSubmission.getStudent().setName(studentName);
            try {
                currentSubmission.setSHA1Hash(SHA1Generator
                        .calculateSHA1Hash(currentSubmission
                                .getSourceCodeLocation()));
            } catch (IOException e) {
                LOGGER.severe("IOException while generating hash, "
                        + "skipping submission from : "
                        + currentSubmission.getStudent().getEmail() + "\n"
                        + e.getMessage());
                continue;
            }

            map.put(currentSubmission.getStudent(), currentSubmission);
        }

        if (submissions.isEmpty()) {
            LOGGER.severe("No submissions were found. ");
            throw new SubmissionFetchingException("Submissions list is empty.");
        }

        LOGGER.info("Processed all submissions");

        return new PreprocessingResult(map, new ArrayList<Student>());
    }
}
