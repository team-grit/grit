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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;

import de.teamgrit.grit.main.Boot;
import de.teamgrit.grit.preprocess.Connection;
import de.teamgrit.grit.preprocess.ConnectionType;
import de.teamgrit.grit.preprocess.CouldNotConnectException;
import de.teamgrit.grit.preprocess.tokenize.InvalidStructureException;
import de.teamgrit.grit.util.config.Configuration;

/**
 * Singleton class that accepts and handles requests from other modules e.g.
 * the web module.
 *
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public final class Controller {

    /**
     * The grit-wide logger.
     */
    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private static final Path CONFIG_LOCATION = Paths.get("config",
            "config.xml");

    /**
     * The instance used for the singleton implementation.
     */
    private static Controller s_instance = null;

    /**
     * The config.
     */
    private Configuration m_config = null;
    private State m_state = null;

    // This field indicates the id of the next course that is generated
    private int m_nextCourseId = 0;

    private int m_nextConnectionId = 0;

    /**
     * A Map of {@link Course}s. All courses present in GRIT will be stored
     * here.
     */
    private Map<Integer, Course> m_courses = null;

    /**
     * A Map of {@link Connection}s. All remote connections present in GRIT
     * will be stored here.
     */
    private Map<Integer, Connection> m_connections = null;

    /**
     * Instantiates a new controller.
     */
    private Controller() {
        m_courses = new HashMap<>();
        m_connections = new HashMap<>();

        try {
            // create log directory if it does not exist
            if (!Paths.get("log").toFile().exists()) {
                File logFolder = new File("log");
                logFolder.mkdir();
            }
            // Tell the logger to log there.
            FileHandler fh =
                    new FileHandler(Paths.get("log", "system.log").toString(),
                            true);
            LOGGER.addHandler(fh);

            // log in human readable format
            fh.setFormatter(new SimpleFormatter());

            // do not log to console
            LOGGER.setUseParentHandlers(false);
        } catch (SecurityException | IOException e) {
            System.err.println("Could not start logger on log/system.log: "
                    + e.getMessage());
            e.printStackTrace();
        }
        try {
            m_config = new Configuration(CONFIG_LOCATION.toFile());
        } catch (ConfigurationException | FileNotFoundException e) {
            LOGGER.severe("Error while creating Controller: " + e.toString());
        }
    }

    /**
     * Gets the controller instance.
     *
     * @return the {@link Controller} of GRIT
     */
    public static Controller getController() {
        if (s_instance == null) {
            s_instance = new Controller();
        }
        return s_instance;
    }

    // --------------------- CONFIGURATION ---------------------

    /**
     * Gets the config.
     *
     * @return the config
     */
    public Configuration getConfig() {
        return m_config;
    }

    /**
     * Gets the state.
     *
     * @return stored state
     */
    public State getState() {
        return m_state;
    }

    /**
     * Sets the configuration.
     *
     * @param config
     *            the new configuration
     * @throws BadConfigException
     *             the bad configuration exception
     */
    public void setConfig(Configuration config) throws BadConfigException {
        m_config = config;
    }

    // --------------------- STATE ---------------------

    /**
     * Restores the state of GRIT.
     *
     * @param state
     *            the State storing the state information.
     * @throws ConfigurationException
     *             if the restoring fails.
     */
    public void restoreState(State state) throws ConfigurationException {
        m_state = state;
        try {
            m_connections = state.restoreConnections();
            if (!m_connections.isEmpty()) {
                m_nextConnectionId =
                        Collections.max(m_connections.keySet()) + 1;
            }
        } catch (InvalidStructureException e) {
            throw new ConfigurationException(e);
        }
        m_courses = state.restoreCourses();
        if (!m_courses.isEmpty()) {
            m_nextCourseId = Collections.max(m_courses.keySet()) + 1;
        }
    }

    // --------------------- COURSES -------------------------

    /**
     * Adds a new course.
     *
     * @param name
     *            the name of the course
     * @return the course
     * @throws ConfigurationException
     *             if the course cant be saved to the state
     */
    public Course addCourse(String name) throws ConfigurationException {
        Course course = new Course(m_nextCourseId, name);
        m_courses.put(course.getId(), course);
        m_state.addCourse(course);
        m_nextCourseId++;
        return course;
    }

    /**
     * Gets a {@link Course} for a given name. Returns null if {@link Course}
     * does not exist.
     *
     * @param id
     *            the ID of the wanted course
     * @return the stored {@link Course} for the name
     */
    public Course getCourse(int id) {
        return m_courses.get(id);
    }

    /**
     * Gets the courses.
     *
     * @return copy of the stored {@link Collection} of {@link Course}s
     */
    public Collection<Course> getCourses() {
        return m_courses.values();
    }

    /**
     * Updates a {@link Course}.
     *
     * @param courseId
     *            the ID of the {@link Course}
     * @param courseName
     *            the new name
     * @return the updated {@link Course}
     * @throws ConfigurationException
     *             if the saving fails
     */
    public Course updateCourse(int courseId, String courseName)
            throws ConfigurationException {
        Course course = getCourse(courseId);
        course.setName(courseName);
        m_state.updateCourse(courseId, courseName);
        return course;
    }

    /**
     * Removes a {@link Course} and and its {@link Exercise}s as
     * well as their corresponding pdf-reports (whether temporary or
     * the final one).
     *
     * @param courseId
     *            the id of {@link Course} that should be removed
     * @return the removed {@link Course} on success, null otherwise
     * @throws ConfigurationException
     *             if the saving fails.
     * @throws IOException
     *             if the directory of the course could not be deleted
     */
    public Course deleteCourse(int courseId) throws ConfigurationException,
            IOException {
        Course course = getCourse(courseId);
        if (course != null) {
            // terminate all exercises
            for (Exercise exercise : course.getExercises()) {
                exercise.terminate();
            }
            m_courses.remove(courseId);
        }
        m_state.deleteCourse(courseId);
        FileUtils.deleteDirectory(Paths.get("wdir", "course-" + courseId)
                .toFile());
        FileUtils.deleteDirectory(Paths.get("res","web","pdf","course-" + courseId)
                .toFile());
        return course;

    }

    // --------------------- CONNECTIONS ---------------------

    /**
     * Adds a connection to the system. It checks the validity of the
     * information before adding it to the list.
     * 
     * @param connectionName
     *            name of the connection
     * @param connectionType
     *            the type of the connection (e.g. SVN, ILIAS)
     * @param location
     *            the location (address) of the data source
     * @param protocol
     *            the protocol of the connection, if it's MAIL
     * @param username
     *            the username to login
     * @param password
     *            the password for the username
     * @param sshUsername
     *            the sshUsername to connect via ssh
     * @param keyFileName
     *            a keyfile to authenticate via ssh
     * @param structure
     *            the submission structure of the data source
     * @return the created connection
     * @throws ConfigurationException
     *             if saving to state fails
     * @throws InvalidStructureException
     *             if the specified structure is invalid
     * @throws CouldNotConnectException 
     *             if the connection could not be established
     */
    public Connection addConnection(String connectionName,
            ConnectionType connectionType, String location, String protocol, String username,
            String password, String sshUsername, String keyFileName,
            List<String> structure, String allowedDomain) throws ConfigurationException,
            InvalidStructureException, CouldNotConnectException {
        Connection connection =
                new Connection(m_nextConnectionId, connectionName,
                        connectionType, location, protocol, username, password,
                        sshUsername, keyFileName, structure, allowedDomain);
        if (!connection.checkConnection()) {
          throw new CouldNotConnectException("Connection could not be established");
        }
        m_connections.put(m_nextConnectionId, connection);
        m_nextConnectionId++;
        m_state.addConnection(connection);
        return connection;
    }

    /**
     * Updates a connection with new information and checks their validity
     * before adding them to the new list.
     * 
     * @param id
     *            the id of the updated connection
     * @param connectionName
     *            the new name of the connection
     * @param connectionType
     *            the new {@link ConnectionType} (SVN, ILIAS)
     * @param location
     *            the new address of the data source
     * @param username
     *            the new login username
     * @param password
     *            the new password for the username
     * @param sshUsername
     *            the new ssh username
     * @param keyFileName
     *            the new keyfile for the ssh username
     * @param structure
     *            the new submission structure
     * @param allowedDomain
     *            the allowed domain for the mail connection
     * @return the updated connection
     * @throws InvalidStructureException
     *             if the submission structure is invalid
     * @throws ConfigurationException
     *             if saving to state fails
     * @throws CouldNotConnectException 
     *             in case a connection could not be established
     */
    public Connection updateConnection(int id, String connectionName,
            ConnectionType connectionType, String location, String protocol,
            String username, String password, String sshUsername,
            String keyFileName, List<String> structure, String allowedDomain) 
                throws InvalidStructureException,
            ConfigurationException, CouldNotConnectException {
        Connection modifiedConnection =
                new Connection(id, connectionName, connectionType, location,
                        protocol, username, password, sshUsername, keyFileName,
                        structure, allowedDomain);
        if (!modifiedConnection.checkConnection()) {
          throw new CouldNotConnectException("Checking connection failed");
        }
        m_connections.put(id, modifiedConnection);
        m_state.deleteConnection(id);
        m_state.addConnection(modifiedConnection);
        return modifiedConnection;

    }

    /**
     * Gets a {@link Connection} for a given name. Returns null if
     * {@link Connection} does not exist.
     *
     * @param id
     *            the id of the wanted connection
     * @return the stored {@link Connection} for the name
     */
    public Connection getConnection(int id) {
        return m_connections.get(id);
    }

    /**
     * Gets the {@link Connection}s.
     *
     * @return copy of the stored {@link Collection} of {@link Connection}s
     */
    public Collection<Connection> getConnections() {
        return m_connections.values();
    }

    /**
     * Removes a {@link Connection} from the connections list.
     *
     * @param connectionId
     *            the id of {@link Connection} that should be removed
     * @return the removed {@link Course} on success, null otherwise
     * @throws ConfigurationException
     *             if the saving to the state fails.
     * @throws ConnectionUsedException
     *             if the connection is still used
     */
    public Connection deleteConnection(int connectionId)
            throws ConfigurationException, ConnectionUsedException {
        Connection connection = getConnection(connectionId);
        for (Course course : getCourses()) {
            for (Exercise exercise : course.getExercises()) {
                if (connectionId == exercise.getContext().getConnectionId()) {
                    throw new ConnectionUsedException("The connection \""
                            + connection.getName() + "\" is still used!");
                }
            }
        }
        m_connections.remove(connectionId);
        m_state.deleteConnection(connectionId);
        return connection;
    }

    // --------------------- EXERCISES ---------------------

    /**
     * Creates an exercise from meta data, adds it to a course and starts it.
     *
     * @param courseId
     *            the id of the course the exercise belongs to.
     * @param connectionId
     *            the id of the connection
     * @param metadata
     *            the meta data for the exercise
     * @return the added exercise
     * @throws ConfigurationException
     *             if the state can't be saved
     * @throws WrongDateException
     *             if the deadline is before the starttime
     */
    public Exercise addExercise(int courseId, int connectionId,
            ExerciseMetadata metadata) throws ConfigurationException,
            WrongDateException {
        Exercise exercise =
                m_courses.get(courseId).addExercise(connectionId, metadata);
        m_state.addExercise(courseId, exercise);
        return exercise;
    }

    // --------------------- EXERCISES ---------------------

    /**
     * Returns the exercise with the given id.
     *
     * @param courseId
     *            the id of the course containing the exercise
     * @param exerciseId
     *            the id of the exercise
     * @return the exercise identified by the id.
     */
    public Exercise getExercise(int courseId, int exerciseId) {
        return m_courses.get(courseId).getExercise(exerciseId);
    }

    /**
     * Replaces the ExerciseContext of a course with the one given as argument.
     *
     * @param courseId
     *            the id of {@link Course} that contains the exercise.
     * @param exerciseId
     *            the id of the exercise.
     * @param connectionId
     *            the id of the connection used by the exercise
     * @param metadata
     *            the meta date for the exercise
     * @return the updated exercise
     * @throws ConfigurationException
     *             if the state can't be saved
     * @throws WrongDateException
     *             if the deadline is before the starttime
     */
    public Exercise updateExercise(int courseId, int exerciseId,
            int connectionId, ExerciseMetadata metadata)
            throws ConfigurationException, WrongDateException {
        Exercise exercise =
                m_courses.get(courseId).updateExercise(exerciseId,
                        connectionId, metadata);
        m_state.updateExercise(courseId, exerciseId, exercise);
        return exercise;
    }

    /**
     * Removes an exercise and stops it.
     *
     * @param courseId
     *            the id of the course that contains the exercise
     * @param exerciseId
     *            the id of the exercise to be removed
     *
     * @return the removed exercise
     * @throws ConfigurationException
     *             if the saving to the state fails.
     */
    public Exercise deleteExercise(int courseId, int exerciseId)
            throws ConfigurationException {

        Exercise removed = m_courses.get(courseId).deleteExercise(exerciseId);
        m_state.deleteExercise(courseId, exerciseId);
        return removed;
    }

    /**
     * Reboots GRIT.
     *
     * @throws CouldNotRebootException
     *             if the grit server could not be stopped
     */
    public void reboot() throws CouldNotRebootException {
        for (Course course : m_courses.values()) {
            course.stopAllExercises();
        }
        s_instance = null;
        try {
            Boot.reboot();
        } catch (Exception e) {
            throw new CouldNotRebootException(e);
        }
    }
}
