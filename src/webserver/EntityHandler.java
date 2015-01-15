package webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import preprocess.ConnectionType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import entities.Controller;
import entities.Exercise;
import entities.LanguageType;

/**
 * An entity handler will present CRUD actions for the respective entity that
 * are used by the website interface.
 *
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */
public abstract class EntityHandler extends AbstractHandler {

    /**
     * A GSON instance for JSON parsing.
     */
    protected static final Gson GSON = new GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation().create();

    /**
     * The grit-wide logger.
     */
    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * This is used for requests that contain a submitted file.
     */
    private static final MultipartConfigElement MULTI_PART_CONFIG =
            new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(
            "HH:mm");

    /**
     * The controller instance will be referenced here.
     */
    protected Controller m_controller;

    /**
     * Constructor for {@link EntityHandler}. It calls the constructor of
     * {@link AbstractHandler} and gets the controller instance.
     */
    public EntityHandler() {
        super();
        m_controller = Controller.getController();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.server.Handler#handle(java.lang.String,
     * org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void handle(
            String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        /*
         * If this request contains a submitted file, use the multi-part
         * configuration.
         */
        if ((request.getContentType() != null)
                && request.getContentType().startsWith("multipart/form-data")) {
            baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT,
                    MULTI_PART_CONFIG);
        }

        /*
         * Set the content type and response status for default entity
         * handling.
         */
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        /*
         * Select and run the action requested. If an error occurs the error is
         * logged and written to the response and the server response status is
         * set respectively.
         */
        try {
            response.getWriter().println(doAction(target, request));
        } catch (final BadRequestException e) {
            final String message = e.getMessage();
            LOGGER.severe(message);
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(message);
        } catch (final InternalActionErrorException e) {
            final String message = e.getMessage();
            LOGGER.severe(message);
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(message);
        } catch (final Exception e) {
            final String message =
                    "An unexpected error occured:\n"
                            + e.getClass().getSimpleName() + ":\n"
                            + e.getMessage();
            LOGGER.severe(message);
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(message);
        }

        /* Tell JETTY that the request has been handled. */
        baseRequest.setHandled(true);
    }

    /**
     * Selects the appropriate action depending on the target and runs it. It
     * then returns the output of the action (normally JSON). The return can be
     * directly written to the response text.
     *
     * @param target
     *            the target as in
     *            {@link Handler#handle(String, Request, HttpServletRequest, HttpServletResponse)}
     * @param request
     *            the request as in
     *            {@link Handler#handle(String, Request, HttpServletRequest, HttpServletResponse)}
     * @return the output of the action
     * @throws BadRequestException
     *             if something goes wrong due to a bad request or the
     *             requested action does not exist
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    protected abstract String doAction(
            String target, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException;

    // --------------------- PARAMETER HELPERS ---------------------

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * name.
     *
     * @param parameter
     *            the parameter to parse to a name
     * @param nameType
     *            defines the type of the name: sshusername, username, name
     * @return a valid name
     * @throws BadRequestException
     *             if the parameter does not represent a valid name
     */
    protected String parseName(String parameter, String nameType)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("The passed " + nameType
                    + " is null.");
        }
        final String name = parameter.trim();
        if (!name.matches(".+")) {
            throw new BadRequestException("The passed " + nameType
                    + " is invalid.");
        }
        return name;
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * {@link LanguageType}.
     *
     * @param parameter
     *            the parameter to parse to a {@link LanguageType}
     * @return a valid {@link LanguageType}
     * @throws BadRequestException
     *             if the parameter does not represent a valid language type
     */
    protected LanguageType parseLanguageType(String parameter)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("Passed language type is null.");
        }
        if (!parameter.matches("[A-Z]+")) {
            throw new BadRequestException(
                    "The passed language type does not exist.");
        }
        try {
            return LanguageType.valueOf(parameter);
        } catch (final NullPointerException e) {
            throw new BadRequestException(
                    "The passed language type does not exist.");
        }
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * {@link ConnectionType}.
     *
     * @param parameter
     *            the parameter to parse to a {@link ConnectionType}
     * @return a valid {@link ConnectionType}
     * @throws BadRequestException
     *             if the parameter does not represent a valid connection type
     */
    protected ConnectionType parseConnectionType(String parameter)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("Passed connection type is null.");
        }
        if (!parameter.matches("[A-Z]+")) {
            throw new BadRequestException(
                    "The passed connection type does not exist.");
        }
        try {
            return ConnectionType.valueOf(parameter);
        } catch (final NullPointerException e) {
            throw new BadRequestException(
                    "The passed connection type does not exist.");
        }
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * location (URL).
     *
     * @param parameter
     *            the parameter to parse to a location
     * @return a valid location
     * @throws BadRequestException
     *             if the parameter does not represent a valid location
     */
    protected String parseLocation(String parameter)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("The passed location is null.");
        }
        final String location = parameter.trim();
        if (!location.matches(".+")) {
            throw new BadRequestException("The passed location is invalid.");
        }
        return location;
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * date-time ({@link Date}). The parameter has to be formatted like in
     * {@link EntityHandler#DATE_TIME_FORMAT}
     *
     * @param parameter
     *            the parameter to parse to a date-time
     * @return a valid {@link Date}
     * @throws BadRequestException
     *             if the parameter does not represent a valid date
     */
    protected Date parseDateTime(String parameter) throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("Passed date is null.");
        }
        try {
            return DATE_TIME_FORMAT.parse(parameter);
        } catch (final ParseException e) {
            throw new BadRequestException(e.getMessage()
                    + "\nA date has to be formatted like this: \""
                    + DATE_TIME_FORMAT.toPattern() + "\".");
        }
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * time period. The parameter has to be formatted like in
     * {@link EntityHandler#TIME_FORMAT}
     *
     * @param parameter
     *            the parameter to parse to a period
     * @return a valid duration in milliseconds
     * @throws BadRequestException
     *             if the parameter does not represent a valid period
     */
    protected long parseTimePeriod(String parameter)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("Passed period is null.");
        }
        if (!parameter.matches("\\d\\d:\\d\\d")) {
            throw new BadRequestException("Could not parse duration: "
                    + "A time has to be formatted like this: \""
                    + TIME_FORMAT.toPattern() + "\".");
        }
        final String[] time = parameter.split(":");
        final long period =
                ((Integer.parseInt(time[0]) * 60) + Integer.parseInt(time[1])) * 60 * 1000;
        return period;
    }

    /**
     * This is a method for a convenient usage of parseId(parameter,
     * "connection").
     *
     * @param parameter
     *            the parameter to parse to a connection ID
     * @return a valid ID
     * @throws BadRequestException
     *             if the parameter does not represent a valid ID
     */
    protected int parseConnectionId(String parameter)
            throws BadRequestException {
        return parseId(parameter, "connection");
    }

    /**
     * This is a method for a convenient usage of parseId(parameter, "course").
     *
     * @param parameter
     *            the parameter to parse to a course ID
     * @return a valid ID
     * @throws BadRequestException
     *             if the parameter does not represent a valid ID
     */
    protected int parseCourseId(String parameter) throws BadRequestException {
        return parseId(parameter, "course");
    }

    /**
     * This is a method for a convenient usage of parseId(parameter,
     * "exercise").
     *
     * @param parameter
     *            the parameter to parse to an exercise ID
     * @return a valid ID
     * @throws BadRequestException
     *             if the parameter does not represent a valid ID
     */
    protected int parseExerciseId(String parameter) throws BadRequestException {
        return parseId(parameter, "exercise");
    }

    /**
     * Parses a given parameter from a request (or any other string) to a valid
     * ID.
     *
     * @param parameter
     *            the parameter to parse to an ID
     * @param entityName
     *            a string representing the name of an entity class (e.g.
     *            "exercise" for {@link Exercise}), which is here solely used
     *            for the error message
     * @return a valid ID
     * @throws BadRequestException
     *             if the parameter does not represent a valid ID
     */
    protected int parseId(String parameter, String entityName)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("The passed " + entityName
                    + " ID is null.");
        }
        if (!parameter.matches("\\d+")) {
            throw new BadRequestException("The passed " + entityName
                    + " ID is no number.");
        }
        return Integer.parseInt(parameter);
    }

    // --------------------- ENTITY MANIPULATION HELPERS ---------------------

    /**
     * This method writes a submitted file of a multipart request to its
     * destination.
     *
     * @param part
     *            the part with the submitted file in it
     * @param outputDirectory
     *            the directory to put the file into
     * @throws BadRequestException
     *             if the submitted file is not valid
     * @throws InternalActionErrorException
     *             if the file could not be written or the path
     */
    protected void writeSubmittedFile(Part part, Path outputDirectory)
            throws BadRequestException, InternalActionErrorException {
        final String submittedFileName = part.getSubmittedFileName();
        if ((submittedFileName == null) || ("".equals(submittedFileName))) {
            throw new BadRequestException("No file submitted!");
        }
        try {
            // make sure the temp directory exists
            final Path tempPath = Paths.get("wdir", "temp");
            Files.createDirectories(tempPath);

            // conpy the file to the temp directory
            final InputStream testfileInputStream = part.getInputStream();
            final Path tempFile = tempPath.resolve(submittedFileName);
            final String fileExtension =
                    FilenameUtils.getExtension(submittedFileName);
            Files.copy(testfileInputStream, tempFile,
                    StandardCopyOption.REPLACE_EXISTING);

            final Path outFilePath;

            // java files have to be in the directory matching their
            // qualified name
            if (fileExtension.equals("java")) {
                final String qualifiedName =
                        getQualifiedNameFromFile(tempFile);
                String subDir =
                        StringUtils.replaceChars(qualifiedName, '.', '/');
                subDir = subDir + "." + fileExtension;
                outFilePath = outputDirectory.resolve(subDir);
            } else {
                // not needed in other languages
                outFilePath = outputDirectory.resolve(submittedFileName);
            }

            Files.createDirectories(outFilePath.getParent());

            // move the file to its final position
            Files.move(tempFile, outFilePath,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (InvalidPathException | IOException e) {
            throw new InternalActionErrorException(
                    "Error while trying to store submitted file:\n"
                            + submittedFileName);
        }
    }

    /**
     * This method writes a submitted file of a multipart request to its
     * destination. If the file does not exist, nothing happens.
     *
     * @param part
     *            the part with the submitted file in it
     * @param outputDirectory
     *            the directory to put the file into
     * @throws BadRequestException
     *             if the submitted file is not valid
     * @throws InternalActionErrorException
     *             if the file could not be written or the path
     */
    protected void optionalWriteSubmittedFile(Part part, Path outputDirectory)
            throws BadRequestException, InternalActionErrorException {
        if (part == null) {
            return;
        }
        final String submittedFileName = part.getSubmittedFileName();
        if ((submittedFileName != null) && !("".equals(submittedFileName))) {
            writeSubmittedFile(part, outputDirectory);
        }
    }

    /**
     * Iterates over a java source file, and identifies the fully qualified
     * name of the class from the package declaration in the source.
     *
     * @param sourceFile
     *            a java source file
     * @return the fully qualified name of the class
     * @throws IOException
     *             if the source file can not be read from.
     */
    private String getQualifiedNameFromFile(Path sourceFile)
            throws IOException {
        final String packageRegex = "package\\s[^,;]+;";
        LineIterator it;
        String result = "";
        it = FileUtils.lineIterator(sourceFile.toFile(), "UTF-8");

        // look for the line identifying the package
        while (it.hasNext()) {
            final String line = it.nextLine();
            if (line.matches(packageRegex)) {
                result = line;
                // strip not needed elements (the word package)
                result = result.substring(8, result.length() - 1);
                it.close();
                result = result + ".";
                break;
            }
        }
        it.close();
        // append the classname
        return result + FilenameUtils.getBaseName(sourceFile.toString());
    }
}
