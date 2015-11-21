package de.teamgrit.grit.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import de.teamgrit.grit.preprocess.Connection;
import de.teamgrit.grit.preprocess.ConnectionType;
import de.teamgrit.grit.preprocess.tokenize.InvalidStructureException;
import de.teamgrit.grit.util.config.NoProperParameterException;

/**
 * This class provides methods to save the state of GRIT to the disk and read
 * from it.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */
public final class State {

    private XMLConfiguration m_state;
    private File m_stateFile;

    /**
     * Loads the state from the specified File.
     * 
     * @param file
     *            the file the config is stored in
     * @throws ConfigurationException
     *             if the config file is malformed
     * @throws FileNotFoundException
     *             if the specified file can't be found
     */
    public State(File file) throws ConfigurationException,
            FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        m_state = new XMLConfiguration(file);
        m_stateFile = file;

        // set xpath engine for more powerful queries
        m_state.setExpressionEngine(new XPathExpressionEngine());
    }

    /**
     * Loads all courses from the state file and starts all containing
     * exercises. This needs to be called after restoring the connections, as
     * the courses depend on them!
     * 
     * @return a list containing all stored courses
     */
    public Map<Integer, Course> restoreCourses() throws ConfigurationException {

        Map<Integer, Course> courses = new HashMap<>();
        List<HierarchicalConfiguration> courseNodes = m_state
                .configurationsAt("/courses/*");

        // find all courses
        for (HierarchicalConfiguration courseNode : courseNodes) {

            // restore the course
            int courseId = courseNode.getInt("@id");
            String courseName = courseNode.getString("@name");
            Course course = new Course(courseId, courseName);

            // restore the exercises
            Map<Integer, Exercise> exerciseMap = new HashMap<>();
            List<HierarchicalConfiguration> exercises = courseNode
                    .configurationsAt("exercises/exercise");
            for (HierarchicalConfiguration exerciseNode : exercises) {
                String name = exerciseNode.getString("@name");
                int exerciseId = exerciseNode.getInt("@id");

                String string = exerciseNode.getString("@language");
                LanguageType languageType = LanguageType.valueOf(string);

                Date startTime = new Date(exerciseNode.getLong("/times/@start"));
                Date deadline = new Date(
                        exerciseNode.getLong("/times/@deadline"));
                long period = exerciseNode.getLong("/times/@period");

                List<String> flags = new ArrayList<>();
                ExerciseMetadata metadata = new ExerciseMetadata(name,
                        languageType, startTime, deadline, period, flags);

                // get the connection from the controller
                int connectionId = exerciseNode.getInt("connection/@id");

                try {
                    ExerciseContext context = ExerciseContextFactory
                            .getExerciseContext(exerciseId, connectionId,
                                    metadata, courseId, courseName);

                    Exercise exercise = new Exercise(exerciseId, context);

                    exerciseMap.put(exerciseId, exercise);

                } catch (WrongDateException | NoProperParameterException e) {
                    throw new ConfigurationException(
                            "Couldn't read the course exercise: " + exerciseId
                                    + " : " + name + " when restoring course: "
                                    + courseId + " : " + courseName, e);
                }
            }
            course.setExercises(exerciseMap);
            courses.put(courseId, course);
        }

        return courses;
    }

    /**
     * Saves a course to the state.
     * 
     * @param course
     *            the course to be saved
     * @throws ConfigurationException
     *             If the saving fails
     */
    public void addCourse(Course course) throws ConfigurationException {
        final String prefix = "courses/course[last()]";
        try {
            m_state.addProperty("courses course", "");
        } catch (IllegalArgumentException e) {
            m_state.addProperty("courses course", "");

        }
        m_state.addProperty(prefix + " @id", course.getId());
        m_state.addProperty(prefix + " @name", course.getName());
        m_state.addProperty(prefix + " exercises", "");
        save();
    }

    /**
     * Saves an connection to the state.
     * 
     * @param connection
     *            the connection
     * @throws ConfigurationException
     *             If the saving fails
     */
    public void addConnection(Connection connection)
            throws ConfigurationException {
        final String connectionPrefix = "connections/connection[last()]";
        m_state.addProperty("connections connection/remote", "");
        m_state.addProperty(connectionPrefix + "/auth", "");
        m_state.addProperty(connectionPrefix + "/ssh", "");
        m_state.addProperty(connectionPrefix + "/structure", "");

        m_state.addProperty(connectionPrefix + "  @id", connection.getId());
        m_state.addProperty(connectionPrefix + "  @name", connection.getName());

        m_state.addProperty(connectionPrefix + "/remote @location",
                connection.getLocation());
        m_state.addProperty(connectionPrefix + "/remote @type", connection
                .getConnectionType().toString());
        m_state.addProperty(connectionPrefix + "/remote @protocol",
            connection.getProtocol());
        m_state.addProperty(connectionPrefix + "/remote @allowedDomain",
            connection.getAllowedDomain());
        m_state.addProperty(connectionPrefix + "/auth @username",
                connection.getUsername());
        m_state.addProperty(connectionPrefix + "/auth @password",
                connection.getPassword());

        m_state.addProperty(connectionPrefix + "/ssh @username",
                connection.getSshUsername());

        m_state.addProperty(connectionPrefix + "/ssh @keyfile",
                FilenameUtils.separatorsToUnix(connection
                        .getSshKeyFileLocation().toString()));

        // write the structure
        if (connection.getStructure() != null) {
            for (String item : connection.getStructure().getStructure()) {
                m_state.addProperty(connectionPrefix + "/structure/item", "");
                m_state.addProperty(connectionPrefix
                        + "/structure/item[last()]" + " @value", item);
            }
        }

        save();

    }

    /**
     * Saves an exercise to the state.
     * 
     * @param courseId
     *            the course id
     * @param exercise
     *            the exercise
     * @throws ConfigurationException
     *             if the saving fails
     */
    public void addExercise(int courseId, Exercise exercise)
            throws ConfigurationException {
        final ExerciseContext context = exercise.getContext();
        final String exercisePrefix = "/courses/course[@id='" + courseId
                + "']/exercises/exercise[last()]";

        String key = "/courses/course[@id='" + courseId
                + "']/exercises exercise";

        // Fixes adding an exercise when the exercises subnode doesn't exist
        try {
            m_state.addProperty(key, "");
        } catch (IllegalArgumentException e) {
            m_state.addProperty("/courses/course[@id='" + courseId
                    + "'] exercises", "");
            m_state.addProperty(key, "");
        }
        m_state.addProperty(exercisePrefix + " @id", exercise.getId());
        m_state.addProperty(exercisePrefix + " @name", exercise.getName());
        m_state.addProperty(exercisePrefix + " @language", context
                .getLanguageType().toString());

        m_state.addProperty(exercisePrefix + " connection", "");
        m_state.addProperty(exercisePrefix + "/connection @id",
                context.getConnectionId());

        m_state.addProperty(exercisePrefix + " times", "");
        m_state.addProperty(exercisePrefix + "/times @deadline", context
                .getDeadline().getTimeInMillis());
        m_state.addProperty(exercisePrefix + "/times @start", context
                .getStartTime().getTimeInMillis());
        m_state.addProperty(exercisePrefix + "/times @period",
                context.getPeriod());

        m_state.addProperty(exercisePrefix + " flags", "");
        for (String flag : context.getCompilerFlags()) {
            m_state.addProperty(exercisePrefix + "/flags/flag", "");
            m_state.addProperty(exercisePrefix + "/flags/flag[last()] @value",
                    flag);
        }
        save();
    }

    /**
     * Loads the Connections from the state.xml.
     * 
     * @return a Map containing the connections, with the ID of the connections
     *         as the Key.
     * @throws InvalidStructureException
     *             if the structure is invalid
     */
    public Map<Integer, Connection> restoreConnections()
            throws InvalidStructureException {
        Map<Integer, Connection> connectionsMap = new HashMap<>();

        List<HierarchicalConfiguration> connectionNodes = m_state
                .configurationsAt("/connections/*");

        for (HierarchicalConfiguration connectionNode : connectionNodes) {

            // extract values
            int id = connectionNode.getInt("@id");
            String name = connectionNode.getString("@name");
            ConnectionType connectionType = ConnectionType
                    .valueOf(connectionNode.getString("remote/@type"));
            String location = connectionNode.getString("remote/@location");
            String protocol = connectionNode.getString("remote/@protocol");
            String username = connectionNode.getString("auth/@username");
            String password = connectionNode.getString("auth/@password");
            String sshUsername = connectionNode.getString("ssh/@username");
            String sshKeyFileName = connectionNode.getString("ssh/@keyfile");
            String allowedDomain = connectionNode.getString("remote/@allowedDomain");

            // extract structure
            List<HierarchicalConfiguration> structure = connectionNode
                    .configurationsAt("/structure/item");
            List<String> structureList = new LinkedList<>();
            for (HierarchicalConfiguration structureNode : structure) {
                structureList.add(structureNode.getString("@value"));
            }

            Connection connection = new Connection(id, name, connectionType,
                    location, protocol, username, password, sshUsername, sshKeyFileName,
                    structureList, allowedDomain);

            connectionsMap.put(id, connection);
        }

        return connectionsMap;
    }

    /**
     * Convenience shortcut for saving.
     * 
     * @throws ConfigurationException
     *             if the saving fails
     */
    public void save() throws ConfigurationException {
        m_state.save();
    }

    /**
     * Deletes the specified course.
     * 
     * @param id
     *            the id
     * @throws ConfigurationException
     *             if the saving of the state fails
     */
    public void deleteCourse(int id) throws ConfigurationException {
        m_state.clearTree("/courses/course[@id=' " + id + "']");
        try {
            m_state.configurationsAt("/courses");
        } catch (IllegalArgumentException e) {
            m_state.addProperty("/ courses", "");
        }
        save();
    }

    /**
     * Deletes the specified connection.
     * 
     * @param id
     *            the id
     * @throws ConfigurationException
     *             if the saving fails
     */
    public void deleteConnection(int id) throws ConfigurationException {
        m_state.clearTree("/connections/connection[@id=' " + id + "']");
        try {
            m_state.configurationsAt("/commections");
        } catch (IllegalArgumentException e) {
            m_state.addProperty("/ connections", "");
        }
        save();
    }

    /**
     * Deletes the specified exercise.
     * 
     * @param courseId
     *            the course id
     * @param exerciseId
     *            the exercise id
     * @throws ConfigurationException
     *             the configuration exception
     */
    public void deleteExercise(int courseId, int exerciseId)
            throws ConfigurationException {

        String derp = "/courses/course[@id='" + courseId
                + "']/exercises/exercise[@id='" + exerciseId + "']";

        String floo = m_state.getString(derp + "/@name");
        System.out.println(floo);

        m_state.clearTree("/courses/course[@id='" + courseId
                + "']/exercises/exercise[@id='" + exerciseId + "']");

        // If we delete the last exercise we need to restore the exercises
        // subnode
        try {
            m_state.configurationAt("/courses/course[@id='" + courseId
                    + "']/exercises");
        } catch (IllegalArgumentException e) {
            m_state.addProperty("/courses/course[@id='" + courseId
                    + "'] exercises", "");
        }

        save();

    }

    /**
     * Updates a course with a new Name.
     * 
     * @param courseId
     *            the id of the course to be updated
     * @param courseName
     *            the new name for the course
     * @throws ConfigurationException
     *             if the saving fails
     */
    public void updateCourse(int courseId, String courseName)
            throws ConfigurationException {
        m_state.setProperty("/courses/course[@id='" + courseId + "']/@name",
                courseName);
        save();
    }

    /**
     * Reads the content of the StateXML file and returns it as String.
     * 
     * @return the content of the state.xml file
     * @throws IOException
     *             if the file can't be read.
     */
    public String readWholeXML() throws IOException {
        return FileUtils.readFileToString(m_stateFile);
    }

    /**
     * Writes the content of the String into the state.xml. <b>ATTENTION!</b>
     * the system needs to be rebooted after running this method!
     * 
     * @param textToWrite
     *            the content for the state.xml file
     * @throws IOException
     *             if the old state.xml could not be deleted
     */
    public void writeWholeXML(String textToWrite) throws IOException {
        Files.delete(m_stateFile.toPath());
        FileUtils.writeStringToFile(m_stateFile, textToWrite);
    }

    /**
     * Writes an updated exercise to the state.xml.
     * 
     * @param courseId
     *            the id of the course the exercise belongs to
     * @param exerciseId
     *            the id of the exercise
     * @param exercise
     *            the exercise itself
     * @throws ConfigurationException
     *             if the state could not be saved
     */
    public void updateExercise(int courseId, int exerciseId, Exercise exercise)
            throws ConfigurationException {

        final ExerciseContext context = exercise.getContext();
        final String exercisePrefix = "/courses/course[@id='" + courseId
                + "']/exercises/exercise[@id='" + exerciseId + "']";

        m_state.setProperty(exercisePrefix + "/@id", exercise.getId());
        m_state.setProperty(exercisePrefix + "/@name", exercise.getName());
        m_state.setProperty(exercisePrefix + "/@language", context
                .getLanguageType().toString());

        m_state.setProperty(exercisePrefix + "/connection/@id",
                context.getConnectionId());

        m_state.setProperty(exercisePrefix + "/times/@deadline", context
                .getDeadline().getTimeInMillis());
        m_state.setProperty(exercisePrefix + "/times/@start", context
                .getStartTime().getTimeInMillis());
        m_state.setProperty(exercisePrefix + "/times/@period",
                context.getPeriod());

        m_state.clearTree(exercisePrefix + "/flags");
        m_state.setProperty(exercisePrefix + "/flags", "");

        for (String flag : context.getCompilerFlags()) {
            m_state.addProperty(exercisePrefix + "/flags/flag", "");
            m_state.addProperty(exercisePrefix + "/flags/flag[last()] @value",
                    flag);
        }

        save();
    }
}
