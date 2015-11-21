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

package de.teamgrit.grit.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Exception;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.runner.Result;

import com.google.gson.annotations.Expose;

import de.teamgrit.grit.checking.CheckingResult;
import de.teamgrit.grit.checking.CompilerOutput;
import de.teamgrit.grit.checking.TestOutput;
import de.teamgrit.grit.checking.compile.BadCompilerSpecifiedException;
import de.teamgrit.grit.checking.compile.BadFlagException;
import de.teamgrit.grit.checking.compile.CompileChecker;
import de.teamgrit.grit.checking.compile.CompilerOutputFolderExistsException;
import de.teamgrit.grit.checking.plausibility.SubmissionPlausibilityChecker;
import de.teamgrit.grit.checking.testing.Tester;
import de.teamgrit.grit.preprocess.Connection;
import de.teamgrit.grit.preprocess.PreprocessingResult;
import de.teamgrit.grit.preprocess.Preprocessors;
import de.teamgrit.grit.preprocess.Student;
import de.teamgrit.grit.preprocess.fetch.SubmissionFetchingException;
import de.teamgrit.grit.preprocess.tokenize.Submission;
import de.teamgrit.grit.report.ReportGenerator;
import de.teamgrit.grit.util.config.NoProperParameterException;

/**
 * The Class representing an Exercise. Needs to be injected with an
 * {@link ExerciseContext} produced by the {@link ExerciseContextFactory}.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 */
public class Exercise {

	// ------------------------------ FIELDS ------------------------------

	private static final Logger LOGGER = Logger.getLogger("systemlog");

	/**
	 * Reference to the controller instance.
	 */
	private Controller m_controller;

	@Expose
	private final int id;

	@Expose
	private ExerciseContext context = null;

	/**
	 * This string indicates the processing status, so it can be displayed on
	 * the website.
	 */
	@Expose
	private String status = "";

	private Map<Student, Submission> m_submissions = new HashMap<>();

	private final ScheduledExecutorService m_taskPool = Executors
			.newScheduledThreadPool(1);

	// --------------------------- CONSTRUCTOR ---------------------------

	/**
	 * To create an {@link Exercise} you need to provide
	 * {@link ExerciseContext}.
	 * 
	 * @param id
	 *            the ID of the new exercise (the {@link Course} takes care of
	 *            assigning IDs to exercises)
	 * @param context
	 *            An {@link ExerciseContext} produced by the
	 *            {@link ExerciseContextFactory}
	 * @throws NoProperParameterException
	 *             If the context is null.
	 */
	public Exercise(int id, ExerciseContext context)
			throws NoProperParameterException {
		m_controller = Controller.getController();

		if (context != null) {
			this.context = context;
			status = "not started yet";
		} else {
			throw new de.teamgrit.grit.util.config.NoProperParameterException(
					"m_context is " + "null");
		}

		// create the working directories
		try {
			Files.createDirectories(context.getTempPdfPath());
			Files.createDirectories(context.getTests());
			Files.createDirectories(context.getBinPath());
			Files.createDirectories(context.getFetchPath());
			Files.createDirectories(context.getOutputPath());
		} catch (IOException e) {
			LOGGER.severe("Couldn't create necessary directories for"
					+ " exercise + " + context.getExerciseName() + ": "
					+ e.toString());
			status = "aborted exercise creation";
		}

		this.id = id;

		if (isDeadlinePassed()) {
            File reportFile =
                    context.getOutputPath().resolve("report.pdf").toFile();
			if (reportFile.exists()) {
				status = "ready for download";
				return;
			}
		}

		long initialDelay;
		if (System.currentTimeMillis() >= context.getStartTime()
				.getTimeInMillis()) {
			initialDelay = 0;
		} else {
            initialDelay =
                    context.getStartTime().getTimeInMillis()
					- System.currentTimeMillis();
		}
		m_taskPool.scheduleAtFixedRate(new Task(), initialDelay,
				context.getPeriod(), TimeUnit.MILLISECONDS);
	}

	/**
	 * The task will be executed as a separate thread. This happens from start
	 * until deadline in a specified period and a single time after the
	 * deadline.
	 * 
	 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano
	 *         Woerner</a>
	 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
	 */
	private class Task implements Runnable {
		/**
		 * Runs a task. Depending on the current time there are two execution
		 * paths as specified in {@link Exercise#preDeadlineProcessing()} and
		 * {@link Exercise#postDeadlineProcessing()}.
		 */
		@Override
		public void run() {
			if (isDeadlinePassed()) {
				LOGGER.info("Deadline Passed");
				postDeadlineProcessing();
			} else {
				preDeadlineProcessing();
			}
		}
	}

	/**
	 * Before the deadline, the submissions are incrementally processed as
	 * follows:
	 * <ol>
	 * <li>The submissions are downloaded from the specified source.</li>
	 * <li>The submissions are checked for plausibility.</li>
	 * <li>The submissions are compiled.</li>
	 * <li>An email is sent to the student with an invalid or corrupted
	 * submission or no submission at all.</li>
	 * </ol>
	 */
	private void preDeadlineProcessing() {

		/* Local variables for the metadata */
		String courseName = context.getCourseName();
		String exerciseName = context.getExerciseName();
        Connection connection =
                m_controller.getConnection(context.getConnectionId());
		Path targetDirectory = context.getFetchPath();
		String fileRegex = context.getFileRegex();
		Path tempPdfPath = context.getTempPdfPath();
		Path binpath = context.getBinPath();
		String archiveRegex = context.getArchiveRegex();
		Date startTime = context.getStartTime().getTime();
		Date endTime = context.getDeadline().getTime();

		LOGGER.info("Started pre deadline processing of exercise: "
				+ exerciseName + " from the course: " + courseName);

		/* download the submissions from the source and tokenize them. */
		status = "fetching submissions";
		PreprocessingResult result = null;
		try {
            result =
                    Preprocessors.preprocess(connection, startTime, endTime,
                      targetDirectory, courseName, exerciseName,
                      fileRegex, archiveRegex);
		} catch (SubmissionFetchingException e) {
			context.logError(e.toString());
			status = "error while fetching submissions";
			return;
		}
		/* Find the submissions that need to be checked */
		Map<Student, Submission> submissions = result.getStudentSubmissions();
		List<Submission> submissionsToProcess = new ArrayList<>();
		for (Map.Entry<Student, Submission> entry : submissions.entrySet()) {
			if (!entry.getValue().equals(m_submissions.get(entry.getKey()))) {
				submissionsToProcess.add(entry.getValue());
			}
		}
		/* store the students that did not turn in */
		m_submissions = submissions;
        List<Student> studentsWithoutSubmission =
                result.getStudentsWithoutSubmission();

		/* check plausibility of the submissions. */
		status = "processing submissions";
		for (Submission submission : m_submissions.values()) {
			try {
				LOGGER.info("checking plausability");
				checkPlausibility(submission);
				if (submission.isPlausible()) {
					/* compile */
					LOGGER.info("compiling submissions");
                    CompilerOutput compileResult =
                            compileSubmission(submission);
					LOGGER.info("testing submission");
                    TestOutput testResult =
                            testSubmission(binpath, compileResult);
					/* create checking result */
					submission.setCheckingResult(new CheckingResult(
							compileResult, testResult));

					/* score card creation */
					LOGGER.info("generate scorecard");
					ReportGenerator.generateReport(submission, tempPdfPath,
							courseName, exerciseName,
							ReportGenerator.ReportType.PDF);
					try {
						FileUtils.cleanDirectory(binpath.toFile());
					} catch (IOException e) {
						LOGGER.severe("Could not delete output directory (binpath)");
					}
				}
			} catch (BadCompilerSpecifiedException | BadFlagException
                    | CompilerOutputFolderExistsException
                    | IOException e) {
				LOGGER.severe("Error during processing a submission, "
						+ "it was skipped. " + e.toString());
			}
		}
		try {
			ReportGenerator.concatenatePdfReports(context.getTempPdfPath(),
					context.getOutputPath(), context.getExerciseName(),
					studentsWithoutSubmission, "tempReport.tex");
		} catch (IOException e) {
			context.logError("error while merging scorecards: "
					+ e.toString());
			status = "error while generating pdf for printout";
			m_taskPool.shutdownNow();
			return;
		}

		if (isDeadlinePassed()) {
			postDeadlineProcessing();
		} else {
			/* send mails to the students with corrupted submissions. */
			status = "sending emails";
            //conType is necessary when the E-Mail submission system is used, to send a notification e-mail
			// to the sutdents which have send a solution.
			try {
            String conType = 
              Controller.getController().getConnection(id).getConnectionType().toString();
				if (conType.equals("MAIL"))
					sendReceivingNotification(submissionsToProcess);
				notifyStudentsWithCorruptedSubmission(submissionsToProcess);
				notifyStudentsWithoutSubmission(studentsWithoutSubmission);
			} catch (NullPointerException e) {
				status = "Error while sending Mail";
				LOGGER.severe("Error while sending Mail. Reason: "
						+ e.toString());
			}
			status = "waiting";
		}
	}

	/**
	 * After the deadline, all submissions are processed as follows:
	 * <ol>
	 * <li>The submissions are downloaded from the specified source.</li>
	 * <li>The submissions are checked for plausibility.</li>
	 * <li>The submissions are compiled.</li>
	 * <li>The submissions are tested with the supplied test.</li>
	 * <li>A scorecard is generated from the results of compiling and testing
	 * for each submissions.</li>
	 * <li>The scorecards are merged into one report file for easy printing,
	 * which is put into the output directory.</li>
     * <li>A notification email is sent to the administrator when the
     * processing is done and the report is ready for download.</li>
	 * </ol>
	 */
	private void postDeadlineProcessing() {
		/* Local variables for the metadata */
		Path tempPdfPath = context.getTempPdfPath();
		Path binpath = context.getBinPath();
		Path targetDirectory = context.getFetchPath();
		String courseName = context.getCourseName();
		String exerciseName = context.getExerciseName();

        Connection connection =
                m_controller.getConnection(context.getConnectionId());
		String fileRegex = context.getFileRegex();
		String archiveRegex = context.getArchiveRegex();
		Date startTime = context.getStartTime().getTime();
		Date endTime = context.getDeadline().getTime();

		/* download the submissions from the source and tokonize them. */
		status = "fetching submissions";
		PreprocessingResult result = null;
		try {
            result =
                 Preprocessors.preprocess(connection, startTime, endTime,
                    targetDirectory, courseName, exerciseName,
                    fileRegex, archiveRegex);
		} catch (SubmissionFetchingException e) {
			context.logError(e.toString());
			status = "error while fetching submissions";
			m_taskPool.shutdownNow();
			return;
		}
		Map<Student, Submission> submissions = result.getStudentSubmissions();
		m_submissions = submissions;

        List<Student> studentsWithoutSubmission =
                result.getStudentsWithoutSubmission();

		/* check plausibility of the submission. */
		status = "processing submissions";
		for (Submission submission : m_submissions.values()) {
			try {
				checkPlausibility(submission);
				if (submission.isPlausible()) {
					/* compile */
					LOGGER.info("compiling submission");
                    CompilerOutput compileResult =
                            compileSubmission(submission);
					/* test */
					LOGGER.info("testing submission");
                    TestOutput testResult =
                            testSubmission(binpath, compileResult);
					/* create checking result */
					submission.setCheckingResult(new CheckingResult(
							compileResult, testResult));

					/* score card creation */
					LOGGER.info("generate scorecard");
					ReportGenerator.generateReport(submission, tempPdfPath,
                            courseName, exerciseName, 
                            ReportGenerator.ReportType.PDF);
					try {
						FileUtils.cleanDirectory(binpath.toFile());
					} catch (IOException e) {
						LOGGER.severe("Could not delete output directory (binpath)");
					}
				}

			} catch (BadCompilerSpecifiedException | BadFlagException
					| CompilerOutputFolderExistsException | IOException e) {
				LOGGER.severe("Error during processing a submission, "
						+ "it was skipped. " + e.toString());
			}
		}

		/* merge the single scorecard files into a large one. */
		try {
			ReportGenerator.concatenatePdfReports(context.getTempPdfPath(),
					context.getOutputPath(), context.getExerciseName(),
					studentsWithoutSubmission, "report.tex");
		} catch (IOException e) {
            context.logError(
              "error while merging scorecards: " + e.toString());
			status = "error while generating pdf for printout";
			m_taskPool.shutdownNow();
			return;
		}

		/* delete all temporary files. */
        //cleanup();

		status = "ready for download";
		notifyAdmin();

		/* all done! */
		m_taskPool.shutdownNow();
	}

	/**
	 * Checks if the current time is past the deadline.
	 * 
	 * @return if the deadline is passed
	 */
	private boolean isDeadlinePassed() {
		// LOGGER.info("IsDeadLinePassed");
    	return System.currentTimeMillis() >= 
    		context.getDeadline().getTimeInMillis();
	}

	/**
	 * Gets the name of the Exercise.
	 * 
	 * @return the name
	 */
	public String getName() {
		return context.getExerciseName();
	}

	/**
	 * Gets the ID of the Exercise.
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * returns the exercise context.
	 * 
	 * @return the exercise context
	 */
	protected ExerciseContext getContext() {
		return context;
	}

	// --------------------- HELPER METHODS ---------------------

	/**
	 * Cleanup when stopping a task. Delete all generated files.
	 */
	private void cleanup() {
		// Delete binaries, test files, temporary files and fetched sources.
		try {
		  LOGGER.info("Cleaning up exercise "+context.getExerciseName());
			FileUtils.deleteDirectory(context.getBinPath().toFile());
			FileUtils.deleteDirectory(context.getTempPdfPath().toFile());
			FileUtils.deleteDirectory(context.getFetchPath().toFile());
			FileUtils.deleteDirectory(context.getTests().toFile());

			// Deleting auxiliary files for report.pdf generation
			File f = context.getOutputPath().toFile();
			File[] paths = f.listFiles();

			for (File file : paths) {
				String temp = file.toString();
				temp = temp.substring(temp.lastIndexOf("/") + 1);
				if (!(temp.equals("report.pdf")))
					file.delete();
			}

		} catch (NullPointerException e) {
			LOGGER.severe("Error while trying to clean up: "
					+ e.toString());
		} catch (IOException e) {
			LOGGER.severe("Error while trying to clean up: " + e.toString());
		}
	}

	/**
	 * getCompilerError returns the compiler errors, so that they can be send to
	 * the students in the method notifyStudentsWithCorruptedSubmission()
	 * 
	 * @param sub
	 *          The submission of the student with compiler errors
     * @return
     *          Returns the compilerOutput as a String
	 */
	private String getCompilerError(Submission sub) {
		// The following code creates a string with the compilerError.
		String compilerOutput = "Die Abgabe kompiliert nicht." + "\n\n";
		for (String error : sub.getCheckingResult().getCompilerOutput()
				.getCompilerErrors()) {
			compilerOutput += "Compiler Errors:" + "\n\n";
			compilerOutput += error + "\n";
			compilerOutput += "\n\n";
		}

        for (String warning : sub.getCheckingResult()
                .getCompilerOutput().getCompilerWarnings()) {
			compilerOutput += "Compiler Warnings:" + "\n\n";
			compilerOutput += warning + "\n";
			compilerOutput += "\n\n";
		}

		for (String info : sub.getCheckingResult().getCompilerOutput()
				.getCompilerInfos()) {

			compilerOutput += "Compiler Infos:" + "\n\n";
			compilerOutput += info + "\n";
			compilerOutput += "\n\n";
		}
		return compilerOutput;
	}

	/**
	 * Sends an E-Mail to every student who has send a solution as an email.
	 * 
	 * @param submissions
	 *            Every new submission which is received by the email-server.
     * 			  List of submissions which contain all relevant
     *            data in order to send mails
	 */
    private void sendReceivingNotification(
    			List<Submission> submissions) {
		for (Submission sub : submissions) {
			// Logger shows that submission(s) does not compile
			LOGGER.info("Send receiving notification.");

    		//The following code will create the 
    		//necessary variables to send an email.
			String receiverEmailAddress = sub.getStudent().getEmail();
    		String sendUserName = 
    				Controller.getController().getConfig().getSenderMailAdress();

    		// The following code creates the mailSubject and 
    		// the mailtext of the email which will be received by the student.
    		String mailSubject = new String(
    			"Ihre Abgabe f\u00FCr"
    		  + " Exercise " + getName() + " wurde empfangen.");
    		String mailText = new String(
    		    "Hallo " + sub.getStudent().getName() + ",\n" +
    		    "Ihre Abgabe wurde empfangen.\n\n" + 
    				"Mit freundlichen Gr\u00FC\u00DFen\n\n" + "Grit");

    		//The following code will send an email
    		// to the student(s) to infrom them that Grit received their email.
    		//The catch statements are necessary because of the EncryptorDecryptor.
			try {
				// EnrcryptorDecryptor ed = new EncryptorDecryptor();
				SendMail sendMail = new SendMail();
				sendMail.sendEMail(sendUserName, receiverEmailAddress,
    			    mailSubject, mailText, null);
			} catch (Exception e) {
    			LOGGER.severe("Exception during sending email because a"
    			    + "solution was received by MAIL " + e.toString());
			}
		}
	}

	/**
     * Checks if plausible flag or cleanCopile flag of submission is set or
     * not. Sends warning email to corresponding student if not. If set this
     * method does nothing.
	 * 
	 * @param submissions
	 *            List of submissions which contain all relevant data in order
	 *            to send mails
	 */
	private void notifyStudentsWithCorruptedSubmission(
			List<Submission> submissions) {

	  LOGGER.info("Checking submissions of course \""+
	      context.getExerciseName()+"\" for plausibility");

		/*
		 * for each submission we check if the flags isPlausible and
		 * isCleanCompile are set
		 */
		for (Submission sub : submissions) {

			/*
			 * if the submission is not plausible we send a warning mail to the
			 * student containing the message that files are missing
			 */
			if (!sub.isPlausible()) {
				LOGGER.finer("Submission of student "+sub.getStudent()+" is not plausible.");

            //The following code will create the necessary 
            //variables to send an email.
				    String receiverEmailAddress = sub.getStudent().getEmail();
        		String sendUserName = 
        		Controller.getController().getConfig().getSenderMailAdress();

        		//The following code creates the mailSubject and
        		//the mailtext of the email which will be 
        		//received by the student.
        		String mailSubject = new String(
						"Ihre Abgabe f\u00FCr Exercise " +
        		getName() + " ist nicht plausibel.");

        		String mailText = new String(
        		  "Hallo " + sub.getStudent().getName() + ",\n" +
        			"Ihre Abgabe ist nicht plausibel.\n" +
        			"Probleme sind: \n" + getCompilerError(sub) + "\n\n" +
        			"Sie haben bis " + context.getDeadlineString() +
        			" Zeit, dies zu korrigieren.\n\n" +
        			"Mit freundlichen Gr\u00FC\u00DFen\n\n" + "Grit");

        		// The following code will send an email to the student(s) to
        		// inform them that Grit received their email.
        		try {
        		  SendMail sendMail = new SendMail();
        			  sendMail.sendEMail(sendUserName, 
        			    receiverEmailAddress, mailSubject, mailText, null);
        		} catch (Exception e) {
        			LOGGER.severe("Exception during sending email because of "
        			    + "submissions which are not plausible: " + e.toString());
        		}

				/*
            	try {
                    SendMailSSL.sendMail(GenerateMailObjectHelper
                            .generateMailObjectMissingFiles(sub, context));
                } catch (MessagingException e) {
                    LOGGER.severe("Exception occured while trying to send "
                            + "mails to students. " + e.getMessage());
                }
				 */

			} else if (!sub.getCheckingResult().getCompilerOutput()
					.isCleanCompile()) {
				/*
				 * if the submission does not compile we send a warning mail to
                 * the student containing all compiler errors,warnings and
                 * infos
				 */

				// Logger shows that Submission(s) does not compile
				LOGGER.info(
				    "Submission of student "+sub.getStudent()
				  + " does not compile.");

				//The following code will create the necessary variables to send an email.
				String receiverEmailAddress = sub.getStudent().getEmail();
				String sendUserName =
				Controller.getController().getConfig().getSenderMailAdress();

				//The following code creates the mailSubject and the mailText of the email
				// which will be received by the student.
				String mailSubject = new String(
						"Ihre Abgabe f\u00FCr Exercise " + getName()
								+ " kompiliert nicht.");
				String mailText = new String(
				    "Hallo " + sub.getStudent().getName() + ",\n" +
				    "Ihre Abgabe kompiliert nicht.\n" +
				    "Probleme sind: \n" + getCompilerError(sub) + "\n\n" +
				    "Sie haben bis " + context.getDeadlineString() +
				    " Zeit, dies zu korrigieren.\n\n" +
				    "Mit freundlichen Gr\u00FC\u00DFen\n\n" + "Grit.");

				//The following code will send an email to the student(s)
				// whose submission does not compile.
				try {
					SendMail sendMail = new SendMail();
					sendMail.sendEMail(sendUserName, receiverEmailAddress,
                        mailSubject, mailText, null);
					LOGGER.fine("Email sent to stud regarding compiler error.");
				} catch (Exception e) {
				  LOGGER.severe("Exception during sending email because of "
				      + "Submissions which does not compile. Reason: " + e.toString());
				}
			}
		}

	}

	/**
	 * Sends a warning mail to all Students without a submission. Only sends
	 * mails 6 and 12 hours before deadline.
	 * 
	 * @param studentsWithoutSubmissions
	 *            List which contains all Students who did not submit anything
	 *            yet.
	 */
	private void notifyStudentsWithoutSubmission(
			List<Student> studentsWithoutSubmissions) {
		long in12hours = System.currentTimeMillis() + (12 * 60 * 60 * 1000);
		long in6hours = System.currentTimeMillis() + (6 * 60 * 60 * 1000);
		long lowerBoundary = context.getDeadline().getTimeInMillis();
		long upperBoundary = context.getPeriod() + lowerBoundary;
		if (((in12hours >= lowerBoundary) && (in12hours < upperBoundary))
				|| ((in6hours >= lowerBoundary) && (in6hours < upperBoundary))) {
			for (Student student : studentsWithoutSubmissions) {
				LOGGER.info("Notify students without submissions.");

            	//The following code will create the necessary
            	//variables to send an email.
				String receiverEmailAddress = student.getEmail();
				String sendUserName =
				Controller.getController().getConfig().getSenderMailAdress();

                //The following code creates the mailSubject and the
                //mailText of the email which will be received by the student.
                String mailSubject = new String(
                    "Sie haben f\u00FCr Exercise " + getName() + 
                    " noch keine L\u00F6sung abgegeben.");
                String mailText = new String(
                    "Hallo " + student.getName() + ",\n" +
                    "Sie haben noch keine L\u00F6sung eingereicht.\n" +
                    "Sie haben bis " + context.getDeadlineString() +
                    " Zeit, dies zu korrigieren.\n\n" +
                    "Mit freundlichen Gr\u00FC\u00DFen\n\n" + "Grit.");

                //The following code will send an email to the student(s)
                //with submissions which does not compile. The catch 
				try {
					SendMail sendMail = new SendMail();
					sendMail.sendEMail(sendUserName, receiverEmailAddress,
                        mailSubject, mailText, null);
                    LOGGER.info(
                        "Email sent to student without submission.");
				} catch (Exception e) {
                    LOGGER.severe(
                      "Exception during sending email to notivy student "
                      + "without submission " + e.toString());
				}

				/*
            	try {
                    SendMailSSL.sendMail(GenerateMailObjectHelper
                            .generateMailObjectNoSubmission(student, context));
                } catch (MessagingException e) {
                    LOGGER.severe("Exception occured while trying to send mails "
                            + "to students. " + e.getMessage());
                }
				 */
			}
		}
	}

	/**
	 * Notifies all students, if existent, with improper mail subject.
	 * 
	 * @param addresses
	 *         addresses who should be notified.
	 */
	public static void notifyStudentsWithInvalidMailSubject(List<String> addresses) {
	  if (addresses.size() > 0) {
	    LOGGER.fine("Notifying students with inproper subject");
	    String sendUserName = 
	        Controller.getController().getConfig().getSenderMailAdress();
	    String mailSubject = new String("Der Betreff Ihrer Abgabe ist ung\u00FCltig");
	    String mailBody = "Sehr geehrte(r) Nutzer(in), \n\n"
	        + "Ich bedauere, Ihnen mitteilen zu m\u00FCssen,"
	        + " dass Ihre Abgabe leider einen f\u00FCr mich nicht verwertbaren"
	        + " Email-Betreff enth\u00E4lt. \u00DCberpr\u00FCfen Sie bitte nochmals insbesondere"
	        + " den Betreff und verschicken Sie gegebenenfalls Ihre Abgabe erneut.\n\n"
	        + "Gleichzeitig m\u00F6chte ich Sie mit dieser (nebenbei automatisch generierten)"
	        + " Nachricht dar\u00FCber informieren, dass Ihre Email im System eingegangen ist."
	        + " Dadurch kommen die Tutoren definitiv (\u00FCber Umwege) an die Abgabe heran,"
	        + " erfreut werden die dar\u00FCber allerdings wohl nicht so sein.\n\n"
	        + "Wir bedanken uns f\u00FCr das von Ihnen entgegegen gebrachte Vertrauen"
	        + " und verbleiben freudlichst. \n\n"
	        + "Der Ihnen (und/oder dem/der Dozent(in))\n"
	        + "treu ergebene grit-Server.";
	      for (String recipient : addresses) {
	        try {
	          SendMail sendMail = new SendMail();
	          sendMail.sendEMail(sendUserName, recipient, mailSubject, mailBody, null);
	          LOGGER.finer("Sent email to "+ recipient
	              +" regarding improper subject.");
	          } catch (Exception e) {
	            LOGGER.severe("Exception during sending email to students"
	                + " with improper subject: " + e.toString());
	          }
	        LOGGER.fine("Sent emails to students with improper subject.");
	      }
	    }
    }

	/**
	 * Notifies the administrator that the exercise has finished processing and
	 * the report is ready for download.
	 */
	private void notifyAdmin() {
      //The following code will create the necessary variables to send an email.
      String adminName = Controller.getController().getConfig().getAdminName();
      // emailAdmin is the receiverEmailAddress
      String emailAdmin = Controller.getController().getConfig().getMailAdmin();
      String sendUserName =
          Controller.getController().getConfig().getSenderMailAdress();

      if (!sendUserName.equals("") || !adminName.equals("")) {
        LOGGER.fine(
            "Notifying admin of finished course: "+context.getCourseName());
        //The following code creates the mailSubject and the mailText of the email
        // which will received by the admin.
        String mailSubject = new String(
            "Exercise " + getName() + " ist abgeschlossen.");
        String mailText =
                  "Sehr geehrte(r) " + adminName + ",\n \n" +
                  "Exercise " + getName() + " hat die Deadline " + 
                  context.getDeadlineString() + " erreicht.\n \n" +
                  "Die Bewertungs-PDF steht zum Download bereit. \n \n" +
                  "Diese E-Mail wurde automatisch von Grit erstellt.";

        // The following code will send an email to the admin as a notification
        // that the exercise has passed the deadline.
        try {
          SendMail sendMail = new SendMail();
          sendMail.sendEMail(sendUserName, emailAdmin, mailSubject, 
              mailText, new File(context.getOutputPath()+"/report.pdf"));
        } catch (Exception e) {
          LOGGER.severe(
              "Exception during sending email to admin: " + e.toString());
          return;
        }
        LOGGER.finer("Notification to admin sent successfully.");
      }
	}

	// --------------------- SUBMISSION HANDLING METHODS ---------------------

	/**
	 * Checks all submissions for plausibility and sets plausible flags in
	 * submissions true if submission is plausible and false if it is not.
	 * 
	 * @param submission
	 *            a submission to check the plausibility upon
	 */
	private void checkPlausibility(Submission submission) {
		Path sourceLocation = submission.getSourceCodeLocation();
        boolean isPlausible =
                SubmissionPlausibilityChecker.checkLocation(sourceLocation,
                    context.getLanguageType());
		submission.setPlausible(isPlausible);
	}

	/**
	 * Compile-checks a submission.
	 * 
	 * @param submission
	 *            the submission to compile
	 * @return the compiler output to use in the {@link CheckingResult}
	 * @throws FileNotFoundException
	 *             from
	 *             {@link CompileChecker#checkProgram(Path, Path, String, List)}
	 * @throws BadCompilerSpecifiedException
	 *             from
	 *             {@link CompileChecker#checkProgram(Path, Path, String, List)}
	 * @throws BadFlagException
	 *             from
	 *             {@link CompileChecker#checkProgram(Path, Path, String, List)}
	 * @throws CompilerOutputFolderExistsException
	 *             from
	 *             {@link CompileChecker#checkProgram(Path, Path, String, List)}
	 */
	private CompilerOutput compileSubmission(Submission submission)
			throws FileNotFoundException, BadCompilerSpecifiedException,
			BadFlagException, CompilerOutputFolderExistsException {
        LOGGER.fine("Compiling code of student " +
            submission.getStudent().getName());

		CompileChecker compiler = context.getCompileChecker();
		Path binPath = context.getBinPath();
		String compilerName = context.getCompilerName();
		List<String> compilerFlags = context.getCompilerFlags();
		return compiler.checkProgram(submission.getSourceCodeLocation(),
				binPath, compilerName, compilerFlags);
	}

	/**
	 * runs tests on single submission if it was compiled clean and returns
	 * TestOutput Object. Returns empty TestOutput Object if it was not
	 * CleanCopiled.
	 * 
	 * @param binaryLocation
	 *            The path to the binaries of the submission to be tested
	 * @param compileResult
	 *            compiler result which holds the information if the code did
	 *            compile or not
	 * @return TestOutput
	 */
	private TestOutput testSubmission(Path binaryLocation,
			CompilerOutput compileResult) {
		Tester tester = context.getTester();
		if (compileResult.isCleanCompile()) {
			try {
				return tester.testSubmission(binaryLocation);
			} catch (ClassNotFoundException | IOException e) {
				LOGGER.severe("Exception while testing Submisson. "
						+ e.toString());
			} catch (NullPointerException n) {
            	// if tester is not properly selected during exercise,
                // then it is null by default. Thats why it is cached here.
				List<Result> results = new ArrayList<>();
				return new TestOutput(results, true);
			}

		} else if (!compileResult.isCleanCompile()) {
			List<Result> results = new ArrayList<>();
			return new TestOutput(results, false);
		}
		return null;
	}

	/**
	 * Shuts the taskPool down in order to reboot the system.
	 */
	public void terminate() {
		m_taskPool.shutdownNow();
	}

}
