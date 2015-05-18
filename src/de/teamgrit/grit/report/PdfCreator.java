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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import de.teamgrit.grit.preprocess.tokenize.Submission;

/**
 * This class provides methods to create a pdf Report containing need
 * information for the corrector of the submission using "pdflatex".
 * 
 * @author <a href="mailto:thomas.3.schmidt@uni-konstanz.de">Thomas Schmidt</a>
 * 
 */

public abstract class PdfCreator {

    /**
     * Creates a pdf from a given Path to a .tex file.
     * 
     * 
     * @param pathToTexFile
     *            A Path to a .tex file.
     * @throws IOException
     *             if pdflatex encounters a problem
     */
    public static void createPdfFromPath(Path pathToTexFile) throws IOException {
        createPdfFromPath(pathToTexFile, pathToTexFile.getParent());
    }

    /**
     * Creates a pdf from a given Path to a .tex file in the given output
     * directory.
     * 
     * @param pathToTexFile
     *            A Path to a .tex file.
     * @param outputDir
     *            the output directory
     * @throws IOException
     *             If something goes wrong when executing pdflatex.
     */
    public static void createPdfFromPath(Path pathToTexFile, Path outputDir)
            throws IOException {
        List<String> compilerInvocation = new LinkedList<>();

        compilerInvocation.add("pdflatex");
        compilerInvocation.add("-interaction=batchmode");
        compilerInvocation.add("--output-directory=" + outputDir.toString());

        if ((pathToTexFile == null)
                || (pathToTexFile.compareTo(Paths.get("")) == 0)) {
            throw new FileNotFoundException("No file to compile specified");
        } else {
            compilerInvocation.add(pathToTexFile.toString());
        }

        Process compilerProcess;
        ProcessBuilder compilerProcessBuilder = new ProcessBuilder(
                compilerInvocation).redirectErrorStream(true);

        compilerProcessBuilder.directory(new File(System
                .getProperty("user.dir")));
        compilerProcess = compilerProcessBuilder.start();

        // we need to consume the output of pdflatex to stop it from
        // freezing on windows
        final InputStream stdout = compilerProcess.getInputStream();

        // Fixes a bug with Windows (outputstream buffer is too small)
        final byte[] devnull = new byte[2048];
        while (stdout.read(devnull) != -1) {
            ;
        }

    }

    /**
     * Creates a pdf Report from a given {@link Submission} (using
     * {@link de.teamgrit.grit.report.PdfCreator#createPdfFromPath}).
     * 
     * @param submission
     *            A SubmissionObj containing the information that the content
     *            gets generated from.
     * @param outdir
     *            the directory where the finished PDF is put.
     * @param courseName
     *            the name of the course the exercise belongs to
     * @param exerciseName
     *            the name of the exercise
     * @throws IOException
     *             If something goes wrong when executing pdflatex.
     * 
     */
    protected static void createPdfFromSubmission(Submission submission,
            Path outdir, String courseName, String exerciseName)
            throws IOException {
        createPdfFromPath(TexGenerator.generateTex(submission, outdir,
                courseName, exerciseName));
    }

}
