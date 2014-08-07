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

package preprocess;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import preprocess.tokenize.InvalidStructureException;
import preprocess.tokenize.SubmissionStructure;

import com.google.gson.annotations.Expose;

/**
 * The connection object stores connection data needed by the Preprocessor.
 *
 * @author <a href="mailto:marcel.hiller@uni-konstanz.de">Marcel Hiller</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */

public class Connection {

    // ------------------------------ FIELDS ------------------------------

    /** The name. */
    @Expose
    private String name;

    @Expose
    private final int id;

    /** The connection type. */
    @Expose
    private ConnectionType connectionType;

    /* The location of the remote, this is typically an IP address. */
    @Expose
    private String location;

    /* Login data for password authentication. */
    @Expose
    private String username;

    /** The password. */
    private String password;

    /* Login data for ssh key file authentication. */
    @Expose
    private String sshUsername;

    @Expose
    /** The ssh key file. */
    private Path m_sshKeyFile;

    // @Expose
    // /* Exposable variant of the submission structure*/
    // private List<String> structure;

    private SubmissionStructure m_structure;

    @Expose
    private String structureString;

    // --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Create a new ConnectionData with location, user name, password SHH user
     * name and SSH key and Structure. This is used by the State class to allow
     * deserialization of all connection objects.
     *
     *
     * @param id
     *            the ID of the connection
     * @param name
     *            the name of the connection
     * @param connectionType
     *            the {@link preprocess.ConnectionType} of the remote
     * @param location
     *            the location of the remote (typically a URL)
     * @param username
     *            username for login.
     * @param password
     *            Password for login.
     * @param sshUsername
     *            username for ssh-login
     * @param keyFileName
     *            name of the secret key file for ssh-login.
     * @throws InvalidStructureException
     *             if the structure is inavlid and needed
     */
    public Connection(int id, String name, ConnectionType connectionType,
            String location, String username, String password,
            String sshUsername, String keyFileName, List<String> structureList)
            throws InvalidStructureException {
        this.name = name;
        this.connectionType = connectionType;
        this.location = location;
        this.username = username;
        this.password = password;
        this.sshUsername = sshUsername;
        if ((keyFileName == null) || "".equals(keyFileName)) {
            m_sshKeyFile = Paths.get("");
        } else {
            m_sshKeyFile =
                    Paths.get(System.getProperty("user.dir"), "res",
                            "keyfiles", "connection-" + id, keyFileName);
        }
        this.id = id;
        setStructure(structureList);
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * @param name
     *            the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the connection type.
     *
     *
     *
     * @param connectionType
     *            the new connection type
     */
    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    /**
     * Sets the location.
     *
     *
     *
     * @param location
     *            the location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the username.
     *
     *
     *
     * @param username
     *            the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the password.
     *
     *
     *
     * @param password
     *            the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the ssh username.
     *
     *
     *
     * @param sshUsername
     *            the new ssh username
     */
    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    /**
     * Sets the ssh key file.
     *
     *
     * @param sshKeyFile
     *            the new ssh key file
     */
    public void setSshKeyFile(Path sshKeyFile) {
        m_sshKeyFile = sshKeyFile;
    }

    /**
     * Sets a new {@link SubmissionStructure}.
     * 
     * @param structureList
     *            the new submission structure
     * @throws InvalidStructureException
     *             if the structure is invalid
     */
    public void setStructure(List<String> structureList)
            throws InvalidStructureException {
        if (connectionType == ConnectionType.SVN) {
            m_structure = new SubmissionStructure(structureList);
            structureString = "";
            for (String structureItem : structureList) {
                structureString += structureItem + "\n";
            }
        } else {
            m_structure = null;
        }
    }

    /**
     * Gets the name.
     *
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the id.
     *
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the connection type.
     *
     * @return the type of the connection
     */
    public ConnectionType getConnectionType() {
        return connectionType;
    }

    /**
     * Gets the location of the remote.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the ssh username.
     *
     * @return the ssh username
     */
    public String getSshUsername() {
        return sshUsername;
    }

    /**
     * Gets the ssh key file.
     *
     * @return the ssh key file
     */
    public Path getSshKeyFileLocation() {
        return m_sshKeyFile;
    }

    /**
     * Gets the structure of the remote.
     *
     * @return the structure of the data downloaded by the remote, this might
     *         be null if such strcuture is not provided (e.g. for ilias)
     */
    public SubmissionStructure getStructure() {
        return m_structure;
    }

}
