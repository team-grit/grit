package preprocess;

import java.nio.file.Path;
import java.util.Date;

import preprocess.fetch.SubmissionFetchingException;

/**
 * Selects the proper {@link Preprocessor} for a {@link Connection}.
 * 
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */
public abstract class Preprocessors {

    /**
     * Returns a {@link PreprocessingResult} that contains all fetched
     * submissions and a list of Students that did not turn in a submission.
     * 
     * @param targetDirectory
     *            the directory to download into
     * @param connection
     *            the connection to use
     * @param courseName
     *            the name of the course
     * @param exerciseName
     *            the name of the exercise
     * @param fileRegex
     *            the regex matching valid source files
     * @param archiveRegex
     *            the regex matching valid archive files
     * @return {@link PreprocessingResult} that contains all fetched
     *         submissions and a list of Students that did not turn in a
     *         submission.
     * @throws SubmissionFetchingException
     *             if the fetching fails
     */
    public static PreprocessingResult preprocess(Connection connection,
            Date startTime, Date deadline, Path targetDirectory,
            String courseName, String exerciseName, String fileRegex,
            String archiveRegex) throws SubmissionFetchingException {
        ConnectionType connectionType = connection.getConnectionType();

        PreprocessingResult result = null;
        switch (connectionType) {
        case ILIAS:
            result = IliasPreprocessor.preprocess(connection, targetDirectory,
                    courseName, exerciseName, fileRegex, archiveRegex);
            break;
        case SVN:
            result = SvnPreprocessor.preprocess(connection, targetDirectory,
                    fileRegex, archiveRegex);
            break;
        case MAIL:
            result = MailPreprocessor.preprocess(connection, targetDirectory,
                    fileRegex, archiveRegex, startTime, deadline, courseName,
                    exerciseName);
            break;
        default:
            throw new SubmissionFetchingException("Invalid connection type: "
                    + connectionType);

        }
        return result;
    }
}
