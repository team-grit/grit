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

package report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.junit.runner.notification.Failure;

import preprocess.tokenize.Submission;
import checking.CheckingResult;
import checking.TestOutput;

/**
 * This class supplies the static method generateCard to generate a plaintext
 * file out of a SubmissionObj.
 * 
 * @author <a href="mailto:thomas.3.schmidt@uni-konstanz.de">Thomas Schmidt</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * 
 */

public final class PlainGenerator {

    /**
     * This method creates a plain-text file from a SubmissionObj instance.
     * 
     * @param submission
     *            A SubmissionObj containing the information that the content
     *            gets generated from.
     * @param outdir
     *            the output directory
     * @param courseName
     *            the name of the Course
     * @param exerciseName
     *            the name of the exercise
     * @return The Path to the created plain-text file.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    public static Path generatePlain(final Submission submission,
            final Path outdir, String courseName, String exerciseName)
            throws IOException {
        final File location = outdir.toFile();

        File outputFile = new File(location, submission.getStudent().getName()
                + ".report.txt");
        if (Files.exists(outputFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(outputFile.toPath());
        }
        outputFile.createNewFile();

        writeHeader(outputFile, submission, courseName, exerciseName);
        writeOverview(outputFile, submission);
        writeTestResult(outputFile, submission);

        // if there are compile errors, put these in the text file instead of
        // JUnit Test result
        CheckingResult checkingResult = submission.getCheckingResult();
        if (checkingResult.getCompilerOutput().compilerStreamBroken()) {
            writeCompilerErrors(outputFile, submission);
        } else {
            TestOutput testResults = checkingResult.getTestResults();
            if ((testResults.getPassedTestCount() < testResults.getTestCount())
                    && testResults.getDidTest()) {
                writeFailedTests(outputFile, submission);
            }
        }

        writeCompilerOutput(outputFile, submission);

        return outputFile.toPath();
    }

    /**
     * Writes the compiler errors into the text file.
     * 
     * @param file
     *            File the compiler errors get written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeCompilerErrors(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("Compilerfehler\n");
        for (String error : submission.getCheckingResult().getCompilerOutput()
                .getCompilerErrors()) {
            writer.append(error + "\n");
        }

        writer.close();
    }

    /**
     * Writes the compiler output into the text file.
     * 
     * @param file
     *            File the compiler output gets written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeCompilerOutput(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("Compilerausgabe:\n");
        for (String warning : submission.getCheckingResult()
                .getCompilerOutput().getCompilerWarnings()) {
            writer.append(warning + "\n");
        }

        for (String info : submission.getCheckingResult().getCompilerOutput()
                .getCompilerInfos()) {
            writer.append(info + "\n");
        }
        writer.append("\n");
        writer.close();

    }

    /**
     * Writes the failed tests into the text file.
     * 
     * @param file
     *            File the failed tests get written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeFailedTests(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("Fehlerhafte Tests\n");

        for (int i = 0; i < submission.getCheckingResult().getTestResults()
                .getResults().size(); i++) {
            if (!(submission.getCheckingResult().getTestResults().getResults()
                    .get(i).wasSuccessful())) {
                writer.append("- Test" + i + "\n");
                for (Failure fail : submission.getCheckingResult()
                        .getTestResults().getResults().get(i).getFailures()) {
                    writer.append(fail.toString() + "\n");
                }
            }
        }

        writer.close();
    }

    /**
     * Writes the header into the text file.
     * 
     * @param file
     *            File the overhead gets written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @param courseName
     *            the name of the course
     * @param exerciseName
     *            the name of the exercise
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void writeHeader(File file, Submission submission,
            String courseName, String exerciseName) throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append(courseName);

        writer.append("Übungsblatt :" + exerciseName + "\n\n");

        writer.append(submission.getStudent().getName() + "\n");

        writer.close();
    }

    /**
     * Writes the Overview into the text file.
     * 
     * @param file
     *            File the overview gets written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeOverview(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("Übersicht\n");

        if (submission.getCheckingResult().getCompilerOutput().isCleanCompile()) {
            writer.append("Abgabe kompiliert\n");
        } else {
            writer.append("Abgabe kompiliert nicht\n");
        }
        writer.append("Testergebnis: "
                + submission.getCheckingResult().getTestResults()
                        .getPassedTestCount()
                + " von "
                + submission.getCheckingResult().getTestResults()
                        .getTestCount() + " Tests bestanden\n");

        writer.close();
    }

    /**
     * Writes the test result into the text file.
     * 
     * @param file
     *            File the test results get written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeTestResult(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);
        writer.append("Testergebnis\n");

        if (submission.getCheckingResult().getTestResults().getDidTest()) {
            for (int i = 0; i < submission.getCheckingResult().getTestResults()
                    .getResults().size(); i++) {
                if (submission.getCheckingResult().getTestResults()
                        .getResults().get(i).wasSuccessful()) {
                    writer.append("Test " + i + "\tpassed\n");
                } else {
                    writer.append("Test " + i + "\tfailed\n");
                }
            }
        } else {
            writer.append("Keine Tests vorhanden.\n");
        }
        writer.close();
    }

}
