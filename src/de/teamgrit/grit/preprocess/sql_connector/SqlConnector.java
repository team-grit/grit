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

package de.teamgrit.grit.preprocess.sql_connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Class which connects to a SQL Database to get data from it. Is used by the
 * IliasFetcher in order to get student/execise info and the paths of the
 * student submissions from Ilias
 *
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 *
 */

public class SqlConnector {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    // Time to wait for an response from the sql server is set to 10 seconds,
    // since this is an acceptable wait.
    private static final int CONNECTIONTIMEOUTSECONDS = 10;

    // standard MySQL Driver we want to use for the connection. Maybe needs to
    // be changed for other DBs.
    // hardcode because we only need to support mysql(ilias) for now.
    private static final String DRIVER = "com.mysql.jdbc.Driver";

    // DB connection information
    private final String m_database;
    private final String m_user;
    private final String m_password;

    private Connection m_sqlConnection;
    private Statement m_sqlStatement;

    /**
     * Constructor: Initialization of the connection with the necessary
     * information to establish a connection to the DB.
     *
     * @param dbUrl
     *            address of the database (put "jdbc:mysql://" at the beginning
     *            of the url)
     * @param user
     *            the user we want to connect with at the database
     * @param pw
     *            password of this user
     */
    public SqlConnector(String dbUrl, String user, String pw) {
        m_database = dbUrl;
        m_user = user;
        m_password = pw;
    }

    /**
     * method which initializes the connection to the database.
     *
     * @return true, if successful
     */
    public boolean establishConnection() {
        try {

            // load the specified driver
            Class.forName(DRIVER);

            // connect to the database with the specified information
            DriverManager.setLoginTimeout(CONNECTIONTIMEOUTSECONDS);
            m_sqlConnection =
                    DriverManager
                            .getConnection(m_database, m_user, m_password);

            // creates object for executing queries on the connection
            m_sqlStatement = m_sqlConnection.createStatement();

        } catch (ClassNotFoundException e) {
            LOGGER.severe("Could not load SQL driver: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            LOGGER.severe("Connection to database at " + m_database
                    + " failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * method to get data from the DB. returns null if query fails.
     *
     * @param query
     *            SQL query which is send to the database
     * @return returns ResultSet which contains the results of the executed
     *         query
     */
    public ResultSet getDataFromDB(String query) {
        try {
            // sends query to the database and returns the results
            return m_sqlStatement.executeQuery(query);
        } catch (SQLException e) {
            LOGGER.severe("Could not execute query on database: "
                    + e.getMessage());
            return null;
        }

    }

    /**
     * method for closing the connection.
     */
    public void closeConnection() {
        try {
            if (m_sqlConnection != null) {
                m_sqlConnection.close();
            }
            if (m_sqlStatement != null) {
                m_sqlStatement.close();
            }
        } catch (SQLException e) {
            LOGGER.severe("Could not close connection to database: "
                    + e.getMessage());
        }

    }

    /**
     * method to test if connection is still alive.
     *
     * @return return false if connection was closed true if connection is
     *         still alive
     * @throws SQLException
     *             throws SQLException if sqlConnection was not yet initialized
     */
    public boolean isConnectionAlive() throws SQLException {
        if (m_sqlConnection == null) {
            LOGGER.severe("error in isConnectionAlive() method: "
                    + "connection not yet initialized. "
                    + "connection is null");
            throw new SQLException("error in isConnectionAlive() method: "
                    + "connection not yet initialized");
        }

        try {
            // check if the connection is still alive with zero timeout
            return m_sqlConnection.isValid(0);
        } catch (SQLException e) {
            LOGGER.severe("Inavlid timeout or invalid SQL connection: "
                    + e.getMessage());
            return false;
        }

    }
}
