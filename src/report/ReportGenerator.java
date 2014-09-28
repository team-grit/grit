package report;

import preprocess.Student;
import preprocess.tokenize.Submission;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by gabriel on 28/09/14.
 */
public class ReportGenerator {

    public enum ReportType {
        PDF, PLAIN
    }

    public static void generateReport(
            Submission submission, Path targetLocation, String courseName,
            String exerciseName, ReportType type) throws IOException {
        switch (type) {
        case PDF:
            PdfCreator.createPdfFromSubmission(submission,
                    targetLocation, courseName, exerciseName);
            break;
        case PLAIN:
            PlainGenerator
                    .generatePlain(submission, targetLocation, courseName,
                            exerciseName);
            break;
        }
    }

    public static void concatenatePdfReports(
            Path folderWithPdfs, Path outPath, String exerciseName,
            List<Student> studentsWithoutSubmissions) throws IOException {
        PdfConcatenator.concatPDFS(folderWithPdfs, outPath, exerciseName,
                studentsWithoutSubmissions);
    }
}
