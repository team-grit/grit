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
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;

import preprocess.Student;

/**
 * The PdfConcatinator is an utility class to merge the pdfs generated by the
 * {@link TexGenerator} .
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */

public abstract class PdfConcatenator {

    /**
     * Concatinates pdfs generated {@link TexGenerator}.
     *
     * @param folderWithPdfs
     *            the folder with pdfs
     * @param outPath
     *            the out path
     * @param exerciseName
     *            the context
     * @param studentsWithoutSubmissions
     *            list of students who did not submit any solution
     * @return the path to the created PDF
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static Path concatPDFS(
            Path folderWithPdfs, Path outPath, String exerciseName,
            List<Student> studentsWithoutSubmissions) throws IOException {

        if ((folderWithPdfs == null) || !Files.isDirectory(folderWithPdfs)) {
            throw new IOException("The Path doesn't point to a Folder");
        }

        File file = new File(outPath.toFile(), "report.tex");

        if (Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(file.toPath());
        }
        file.createNewFile();

        writePreamble(file, exerciseName);
        writeMissingStudents(file, studentsWithoutSubmissions);
        writeFiles(file, folderWithPdfs);
        writeClosing(file);

        PdfCreator.createPdfFromPath(file.toPath(), outPath);

        return file.toPath();

    }

    /**
     * Writes the preamble and title to the file.
     *
     * @param file
     *            the file
     * @param exerciseName
     *            the name of the exercise
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void writePreamble(File file, String exerciseName)
            throws IOException {
        FileWriterWithEncoding writer =
                new FileWriterWithEncoding(file, "UTF-8", true);

        writer.append("\\documentclass[a4paper,10pt,ngerman]{scrartcl} \n");
        writer.append("\\usepackage[ngerman]{babel}\n");
        writer.append("\\usepackage[utf8]{inputenc}");
        writer.append("\\usepackage{pdfpages} \n");
        writer.append("\\usepackage{grffile} \n");
        writer.append("\\begin{document} \n");
        writer.append("\\begin{titlepage}\n");
        writer.append("\\begin{center}\n");
        writer.append("\\textsc{\\LARGE Universität Konstanz}\\\\[1.5cm]\n\n");
        writer.append("{\\large Korrektur\n\n");
        writer.append("\\rule{\\linewidth}{0.5mm}\\\\[0.4cm]\n");
        writer.append("{\\fontfamily{qhv}\\huge\\bfseries " + exerciseName
                + " \\\\[0.4cm]}\n\n");
        writer.append("\\rule{\\linewidth}{0.5mm}\\\\[0.5cm]\n\n");
        writer.append("\\vfill\n");
        writer.append("{\\large\\today\n");
        writer.append("\\end{center}\n");
        writer.append("\\end{titlepage}\n");

        writer.close();
    }

    /**
     * Write the pdfs to the file.
     *
     * @param outFile
     *            the out file
     * @param folderWithPdfs
     *            the folder with pdfs
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void writeFiles(File outFile, Path folderWithPdfs)
            throws IOException {

        FileWriterWithEncoding writer =
                new FileWriterWithEncoding(outFile, "UTF-8", true);

        File[] files = folderWithPdfs.toFile().listFiles();
        for (File file : files) {

            // We only want the the PDFs as input
            if ("pdf".equals(FilenameUtils.getExtension(file.getName()))) {
                writer.append("\\includepdf[pages={1-}]{"
                        + file.getAbsolutePath() + "} \n");
            }
        }
        writer.close();
    }

    /**
     * Writes the closing part of the file.
     *
     * @param file
     *            the file
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void writeClosing(File file) throws IOException {
        FileWriterWithEncoding writer =
                new FileWriterWithEncoding(file, "UTF-8", true);

        writer.append("\\label{lastpage}");
        writer.append("\\end{document}\n");

        writer.close();
    }

    /**
     * Writes the names of the students that didn't hand in a submission.
     *
     * @param file
     *            the file that gets written into.
     * @param studentWithoutSubmissions
     *            a list with students who did not submit any solution.
     * @throws IOException
     *             If something goes wrong when writing.
     */
    private static void writeMissingStudents(
            File file, List<Student> studentWithoutSubmissions)
            throws IOException {

        // // the list of students who didn't hand in a submission
        // List<Student> missing = context.getPreprocessor()
        // .getStudentsWithoutSubmission();

        // only write if there are any missing submissions
        if (!(studentWithoutSubmissions == null)
                && !(studentWithoutSubmissions.isEmpty())) {
            FileWriterWithEncoding writer =
                    new FileWriterWithEncoding(file, "UTF-8", true);

            writer.append("{\\LARGE\\bf Studenten welche nicht abgegeben haben:}\\\\\n\\\\");
            writer.append("\\begin{minipage}{.5\\textwidth}\n");
            writer.append("\\begin{itemize}\n");
            for (int i = 0; i < studentWithoutSubmissions.size(); i += 2) {
                writer.append("\\item "
                        + studentWithoutSubmissions.get(i).getName() + "\n");
            }
            writer.append("\\end{itemize}\n");
            writer.append("\\end{minipage}");

            writer.append("\\begin{minipage}{.5\\textwidth}\\raggedright\n");
            writer.append("\\begin{itemize}\n");
            for (int i = 1; i < studentWithoutSubmissions.size(); i += 2) {
                writer.append("\\item "
                        + studentWithoutSubmissions.get(i).getName() + "\n");
            }
            writer.append("\\end{itemize}\n");
            writer.append("\\end{minipage}\n");
            writer.close();
        }
    }
}
