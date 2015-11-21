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

package de.teamgrit.grit.preprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import de.teamgrit.grit.preprocess.fetch.SubmissionFetchingException;
import de.teamgrit.grit.preprocess.fetch.SvnFetcher;
import de.teamgrit.grit.preprocess.tokenize.GeneralTokenizer;
import de.teamgrit.grit.preprocess.tokenize.MaximumDirectoryDepthExceededException;
import de.teamgrit.grit.preprocess.tokenize.Submission;
import de.teamgrit.grit.preprocess.tokenize.Tokenizer;
import de.teamgrit.grit.util.hashing.SHA1Generator;

/**
 * This SVN Preprocessor uses the {@link SvnFetcher} to collect all submissions
 * from a SVN repository. It also replaces the name of the student objects by
 * their real name. This is done by a 'students.txt' file in the toplevel of
 * the SVN repo, which contains lines like the following:
 *
 * studXX = max.2.mustermann@<domain>.<topleveldomain>
 *
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */
public final class SvnPreprocessor {
    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * This is a singleton no need for instances.
     */

    private SvnPreprocessor() {
    }

    /**
     *
     * @param connection
     *            the connection describing the svn server
     * @param targetDirectory
     *            the target directory to download the submissions into
     * @param fileRegex
     *            the regex matching the valid source files
     * @param archiveRegex
     *            the regex matching the valid archive types
     * @return the result of the preprocessing
     * @throws SubmissionFetchingException
     */
    public static PreprocessingResult preprocess(Connection connection,
            Path targetDirectory, String fileRegex, String archiveRegex)
            throws SubmissionFetchingException {
    	    	    	    	
       Tokenizer submissionTokenizer =
                new GeneralTokenizer(fileRegex, archiveRegex);
        List<Student> studentsWithoutSubmission = new ArrayList<>();
        Map<Student, Submission> submissions = new HashMap<>();

        LOGGER.info("Fetching submissions from SVN");
        // throws SubmissionFetchingException
        Path pathToSubmissions =
                SvnFetcher.fetchSubmissions(connection, targetDirectory);
       
        
        if (pathToSubmissions != null) {
            // since we're using SVN we don't receive a list of mail
            // adresses, so we have to parse the corresponding file
            // which has to be provided in the toplevel directory of
            // the repository
            LOGGER.info("Collecting submissions");
            try {
                // getting a list of all submissions with the corresponding
                // student
                // throws MaximumDirectoryDepthExceededException
                List<Submission> tokenizedSubmissions =
                        submissionTokenizer.exploreSubmissionDirectory(
                                connection.getStructure(), pathToSubmissions);
                
                File studentsMapping =
                        new File(pathToSubmissions.toString(), "students.txt");
                                
                Scanner fileReader = new Scanner(studentsMapping);

                List<String> tempStudents = new ArrayList<>();

                // write every line from file not starting with #, being empty
                // or
                // consisting only out of whitespaces in tempStudents
                while (fileReader.hasNext()) {
                    String line = fileReader.nextLine();
                    if (!(line.startsWith("#") || line.isEmpty() || line
                            .matches("\\s*"))) {
                        tempStudents.add(line);
                    }
                }

                fileReader.close();
                                
                String[] students = tempStudents.toArray(new String[0]);

                List<Path> emptySubmissionPaths =
                        submissionTokenizer.getEmptySubmissions();
                LOGGER.finer("Mapping student with submission");
                // map each student in the mapping file to his submission
                for (String name : students) {
                    String[] mapping = name.split("=");
                    if (mapping.length < 2) {
                        LOGGER.warning("Mapping file contains invalid line: "
                                + name + "\n Students might be named wrong.");
                    }
                    String tempName = mapping[mapping.length - 1];

                    // remove all whitespaces in front of the name and the
                    // acronym
                    tempName = tempName.trim();
                    mapping[0] = mapping[0].trim();
                    
                    // find the submission that belongs to the student by
                    // checking if his acronym is contained in the path
                    for (Submission submission : tokenizedSubmissions) {
                        Student stud = submission.getStudent();
                        String submissionPath =
                                submission.getSourceCodeLocation().toString();
                        submissionPath = submissionPath.substring(submissionPath.lastIndexOf("/")+1);
                        if (submissionPath.equals(mapping[0])) {
                            stud.setName(tempName.substring(0,
                                    tempName.indexOf("@")));
                            stud.setEmail(tempName);
                            try {
                                submission.setSHA1Hash(SHA1Generator
                                        .calculateSHA1Hash(submission
                                                .getSourceCodeLocation()));
                            } catch (IOException e) {
                                LOGGER.severe("IOException while generating hash, "
                                        + "skipping submission from : "
                                        + tempName + "\n" + e.getMessage());
                            }
                            submissions.put(submission.getStudent(),
                                    submission);
                        }
                    }
                    for (Path location : emptySubmissionPaths) {
                        if (location.toString().contains(mapping[0])) {
                            Student stud =
                                    new Student(tempName.substring(0,
                                            tempName.indexOf("@")));
                            stud.setEmail(tempName);
                            studentsWithoutSubmission.add(stud);
                        }
                    }
                }

                LOGGER.info("Preprocessing completed");

                return new PreprocessingResult(submissions,
                        studentsWithoutSubmission);
            } catch (MaximumDirectoryDepthExceededException e) {
                throw new SubmissionFetchingException(
                        "Maximum Depth exceeded in Tokenizer.", e);
            } catch (FileNotFoundException e) {
                throw new SubmissionFetchingException(
                        "No mapping file to read from", e);
            }

        }

        // in this case pathToSubmissions is null so there are no submissions
        return null;
    }
}