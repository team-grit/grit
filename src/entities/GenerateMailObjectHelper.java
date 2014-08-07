package entities;

import preprocess.Student;
import preprocess.tokenize.Submission;
import util.mailer.MailObjectWithConnection;

/**
 * Class to help with the creation of mailObjects.
 *
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 *
 */
public class GenerateMailObjectHelper {

    /**
     * Creates a mailObject with the correct mail text when a submission is
     * lacking necessary files.
     *
     * @param submission
     *            submission containing needed info for the creation of the
     *            mailObject
     * @param context
     *            ExerciseContext holding info about the exercise
     * @return MailObjectWithConnection
     */
    public static MailObjectWithConnection generateMailObjectMissingFiles(
            Submission submission, ExerciseContext context) {

        String recipientMailAdress = submission.getStudent().getEmail();
        String senderMailAdress =
                Controller.getController().getConfig().getSenderMailAdress();
        String emailPassword =
                Controller.getController().getConfig().getMailPassword();
        String smtpHost = Controller.getController().getConfig().getSmtpHost();
        String exerciseName = context.getExcerciseName();
        String studentName = submission.getStudent().getName();
        String missingFiles;
        String deadline = context.getDeadlineString();

        // generate correct message string
        switch (context.getLanguageType()) {
        case JAVA:
            missingFiles = "Es fehlen .java Dateien.";
            break;
        case C:
            missingFiles = "Es fehlen .c Dateien.";
            break;
        case CPP:
            missingFiles = "Es fehlen .cpp Dateien.";
            break;
        case HASKELL:
            missingFiles = "Es fehlen .hs oder .lhs Dateien.";
            break;
        default:
            throw new IllegalArgumentException(
                    "Language Type is not supported!");
        }

        MailObjectWithConnection mailData =
                new MailObjectWithConnection(senderMailAdress,
                        recipientMailAdress, null, null, emailPassword,
                        smtpHost, 465, true, 465);

        // set the mail message
        mailData.setMailStudentProblems(exerciseName, studentName,
                missingFiles, deadline);

        return mailData;
    }

    /**
     * Creates a mailObject with the correct mail text when a submission is not
     * compiling.
     *
     * @param submission
     *            submission containing needed info for the creation of the
     *            mailObject
     * @param context
     *            ExerciseContext holding info about the exercise
     * @return MailObjectWithConnection
     */
    public static MailObjectWithConnection generateMailObjectDoesNotCompile(
            Submission submission, ExerciseContext context) {

        String recipientMailAdress = submission.getStudent().getEmail();
        String senderMailAdress =
                Controller.getController().getConfig().getSenderMailAdress();
        String emailPassword =
                Controller.getController().getConfig().getMailPassword();
        String smtpHost = Controller.getController().getConfig().getSmtpHost();
        String exerciseName = context.getExcerciseName();
        String studentName = submission.getStudent().getName();
        String deadline = context.getDeadlineString();
        String compilerOutput = "Die Abgabe compiliert nicht." + "\n\n";

        // generate an message string from the compilerOutput containing all
        // compiling errors, warnings and infos
        for (String error : submission.getCheckingResult().getCompilerOutput()
                .getCompilerErrors()) {
            compilerOutput += "Compiler Errors:" + "\n\n";
            compilerOutput += error + "\n";
            compilerOutput += "\n\n";
        }

        for (String warning : submission.getCheckingResult()
                .getCompilerOutput().getCompilerWarnings()) {
            compilerOutput += "Compiler Warnings:" + "\n\n";
            compilerOutput += warning + "\n";
            compilerOutput += "\n\n";
        }

        for (String info : submission.getCheckingResult().getCompilerOutput()
                .getCompilerInfos()) {

            compilerOutput += "Compiler Infos:" + "\n\n";
            compilerOutput += info + "\n";
            compilerOutput += "\n\n";
        }

        MailObjectWithConnection mailData =
                new MailObjectWithConnection(senderMailAdress,
                        recipientMailAdress, null, null, emailPassword,
                        smtpHost, 465, true, 465);

        // set the mail message
        mailData.setMailStudentProblems(exerciseName, studentName,
                compilerOutput, deadline);

        return mailData;
    }

    /**
     * Creates a mailObject with the correct mail text when a student did not
     * submit anything.
     *
     * @param stud
     *            student without submission
     * @param context
     *            ExerciseContext holding info about the exercise
     * @return MailObjectWithConnection
     */
    public static MailObjectWithConnection generateMailObjectNoSubmission(
            Student stud, ExerciseContext context) {
        String recipientMailAdress = stud.getEmail();
        String senderMailAdress =
                Controller.getController().getConfig().getSenderMailAdress();
        String emailPassword =
                Controller.getController().getConfig().getMailPassword();
        String smtpHost = Controller.getController().getConfig().getSmtpHost();
        String exerciseName = context.getExcerciseName();
        String studentName = stud.getName();
        String deadline = context.getDeadlineString();
        String problem = "Du hast bis jetzt noch nichts Abgegeben.";

        MailObjectWithConnection mailData =
                new MailObjectWithConnection(senderMailAdress,
                        recipientMailAdress, null, null, emailPassword,
                        smtpHost, 465, true, 465);

        // set the mail message
        mailData.setMailStudentProblems(exerciseName, studentName, problem,
                deadline);

        return mailData;
    }

    /**
     * Creates a mailObject with the correct mail text to notify the admin when
     * a exercise has finished.
     *
     * @param exerciseID
     *            the number of the finished exercise
     * @param context
     *            ExerciseContext holding info about the exercise
     * @return MailObjectWithConnection
     */
    public static MailObjectWithConnection generateMailObjectNotifyAdmin(
            ExerciseContext context, int exerciseID) {
        String senderMailAdress =
                Controller.getController().getConfig().getSenderMailAdress();
        String emailPassword =
                Controller.getController().getConfig().getMailPassword();
        String smtpHost = Controller.getController().getConfig().getSmtpHost();
        String recipientMailAdress =
                Controller.getController().getConfig().getMailAdmin();
        String courseName = context.getCourseName();
        String adminName =
                Controller.getController().getConfig().getAdminName();

        MailObjectWithConnection mailData =
                new MailObjectWithConnection(senderMailAdress,
                        recipientMailAdress, null, null, emailPassword,
                        smtpHost, 465, true, 465);

        // set the mail message
        mailData.setMailProfessorInformation(courseName, exerciseID, adminName);
        return mailData;
    }
}
