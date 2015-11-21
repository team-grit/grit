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

package de.teamgrit.grit.preprocess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import com.google.gson.annotations.Expose;

import de.teamgrit.grit.preprocess.tokenize.InvalidStructureException;
import de.teamgrit.grit.preprocess.tokenize.SubmissionStructure;
import de.teamgrit.grit.util.mailer.EncryptorDecryptor;

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

    private static final Logger LOGGER = Logger.getLogger("systemlog");

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
    
    /* The protocol of the remote. Only non-blank in case of a Mail
     * connection */
    @Expose
    private String protocol;

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

    @Expose
    /** the allowed domain for the mail connection*/
    private String allowedDomain;

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
     *            the {@link de.teamgrit.grit.preprocess.ConnectionType} of the remote
     * @param location
     *            the location of the remote (typically a URL)
     * @param protocol
     *            the protocol of this location
     * @param username
     *            username for login.
     * @param password
     *            Password for login.
     * @param sshUsername
     *            username for ssh-login
     * @param keyFileName
     *            name of the secret key file for ssh-login.
     * @param allowedDomain
     *            the domain whose emails are seen as submission(s)
     * @throws InvalidStructureException
     *             if the structure is inavlid and needed
     */
    public Connection(int id, String name, ConnectionType connectionType,
            String location, String protocol, String username, String password,
            String sshUsername, String keyFileName, List<String> structureList,
            String allowedDomain)
            throws InvalidStructureException {
        this.name = name;
        this.connectionType = connectionType;
        this.location = location;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
        this.allowedDomain = allowedDomain;
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

    // --------------------- HELPER METHODS ------------------------------

    /**
     * Checks the associated connection for validity in terms of
     * connection and whether it can be established.
     * It does so by performing a dry run on the given connection,
     * i.e. performing a "list" command for the svn-repository.
     * <br></br>
     * All arising exceptions have been summed up to 
     * {@link CouldNotConnectException}.
     * @return 
     *    true in case the check went fine. Otherwise false.
     * @throws CouldNotConnectException
     *              In case something went wrong while checking.
     */
    public boolean checkConnection() throws CouldNotConnectException {
      switch (connectionType) {
        case SVN:
          LOGGER.info("Testing svn connection to "+location);
          try {
                List<String> svnCommand = new LinkedList<>();
                svnCommand.add("svn");
                svnCommand.add("ls");
                svnCommand.add(location);
                svnCommand.add("--non-interactive");
                svnCommand.add("--no-auth-cache");
                svnCommand.add("--depth");
                svnCommand.add("immediates");
                svnCommand.add("--username");
                svnCommand.add(username);
                svnCommand.add("--password");
                EncryptorDecryptor ed = new EncryptorDecryptor();
                svnCommand.add(ed.decrypt(password));
  
                Process svnProcess = null;
                ProcessBuilder svnProcessBuilder = new ProcessBuilder(svnCommand);
                svnProcessBuilder.directory(new File(System.getProperty("user.dir")));
  
                svnProcess = svnProcessBuilder.start();
                svnProcess.waitFor();
  
                if (svnProcess.exitValue() == 0) {
                  return true;
                }
            } catch (InterruptedException e)  {
              LOGGER.severe("Interrupted while waiting for SVN-Check"
                  + " to finish.");
              throw new CouldNotConnectException("SVN-Check got interrupted");

            } catch (InvalidKeyException | NoSuchAlgorithmException
                | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException | IOException e) {
                LOGGER.severe("Could not check "+location+" for working"
                    + " connection. Reason: "+e.toString());
                throw new CouldNotConnectException("Reason: "+e.toString());
             }
             return false;

        case MAIL:
          LOGGER.info("Testing mail connection to "+location);
          Properties props = new Properties(System.getProperties());
          props.setProperty("mail.store.protocol", protocol);
          props.setProperty("mail.imap.timeout", "5000");
          props.setProperty("mail.imap.starttls.enable", "true");
          props.setProperty("mail.imap.connectiontimeout", "5000");
          props.setProperty("mail.imaps.timeout", "5000");
          props.setProperty("mail.imaps.connectiontimeout", "5000");
          props.setProperty("mail.pop3.timeout", "5000");
          props.setProperty("mail.pop3.connectiontimeout", "5000");

          // specified mailbox is searched
          Session session = Session.getInstance(props, null);
          Store store;
          try {
            store = session.getStore();
            EncryptorDecryptor ed;
              ed = new EncryptorDecryptor();
              store.connect(location, username,
                      ed.decrypt(password));
              store.close();
            } catch (InvalidKeyException |
                NoSuchAlgorithmException |
                NoSuchPaddingException |
                IllegalBlockSizeException |
                BadPaddingException |
                MessagingException
                | IOException e) {
              LOGGER.severe("Could not check "+location+" for working"
                  + " connection. Reason: "+e.toString());
              throw new CouldNotConnectException("Reason: "+e.toString());
          }
          return true;
        case ILIAS:
          LOGGER.info("Testing ilias connection to "+location);
          return true;
//          untested! the following code was planned to be used.
//          String dbConnectionString = "jdbc:mysql://" + location + "/ilias";
//          SqlConnector sqlConnection = new SqlConnector(dbConnectionString,
//              username, password);
//          if (sqlConnection.establishConnection()) {
//              return true;
//          }
//          return false;

      default:
        return false;
      }
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
     * Sets the protocol for this connection.
     * Only relevant for connections of type 
     * {@link ConnectionType#MAIL}.
     * 
     * @param protocol, the protocol
     */
    public void setProtocol(String protocol) {
      this.protocol = protocol;
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
     * Gets the protocol of the connection.
     * Only relevant for connections of type
     * {@link ConnectionType#MAIL}
     * 
     * @return the protocol, if applicable. 
     * Otherwise <b>null</b>
     */
    public String getProtocol() {
      return protocol;
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
     * Gets the allowed domain for the mail
     * connection.
     * @return 
     *    the allowed domain, if set by connection. Otherwise null.
     */
    public String getAllowedDomain() {
      return allowedDomain;
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
