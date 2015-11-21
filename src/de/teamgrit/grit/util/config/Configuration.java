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

package de.teamgrit.grit.util.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.FileUtils;

/**
 * 
 * @author <a href=mailto:gabriel.einsdorf@uni-konstanz.de>Gabriel Einsdorf</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 */
public class Configuration {

    private static final Logger LOGGER = Logger.getLogger("syslog");

    // ------------------------------ FIELDS ------------------------------

    private XMLConfiguration m_config;
    private File m_configXML;

    /* mail configuration of mail address we want to send mails from */
    private String m_SMTP_HOST;
    private String m_MAIL_PORT;
    private String m_SENDER_MAIL_ADRESS;
    private String m_MAIL_PASSWORD;
    /* admin info */
    private String m_MAIL_ADMIN;
    private String m_NAME_ADMIN;
    private String m_PASSWORD_ADMIN;
    /* server info */
    private int m_SERVER_PORT;
    private String m_LOGLEVEL;

    // --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Loads the configuration from the specified File.
     * 
     * @param file
     *            the file the config is stored in
     * @throws ConfigurationException
     *             if the configuration file can't be read.
     * @throws FileNotFoundException
     *             if the file can't be found
     */
    public Configuration(File file) throws ConfigurationException,
            FileNotFoundException {

        LOGGER.info("Loading configuration from file: "
                + file.getAbsolutePath());

        if (!file.exists()) {
            LOGGER.warning("Could not find configuration file: "
                    + file.getAbsolutePath());
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        m_configXML = file;
        m_config = new XMLConfiguration(file);

        // set xpath engine for more powerful queries
        m_config.setExpressionEngine(new XPathExpressionEngine());
        loadToMember();
        // save();
    }

    // --------------------------- GETTER/SETTER ---------------------------

    /**
     * Gets the current server port.
     * 
     * @return the current port
     */
    public int getServerPort() {
        return m_SERVER_PORT;
    }

    /**
     * Returns the log level of the logger.
     * 
     * @return the currently set logLevel
     */
    public String getLogLevel() {
      return m_LOGLEVEL;
    }

    /**
     * Gets the smtp host.
     * 
     * @return the smtp host
     */
    public String getSmtpHost() {
        return m_SMTP_HOST;
    }

    /**
     * Gets the sender mail address.
     * 
     * @return the mail address
     */
    public String getSenderMailAdress() {
        return m_SENDER_MAIL_ADRESS;
    }

    /**
     * Gets the mail-protocol for notifications.
     * Usually smtp or smtps.
     * @return
     */
    public String getMailPort() {
      return m_MAIL_PORT;
    }

    /**
     * Gets the current mail account password.
     * 
     * @return the password
     */
    public String getMailPassword() {
        return m_MAIL_PASSWORD;
    }

    /**
     * Gets the current mail account for the admin notification.
     * 
     * @return the mail address
     */
    public String getMailAdmin() {
        return m_MAIL_ADMIN;
    }

    /**
     * Gets the name of the admin for the login on the website.
     * 
     * @return the name
     */
    public String getAdminName() {
        return m_NAME_ADMIN;
    }

    /**
     * Gets the current admin password for the login on the website.
     * 
     * @return the password
     */
    public String getAdminPassword() {
        return m_PASSWORD_ADMIN;
    }

    /**
     * Sets the new name of the admin for the login.
     * 
     * @param nameAdmin
     *            the new name
     */
    public void setAdminName(String nameAdmin) {
        m_NAME_ADMIN = nameAdmin;
    }

    /**
     * Sets the new admin password for the login.
     * 
     * @param passwordAdmin
     *            the new password
     */
    public void setPasswordAdmin(String passwordAdmin) {
        m_PASSWORD_ADMIN = passwordAdmin;
    }

    /**
     * Sets the new mail for the notifications for the admin.
     * 
     * @param mailAdmin
     *            the new mail address
     */
    public void setAdminMail(String mailAdmin) {
        m_MAIL_ADMIN = mailAdmin;
    }

    /**
     * Sets the log level of the server log.
     * For a list of supported loglevels, consider {@link Level}. 
     * In case of an invalid input, the log level will be left
     * untouched.
     * <br></br>
     * In case of an entirely invalid and config-breaking value,
     * it is being attempted to repair this part by setting
     * the level to "INFO".
     * 
     * @param logLevel 
     *            the desired log level
     */
    public void setLogLevel(String logLevel) {
      try {
        logLevel = logLevel.toUpperCase(Locale.ENGLISH);
        LOGGER.setLevel(Level.parse(logLevel));
      }
      catch (NullPointerException | IllegalArgumentException e) {
        if (LOGGER.getLevel() != null) {
          m_LOGLEVEL = LOGGER.getLevel().toString();
          m_config.setProperty("server/loglevel/@value", m_LOGLEVEL);
          return;
        } else {
          LOGGER.setLevel(Level.parse("INFO"));
          m_config.setProperty("server/loglevel/@value", "INFO");
          return;
        }
      }
      m_LOGLEVEL = logLevel;
    }

    // --------------------------- HELP METHODS ---------------------------

    /**
     * reads from the configuration file and sets the member variables.
     */
    public void loadToMember() {
        /* read mail info from configuration xml */
        m_MAIL_PASSWORD = m_config.getString("email/auth/@password");
        m_SENDER_MAIL_ADRESS = m_config.getString("email/auth/@senderAddress");
        m_MAIL_PORT = m_config.getString("email/auth/@port");
        m_SMTP_HOST = m_config.getString("email/auth/@host");
        /* read admin info from configuration xml */
        m_MAIL_ADMIN = m_config.getString("admin/user/@email");
        m_NAME_ADMIN = m_config.getString("admin/user/@name");
        m_PASSWORD_ADMIN = m_config.getString("admin/user/@password");
        /* read server info from configuration xml */
        m_SERVER_PORT = m_config.getInt("server/port/@value");
        setLogLevel(m_config.getString("server/loglevel/@value"));

    }

    /**
     * Writes the values of the members to the config and saves it to the disk.
     * 
     * @throws ConfigurationException
     *             if we can't write to the config
     */
    public void writeFromMember() throws ConfigurationException {
        m_config.setProperty("admin/user/@email", m_MAIL_ADMIN);
        m_config.setProperty("admin/user/@name", m_NAME_ADMIN);
        m_config.setProperty("admin/user/@password", m_PASSWORD_ADMIN);
        m_config.setProperty("server/loglevel/@value", m_LOGLEVEL);
        m_config.save();
    }

    /**
     * Reads the content of the config file and returns it as String.
     * 
     * @return the content of the config.xml file
     * @throws IOException
     *             if the file can't be read.
     */
    public String readWholeXML() throws IOException {
        return FileUtils.readFileToString(m_configXML);
    }

    /**
     * Writes the content of the String into the config.xml. <b>ATTENTION!</b>
     * the system needs to be rebooted after running this method!
     * 
     * @param textToWrite
     *            the content for the config.xml file
     * @throws IOException
     *             if the old configuration can not be deleted
     */
    public void writeWholeXML(String textToWrite) throws IOException {
        Files.delete(m_configXML.toPath());
        FileUtils.writeStringToFile(m_configXML, textToWrite);
    }

}
