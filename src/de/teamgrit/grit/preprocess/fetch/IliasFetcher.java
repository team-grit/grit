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

package de.teamgrit.grit.preprocess.fetch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import de.teamgrit.grit.preprocess.Connection;
import de.teamgrit.grit.preprocess.sql_connector.SqlConnector;
import de.teamgrit.grit.preprocess.tokenize.Submission;

/**
 * This class implements an ILIASFetcher, it  is capable of fetching {@link Submission} from an
 * ILIAS server.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */

public class IliasFetcher {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    // Following query fetches the necessary data we need to fill
    // the submission objects.
    // The output looks like this: (with example data)
    //
    // exc_title | title | email | firstname | lastname | ts | filename
    // ----------|--------|-------|-----------|----------|-------|---------
    // PK1 | Blatt 1| ..@.. | hansi | hirsch | 2014..| /.../...
    // PK1 | Blatt 1| ..@.. | hansi | hirsch | null | null
    //
    // If a student did not hand in a submission the corresponding fields
    // are null.
    // Explanation of the query:
    // First we look at the used tables to see what data they contain
    // exc_assignment: contains all assignments(name etc.) and a mapping
    // to which exercise it belongs to
    // exc_mem_ass_status ES: contains information if a student did or did
    // not hand in a submission. also maps assignments to students
    // usr_data U: contains information about students
    // exc_returned ER: contains every single submission that was handed in
    // as well as the submission time and the filepath to the
    // submitted files
    // object_data OD: contains all kinds of information. Also contains
    // the exercise names and course names
    private static final String DATA_QUERY_TEMPLATE = "SELECT  EXC.title AS exc_title, E.title , U.email , "
            + "U.firstname , U.lastname , ER.ts , ER.filename"
            // first we join E,ES and U in order to map submissions to
            // students
            + " FROM (((exc_assignment E JOIN exc_mem_ass_status ES"
            + " ON E.id = ES.ass_id) JOIN usr_data U"
            /*
             * then we do a left join with ER. ER only contains submissions
             * that were handed in. So when we do a left join the filepath
             * field of a student who did not submit anything will have the
             * value null. This happens because the join of E,ES and U has a
             * row for every student for every submission.
             */
            + " ON ES.usr_id = U.usr_id) LEFT JOIN exc_returned ER"
            + " ON ER.user_id = U.usr_id AND ER.ass_id = E.id) JOIN"
            // now we join with O because O contains the names of the
            // exercises
            + " (SELECT OD.type , OD.title , OD.obj_id"
            + " FROM object_data OD) EXC" + " ON E.exc_id = EXC.obj_id"
            /*
             * here we can select which exercise and submission we want to look
             * at
             */
            + " WHERE EXC.title = '%s' AND E.title = '%s'";

    /**
     * Downloads submissions.
     * 
     * @param courseName
     *            the name of the course
     * @param exerciseName
     *            the name of the exercise
     * @param connection
     *            the connection to the remote
     * @param targetDirectory
     *            the directory to download into
     * 
     * @return IliasFetchingResult
     * @throws SubmissionFetchingException
     *             if the fetching fails
     */
    public static List<StudentRepoData> fetchSubmissions(Connection connection,
            String courseName, String exerciseName, Path targetDirectory)
            throws SubmissionFetchingException {

        String iliasLocation = connection.getLocation();
        String sqlUsername = connection.getUsername();
        String sqlPassword = connection.getPassword();
        String sshUserName = connection.getSshUsername();
        Path sshKeyFileLocation = connection.getSshKeyFileLocation();

        // location needs to look like
        // jdbc:mysql://[some url or IP to host]/ilias
        // jdbc:mysql:// is prepended to indicate a DB connection, ilias is the
        // table we'll read from
        String dbConnectionString = "jdbc:mysql://" + iliasLocation + "/ilias";

        SqlConnector sqlConnection = new SqlConnector(dbConnectionString,
                sqlUsername, sqlPassword);
        if (!sqlConnection.establishConnection()) {
            LOGGER.severe("Database connection could not be established with "
                    + "parameters: \"" + dbConnectionString + "\", \""
                    + sqlUsername + "\", \"" + sqlPassword);
            throw new SubmissionFetchingException(
                    "Error while fetching from ILIAS Database.");
        }
        String query = String.format(DATA_QUERY_TEMPLATE, courseName,
                exerciseName);
        ResultSet submissionData = sqlConnection.getDataFromDB(query);

        if (submissionData == null) {
            throw new SubmissionFetchingException(
                    "Bad query or insufficient permissions on database.");
        }

        List<String> files = new LinkedList<>();
        List<StudentRepoData> studentData = new ArrayList<>();

        try {
            LOGGER.info("Generating studentRepoData objects "
                    + "and collecting files for download");
            while (submissionData.next()) {
                String submissionFilename = submissionData
                        .getString("filename");
                files.add(submissionFilename);

                StudentRepoData s = new StudentRepoData(
                        submissionData.getString("firstname"),
                        submissionData.getString("lastname"),
                        submissionData.getString("email"),
                        submissionData.getTimestamp("ts"),
                        submissionData.getString("title"), submissionFilename);
                studentData.add(s);

                Path newDir = targetDirectory.resolve(Paths.get(submissionData
                        .getString("email")));
                if (!Files.exists(newDir)) {
                    Files.createDirectory(newDir);
                }
                if (!Files.exists(newDir.resolve(submissionData
                        .getString("title")))) {
                    Files.createDirectory(newDir.resolve(submissionData
                            .getString("title")));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error while fetching from ILIAS Databse: "
                    + e.getMessage());
            throw new SubmissionFetchingException(
                    "Error while fetching from ILIAS Database: "
                            + e.getMessage());
        } catch (IOException e) {
            LOGGER.severe("IOException whille creating submission folders: "
                    + e.getMessage());
            throw new SubmissionFetchingException(
                    "IOException while creating submission folders: "
                            + e.getMessage());
        }

        // download into target directory.
        getFilesFromServer(files, targetDirectory, iliasLocation, sshUserName,
                sshKeyFileLocation);

        // now copy files to proper folder.
        copyDownloadsToNamedFolders(studentData, targetDirectory);

        LOGGER.info("Done Fetching from ILIAS server.");

        return studentData;
    }

    /**
     * Downloads the given list of files via scp from the ILIAS server. All
     * previous contents of the the target directory are deleted
     * 
     * @param files
     *            the collection of files that has to be downloaded
     * @param targetDirectory
     *            the directory that the files are
     * @param sshLoginName
     *            the username for the sshkey
     * @param sshKeyFileLocation
     *            the storage location of the keyfile
     * @param iliasLocation
     *            the location of the
     * @return true, if download was successfull, and false, if download was
     *         not successfull
     */
    private static boolean getFilesFromServer(List<String> files,
            Path targetDirectory, String iliasLocation, String sshLoginName,
            Path sshKeyFileLocation) {

        if ((files == null) || (files.size() == 0)) {
            return false;
        }

        // new we generate the pretty long command we can execute
        List<String> scpCommand = createScpCommand(files, targetDirectory,
                iliasLocation, sshLoginName, sshKeyFileLocation);

        // build process: construct command and set working directory.
        Process scpProcess = null;
        ProcessBuilder scpProcessBuilder = new ProcessBuilder(scpCommand);
        scpProcessBuilder.directory(targetDirectory.toFile());

        try {
            scpProcess = scpProcessBuilder.start();
        } catch (IOException e) {
            LOGGER.severe("Error while starting scp: " + e.getMessage());
        }

        // wait until it's done.
        try {
            scpProcess.waitFor();
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while waiting for SCP. "
                    + "Cannot guarantee clean command run!" + e.getMessage());
            return false;
        }

        // get output from ssh
        InputStream scpErrorStream = scpProcess.getErrorStream();
        InputStreamReader scpStreamReader = new InputStreamReader(
                scpErrorStream);
        BufferedReader scpOutputBuffer = new BufferedReader(scpStreamReader);
        String line;

        try {
            while ((line = scpOutputBuffer.readLine()) != null) {
                LOGGER.info("SSH:: " + line);
            }
        } catch (IOException e) {
            LOGGER.severe("Error while reading from scp stream."
                    + e.getMessage());
        }

        // check return value
        switch (scpProcess.exitValue()) {
        case 0:
            // all is well :)
            return true;
        case 1:
            LOGGER.severe("SSH generic error.");
            return false;
        case 2:
            LOGGER.severe("SSH Remote host connection failure");
            return false;
        default:
            LOGGER.severe("Unknown SSH/SCP return code: "
                    + scpProcess.exitValue()
                    + " (Check the scp manual or RFC4253");
            return false;
        }

    }

    /**
     * Creates the SCP command.
     * 
     * @param files
     *            the files to download
     * @param targetDirectory
     *            the directory to download to
     * @param iliasLocation
     *            the location of the remote
     * @param sshLoginName
     *            the loginname for the ssh authentication
     * @param sshKeyFileLocation
     *            the location of the sshkey
     * @return the scp command to download all the needed files
     */
    private static List<String> createScpCommand(List<String> files,
            Path targetDirectory, String iliasLocation, String sshLoginName,
            Path sshKeyFileLocation) {
        List<String> scpCommand = new LinkedList<>();
        scpCommand.add("scp");
        scpCommand.add("-v");

        // enable compression
        scpCommand.add("-C");

        // batch mode (no prompt for pws etc)
        scpCommand.add("-B");

        // use this to offer only our public key and not spam the server with
        // multiple other keys which are also installed.
        scpCommand.add("-o");
        scpCommand.add("IdentitiesOnly=yes");

        // location of keyfile
        scpCommand.add("-i");
        scpCommand.add(sshKeyFileLocation.toAbsolutePath().toString());

        // we can get multiple files a,b,c as user foo from host bar by passing
        // scp foo@bar:\{a,b,c\}

        StringBuilder paths = new StringBuilder();
        paths.append(sshLoginName + "@" + iliasLocation + ":");

        for (String file : files) {
            // since some students might submit nothing, we have to watch out
            // for nulls here.
            if (!(file == null) && !(file.matches("null"))) {
                paths.append(file + " ");
            }
        }

        scpCommand.add(paths.toString());
        scpCommand.add(targetDirectory.toAbsolutePath().toString());
        return scpCommand;
    }

    /**
     * After files have been downloaded this method will organize them into
     * folgers studentmail/exercise. Archives will be unpacked in the process.
     * 
     * @param studentData
     *            the list containing the studentdata
     * @param targetDirectory
     *            the target directory
     * @throws SubmissionFetchingException
     *             encapsulating fetching errors.
     */
    private static void copyDownloadsToNamedFolders(
            List<StudentRepoData> studentData, Path targetDirectory)
            throws SubmissionFetchingException {
        for (StudentRepoData srd : studentData) {
            String submissionFilename = null;
            try {
                if (srd.getSubmissionPath() != null) {
                    URI submissionURI = new URI(srd.getSubmissionPath());
                    String path = submissionURI.getPath();
                    submissionFilename = path
                            .substring(path.lastIndexOf('/') + 1);
                } else {
                    /*
                     * if getSubmissionPath returns null the student did not
                     * upload a submission so we process the next student
                     */
                    continue;
                }

            } catch (URISyntaxException e) {
                throw new SubmissionFetchingException(
                        "Error while parsing Filename from: "
                                + srd.getSubmissionPath() + " -> "
                                + e.getMessage());
            }

            if (srd.getExercise() != null) {
                // just copy, unzipping is done by the tokenizer.
                try {
                    Files.move(
                            targetDirectory.resolve(submissionFilename),
                            targetDirectory.resolve(srd.getEmail())
                                    .resolve(srd.getExercise())
                                    .resolve(submissionFilename),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.severe("Error while copying " + submissionFilename
                            + " : " + e.getMessage());
                }
            }
        }
    }
}
