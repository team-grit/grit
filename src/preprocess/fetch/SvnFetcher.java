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

package preprocess.fetch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import preprocess.Connection;

/**
 * This SVNFetcher is able  to fetch
 * submissions from a SVN repository.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */
public final class SvnFetcher {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * This is a static class, so no instances required.
     */
    private SvnFetcher() {

    }

    /**
     * Fetches from a remote svn repository. All login information and the
     * location of the svn repository are saved in the {@link Connection}. The
     * checkout will be placed in the targetDirectory.
     *
     * @param connection
     *            contains login information and address of remote svn
     * @param targetDirectory
     *            directory in which the checkout will be placed
     * @return the path to the root of the svn repository, null if checkout
     *         failed failed
     * @throws SubmissionFetchingException
     *             if the fetching fails
     */
    public static Path fetchSubmissions(
            final Connection connection, final Path targetDirectory)
            throws SubmissionFetchingException {
        if (!checkConnectionToRemoteSVN(connection.getLocation())) {
            throw new SubmissionFetchingException(
                    "No connection to remote SVN.");
        }
        if (!isDataSourceInitialized(targetDirectory)) {
            initializeDataSource(connection, targetDirectory);
        }

        Path newTargetDirectory =
                updateDirectoryPath(connection.getLocation(), targetDirectory);

        LOGGER.info("Trying to update local svn repository.");

        SVNResultData svnResult = null;
        try {
            List<String> svnCommand = new LinkedList<>();
            svnCommand.add("svn");
            svnCommand.add("update");
            svnResult =
                    runSVNCommand(connection, svnCommand, newTargetDirectory);
        } catch (IOException e) {
            throw new SubmissionFetchingException(
                    "SVN Update failed: Could not start SVN or could "
                            + "not read from output stream." + e.getMessage());
        }

        if (svnResult != null) {
            // Any SVN return value != 0 implies an error and the fetch wasn't
            // clean. Hence we bundle the output into the exception and throw.
            if (svnResult.getReturnValue() != 0) {
                String svnOutForException = "";
                for (String message : svnResult.getSvnOutputLines()) {
                    message = svnOutForException.concat(message + "\n");
                }
                throw new SubmissionFetchingException(svnOutForException);
            }

        } else {
            LOGGER.severe("BUG: Failed to acquire SVN result object.");
            throw new SubmissionFetchingException(
                    "Failed to acquire SVN result object, "
                            + "should never happen.");
        }

        LOGGER.info("Done fetching from SVN Repository.");

        return newTargetDirectory;
    }

    /**
     * checks if GRIT can connect to the remote SVN server.
     *
     * @param svnRepoAdress
     *            the adress of the remote
     * @return true if GRIT can connect to the SVN.
     * @throws SubmissionFetchingException
     *             If the URL to the SVN is invalid.
     */
    private static boolean checkConnectionToRemoteSVN(String svnRepoAdress)
            throws SubmissionFetchingException {
        try {
            int connectionTimeoutMillis = 10000;
            if (!svnRepoAdress.startsWith("file://")) {
                URL svnRepoLocation = new URL(svnRepoAdress);
                URLConnection svnRepoConnection =
                        svnRepoLocation.openConnection();
                svnRepoConnection.setConnectTimeout(connectionTimeoutMillis);
                svnRepoConnection.connect();
            }
            return true;
        } catch (MalformedURLException e) {
            throw new SubmissionFetchingException(
                    "Bad URL specified. Can not connect to this URL: "
                            + svnRepoAdress + e.getMessage());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Initializes the workspace by performing a checkout in a specified
     * directory.
     *
     * @param connectionData
     *            holds login information an remote location of the svn
     *            repository
     * @param targetDirectory
     *            the local directory in which the checkout will be placed
     * @return true if checkout was successful, false otherwise
     * @throws SubmissionFetchingException
     *             if the fetching fails
     */
    private static boolean initializeDataSource(
            Connection connectionData, Path targetDirectory)
            throws SubmissionFetchingException {
        // nuke previous contents, so we can be sure that we have a clean
        // state.
        try {
            FileUtils.deleteDirectory(targetDirectory.toFile());
            Files.createDirectories(targetDirectory);
        } catch (IOException e) {
            LOGGER.severe("Could not clean data source: "
                    + targetDirectory.toAbsolutePath().toString() + " -> "
                    + e.getMessage());
            return false;
        }

        if (!checkConnectionToRemoteSVN(connectionData.getLocation())) {
            return false;
        }

        // now tell svn to checkout.
        try {
            List<String> svnCommand = new LinkedList<>();
            svnCommand.add("svn");
            svnCommand.add("checkout");
            svnCommand.add(connectionData.getLocation());

            SVNResultData svnResult =
                    runSVNCommand(connectionData, svnCommand, targetDirectory);

            if (svnResult != null) {
                LOGGER.info("Successful SVN pull from "
                        + connectionData.getLocation());
            }

            // Any SVN return value != 0 implies an error and the fetch wasn't
            // clean. Hence we bundle the output into the exception and throw.
            if (svnResult.getReturnValue() != 0) {
                String svnOutForException = "";
                for (String message : svnResult.getSvnOutputLines()) {
                    svnOutForException =
                            svnOutForException.concat(message + "\n");
                }
                throw new SubmissionFetchingException(svnOutForException);
            }
        } catch (IOException e) {
            LOGGER.warning("unable to check out repository: "
                    + connectionData.getLocation());
            return false;
        }

        LOGGER.config("Checked out, moving internal repository path to "
                + targetDirectory.toString());

        return true;
    }

    /**
     * Updates the directory path for remote svn repos.
     *
     * @param location
     *            the adress of the repo
     * @param oldPath
     *            the current path
     * @return an updated path
     */
    private static Path updateDirectoryPath(String location, Path oldPath) {
        Path targetDirectory = Paths.get(oldPath.toAbsolutePath().toString());
        // check whether the svn repo is given via a url or is given via a path
        if (!location.startsWith("file://")) {

            // We need to get the name of the checked out folder / repo.
            int occurences = StringUtils.countMatches(location, "/");
            int index = StringUtils.ordinalIndexOf(location, "/", occurences);
            String temp = location.substring(index + 1);

            // stitch the last part of the hyperlink to the targetDirectory to
            // receive the structure
            targetDirectory = Paths.get(targetDirectory.toString(), temp);
        } else {
            targetDirectory =
                    targetDirectory.resolve(Paths.get(location).getFileName());
        }
        return targetDirectory;
    }

    /**
     * runSVNCommand runs an svn command in the specified directory.
     *
     * @param connection
     *            the connection storing the connection infos for the remote
     * @param svnCommand
     *            is the command to be executed. Each part of the command must
     *            be a string element. For example: "svn" "checkout"
     *            "file://some/path"
     * @param workingDir
     *            where the svn command should be executed (must be a
     *            repository).
     * @return A list of all output lines.
     * @throws IOException
     *             Thrown when process can't be started or an error occurs
     *             while reading its output
     */
    private static SVNResultData runSVNCommand(
            Connection connection, List<String> svnCommand, Path workingDir)
            throws IOException {

        // add boilerplate to command
        svnCommand.add("--non-interactive");
        svnCommand.add("--no-auth-cache");
        svnCommand.add("--force");

        if ((connection.getUsername() != null)
                && !connection.getUsername().isEmpty()) {
            svnCommand.add("--username");
            svnCommand.add(connection.getUsername());
        }

        if ((connection.getPassword() != null)
                && !connection.getPassword().isEmpty()) {
            svnCommand.add("--password");
            svnCommand.add(connection.getPassword());
        }

        // build process: construct command and set working directory.
        Process svnProcess = null;
        ProcessBuilder svnProcessBuilder = new ProcessBuilder(svnCommand);
        svnProcessBuilder.directory(workingDir.toFile());

        svnProcess = svnProcessBuilder.start();
        try {
            svnProcess.waitFor();
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while waiting for SVN. "
                    + "Cannot guarantee clean command run!");
            return null;
        }

        InputStream svnOutputStream = svnProcess.getInputStream();
        InputStreamReader svnStreamReader =
                new InputStreamReader(svnOutputStream);
        BufferedReader svnOutputBuffer = new BufferedReader(svnStreamReader);
        String line;

        List<String> svnOutputLines = new LinkedList<>();

        while ((line = svnOutputBuffer.readLine()) != null) {
            svnOutputLines.add(line);
        }

        return new SVNResultData(svnProcess.exitValue(), svnOutputLines);
    }

    /**
     * Checks whether the specified directory already contains a svn
     * repository.
     *
     * @param targetDirectory
     *            the directory which will contain a repository
     * @return true if there is a repository, false otherwise
     */
    private static boolean isDataSourceInitialized(Path targetDirectory) {
        return (targetDirectory != null)
                && Files.exists(targetDirectory.resolve(".svn/"),
                        LinkOption.NOFOLLOW_LINKS);
    }
}
