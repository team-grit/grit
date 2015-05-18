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

package de.teamgrit.grit.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.junit.runner.notification.Failure;

import de.teamgrit.grit.checking.CheckingResult;
import de.teamgrit.grit.checking.TestOutput;
import de.teamgrit.grit.preprocess.tokenize.Submission;

/**
 * This class supplies the static method generateCard to generate a .tex file
 * out of a SubmissionObj.
 * 
 * @author <a href="mailto:thomas.3.schmidt@uni-konstanz.de">Thomas Schmidt</a>
 * 
 */

class TexGenerator {

    private TexGenerator(){
        // prevent initialisation
    }

    /**
     * This method creates a TeX file from a Submission instance.
     * 
     * @param submission
     *            A SubmissionObj containing the information that the content
     *            gets generated from.
     * @param outdir
     *            the output directory
     * @param courseName
     *            the name of the course the exercise belongs to
     * @param exerciseName
     *            the name of the exercise
     * @return The Path to the created TeX file.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    public static Path generateTex(final Submission submission,
            final Path outdir, final String courseName,
            final String exerciseName) throws IOException {

        final File location = outdir.toFile();

        File file = new File(location, submission.getStudent().getName()
                + ".report.tex");
        if (Files.exists(file.toPath())) {
            Files.delete(file.toPath());
        }
        file.createNewFile();

        writePreamble(file);
        writeHeader(file, submission, courseName, exerciseName);
        writeOverview(file, submission);
        writeTestResult(file, submission);

        // if there are compile errors, put these in the .tex file instead of
        // JUnit Test result
        CheckingResult checkingResult = submission.getCheckingResult();
        if (!(checkingResult.getCompilerOutput().isCleanCompile())) {
            writeCompilerErrors(file, submission);
        } else {
            TestOutput testResults = checkingResult.getTestResults();
            if ((testResults.getPassedTestCount() < testResults.getTestCount())
                    && testResults.getDidTest()) {
                writeFailedTests(file, submission);
            }
        }

        writeCompilerOutput(file, submission);
        writeSourceCode(file, submission);
        writeClosing(file);

        return file.toPath();
    }

    /**
     * Writes the closing into the TeX file.
     * 
     * @param file
     *            File the closing gets written into.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeClosing(File file) throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("\\end{student}\n");
        writer.append("\\label{lastpage}");
        writer.append("\\end{document}\n");

        writer.close();
    }

    /**
     * Writes the compiler errors into the TeX file.
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

        writer.append("\\paragraph{Compilerfehler}~\\\\\n");
        writer.append("\\begin{lstlisting}[language=bash, breaklines=true, "
                + "basicstyle=\\color{black}\\footnotesize\\ttfamily,numberstyle"
                + "=\\tiny\\color{black}]\n");
        for (String error : submission.getCheckingResult().getCompilerOutput()
                .getCompilerErrors()) {
            writer.append(error + "\n");
        }
        writer.append("\\end{lstlisting}\n");

        writer.close();
    }

    /**
     * Writes the compiler output into the TeX file.
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

        writer.append("\\paragraph{Compilerausgabe}~\\\\\n");
        writer.append("\\color{black}\n");
        writer.append("\\begin{lstlisting}[language=bash, "
                + "breaklines=true]{Warnings}\n");
        for (String warning : submission.getCheckingResult()
                .getCompilerOutput().getCompilerWarnings()) {
            writer.append(warning + "\n");
        }
        writer.append("\\end{lstlisting}\n");

        writer.append("\\begin{lstlisting}[language=bash, "
                + "breaklines=true]{Infos}\n");
        for (String info : submission.getCheckingResult().getCompilerOutput()
                .getCompilerInfos()) {
            writer.append(info + "\n");
        }
        writer.append("\\end{lstlisting}\n");

        writer.close();
    }

    /**
     * Writes the failed tests into the TeX file.
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
        if (submission.getCheckingResult().getTestResults().getDidTest()) {
            writer.append("\\paragraph{Fehlerhafte Tests}~\\\\\n");
            writer.append("\\begin{itemize}\n");

            for (int i = 0; i < submission.getCheckingResult().getTestResults()
                    .getResults().size(); i++) {
                if (!(submission.getCheckingResult().getTestResults()
                        .getResults().get(i).wasSuccessful())) {
                    writer.append("\\item{Test" + i + "}\\\n");
                    writer.append("\\begin{lstlisting}[language=bash, "
                            + "breaklines=true, basicstyle=\\color{red}"
                            + "\\footnotesize\\ttfamily,numberstyle"
                            + "=\\tiny\\color{black}]{Fehler}\n");
                    for (Failure fail : submission.getCheckingResult()
                            .getTestResults().getResults().get(i).getFailures()) {
                        writer.append(fail.toString() + "\n");
                    }
                    writer.append("\\end{lstlisting}\n");
                }
            }
        }

        writer.close();
    }

    /**
     * Writes the header into the TeX file.
     * 
     * @param file
     *            File the overhead gets written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @param courseName
     *            the name of the course the exercise belongs to
     * @param exerciseName
     *            the name of the exercise
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void writeHeader(File file, Submission submission,
            final String courseName, final String exerciseName)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("\\newcommand{\\studycourse}{" + courseName + "}\n");
        writer.append("\\newcommand{\\assignmentnumber}{" + exerciseName
                + "}\n");
        writer.append("\\begin{document}\n");

        writer.append("\\begin{student}{" + submission.getStudent().getName()
                + "}\n");

        writer.close();
    }

    /**
     * Writes the Overview into the .tex file.
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

        writer.append("\\paragraph{Übersicht}~\\\\\n");

        CheckingResult checkingResult = submission.getCheckingResult();
        if (checkingResult.getCompilerOutput().isCleanCompile()) {
            writer.append("Abgabe kompiliert \\hfill \\textcolor{green}{JA}\\\\");
        } else {
            writer.append("Abgabe kompiliert \\hfill \\textcolor{red}{NEIN}\\\\ \n");
        }
        writer.append("Testergebnis \\hfill "
                + checkingResult.getTestResults().getPassedTestCount()
                + " von " + checkingResult.getTestResults().getTestCount()
                + " Tests bestanden\n");

        writer.close();
    }

    /**
     * Writes the Preamble into the .tex file.
     * 
     * @param file
     *            File the Preamble gets written into.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writePreamble(File file) throws IOException {
        final File preamble = new File(Paths.get(
                System.getProperty("user.dir"), "res", "tex",
                "report_preamble.tex").toUri());

        String preambleToString = FileUtils.readFileToString(preamble, "UTF-8");

        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);
        writer.append(preambleToString);
        writer.append("\n\n");

        writer.close();
    }

    /**
     * Writes the source code into the .tex file.
     * 
     * @param file
     *            File the source code gets written into.
     * @param submission
     *            SubmissionObj the needed information gets taken from.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeSourceCode(File file, Submission submission)
            throws IOException {
        FileWriterWithEncoding writer = new FileWriterWithEncoding(file,
                "UTF-8", true);

        writer.append("\\paragraph{Code}~\\\\\n");

        for (File f : FileUtils.listFiles(submission.getSourceCodeLocation()
                .toFile(), FileFilterUtils.fileFileFilter(), TrueFileFilter.INSTANCE)) {

            // determines programming language of the file and adjusts the
            // lstlisting according to it
            String language = "no valid file";
            String fileExtension = FilenameUtils.getExtension(f.toString());

            if (fileExtension.matches("[Jj][Aa][Vv][Aa]")) {
                language = "Java";
            } else if (fileExtension.matches("([Ll])?[Hh][Ss]")) {
                language = "Haskell";
            } else if (fileExtension.matches("[Cc]|[Hh]")) {
                language = "C";
            } else if (fileExtension.matches("[Cc][Pp][Pp]")) {
                language = "C++";
            } else {
                // file is not a valid source file
                continue;
            }

            writer.append("\\lstinputlisting[language=" + language);

            writer.append(", breaklines=true]{"
                    + FilenameUtils.separatorsToUnix((f.getAbsolutePath()))
                    + "}\n");

        }
        writer.close();
    }

    /**
     * Writes the test result into the .tex file.
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
        writer.append("\\paragraph{Testergebnis}~\\\\\n");

        TestOutput testResults = submission.getCheckingResult()
                .getTestResults();

        if (testResults.getDidTest() && (testResults.getTestCount() > 0)) {
            writer.append("Bestandene Tests \\hfill\\progressbar[subdivisions="
                    + testResults.getTestCount()
                    + ", ticksheight=1, emptycolor=red, filledcolor=green]{"
                    + (testResults.getPassedTestCount() / testResults
                            .getTestCount())
                    + "} \\\\\n");
            for (int i = 0; i < testResults.getResults().size(); i++) {
                if (testResults.getResults().get(i).wasSuccessful()) {
                    writer.append("Test " + i + "\\hfill \\checkedbox \\\\\n");
                } else {
                    writer.append("\\textcolor{red}{Test " + i
                            + "\\hfill \\XBox} \\ \\\n");
                }
            }
            writer.append("\\pagebreak\n");
        } else {
            writer.append("Keine Tests vorhanden.\n");
        }
        writer.close();
    }

}
