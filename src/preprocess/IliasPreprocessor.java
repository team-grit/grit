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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

import preprocess.fetch.IliasFetcher;
import preprocess.fetch.StudentRepoData;
import preprocess.fetch.SubmissionFetchingException;
import preprocess.tokenize.GeneralTokenizer;
import preprocess.tokenize.InvalidStructureException;
import preprocess.tokenize.MaximumDirectoryDepthExceededException;
import preprocess.tokenize.Submission;
import preprocess.tokenize.SubmissionStructure;
import util.hashing.SHA1Generator;

/**
 * The ILIAS preprocessor uses the {@link IliasFetcher} to fetch submissions
 * from an ILIAS installation.
 * 
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */

public final class IliasPreprocessor {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * Class not Constructable.
     * 
     */
    private IliasPreprocessor() {
    }

    /**
     * Downloads the submissions from the ilias specified in the connection
     * into the target directory.
     * 
     * @param connection
     *            the connection specifying the remote ilias
     * @param targetDirectory
     *            the directory the submissions are stored in
     * @param courseName
     *            the name of the course.
     * @param exerciseName
     *            the name of the exercise
     * @param fileRegex
     *            the regex expression defining the
     * @param archiveRegex
     *            the regex expression definig the acepted archive files
     * 
     * @return A {@link PreprocessingResult} containing the fetched submissions
     *         and the students who did not submit.
     * @throws SubmissionFetchingException
     *             if the fetching goes wrong
     */
    public static PreprocessingResult preprocess(Connection connection,
            Path targetDirectory, String courseName, String exerciseName,
            String fileRegex, String archiveRegex)
            throws SubmissionFetchingException {

        // initialize internal members

        List<String> structureList = new ArrayList<>(4);
        structureList.add("TOPLEVEL");
        structureList.add(".*@.*");
        structureList.add(exerciseName);
        structureList.add("SUBMISSION");

        SubmissionStructure submissionStructure;
        try {
            submissionStructure = new SubmissionStructure(structureList);
        } catch (InvalidStructureException e) {
            LOGGER.severe("Error while creating submission structure"
                    + e.getMessage());
            throw new SubmissionFetchingException(e.getMessage());
        }

        GeneralTokenizer submissionTokenizer = new GeneralTokenizer(fileRegex,
                archiveRegex);

        Map<Student, Submission> studentSubmissions = new HashMap<>();
        List<Student> studentsWithoutSubmissions = new ArrayList<>();

        // Fetch submissions
        LOGGER.info("Fetching submissions from ILIAS");
        List<StudentRepoData> iliasStudentData = IliasFetcher.fetchSubmissions(
                connection, courseName, exerciseName, targetDirectory);

        try {
            LOGGER.info("Collecting the submissions");
            List<Submission> fetchedSubmissions = submissionTokenizer
                    .exploreSubmissionDirectory(submissionStructure,
                            targetDirectory);

            // detect if the students turned in submissions
            for (StudentRepoData studentData : iliasStudentData) {
                for (Submission submission : fetchedSubmissions) {
                    if ((submission.getSourceCodeLocation() != null)
                            && (studentData.getSubmissionPath() != null)) {

                        setSubmitted(studentData, submission,
                                studentSubmissions);
                        try {
                            submission.setSHA1Hash(SHA1Generator
                                    .calculateSHA1Hash(submission
                                            .getSourceCodeLocation()));
                        } catch (IOException e) {
                            LOGGER.severe("IOException while generating hash, "
                                    + "skipping submission from : "
                                    + submission.getStudent().getEmail() + "\n"
                                    + e.getMessage());
                        }
                        // studentSubmissions.put(submission.getStudent(),
                        // submission);

                    } else {
                        addToNotSubmitted(studentData,
                                studentsWithoutSubmissions);
                        // Student stud = new
                        // Student(studentData.getFirstName()
                        // + " " + studentData.getLastName());
                        // stud.setEmail(studentData.getEmail());
                        // studentsWithoutSubmissions.add(stud);
                    }
                }
            }

            LOGGER.info("Processed all submissions");
            return new PreprocessingResult(studentSubmissions,
                    studentsWithoutSubmissions);
        } catch (MaximumDirectoryDepthExceededException e) {
            LOGGER.severe("Tokenizer crashed: " + e.getMessage());
            throw new SubmissionFetchingException(e);
        }

    }

    /**
     * Marks a Student, who did not turn in a submission as such.
     * 
     * @param student
     *            the Student who did not turn in a solution.
     * @param studentsWithoutSubmission
     *            the list containing the students who did not submit
     */
    private static void addToNotSubmitted(StudentRepoData student,
            List<Student> studentsWithoutSubmission) {
        Student stud = new Student(student.getFirstName() + " "
                + student.getLastName());
        stud.setEmail(student.getEmail());
        // avoid duplicates in the list
        boolean iscontained = false;
        for (Student tempStud : studentsWithoutSubmission) {
            if (tempStud.getEmail().equals(stud.getEmail())) {
                iscontained = true;
            }
        }
        if (!iscontained) {
            studentsWithoutSubmission.add(stud);
        }
    }

    /**
     * Sets the Students name and mail according to his submission.
     * 
     * @param studentData
     *            the Student
     * @param studentSubmission
     *            his Submission
     * @param studentSubmissions
     *            the map storing the students and their submissions
     */
    private static void setSubmitted(StudentRepoData studentData,
            Submission studentSubmission,
            Map<Student, Submission> studentSubmissions) {

        String downloadedFile = FilenameUtils.removeExtension(Paths
                .get(studentData.getSubmissionPath()).getFileName().toString());

        String studentFile = studentSubmission.getSourceCodeLocation()
                .getFileName().toString();

        if (studentFile.equals(downloadedFile)) {
            Student student = studentSubmission.getStudent();
            student.setName(studentData.getFirstName() + " "
                    + studentData.getLastName());
            student.setEmail(studentData.getEmail());
            studentSubmissions.put(student, studentSubmission);
        }
    }

}
