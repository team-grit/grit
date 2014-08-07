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

package webserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import preprocess.Connection;
import preprocess.ConnectionType;
import preprocess.tokenize.InvalidStructureException;
import entities.ConnectionUsedException;

/**
 * This is the entity handler for connections. It presents CRUD actions for
 * {@link Connection}s to the website interface.
 *
 * @author Stefano Woerner <stefano.woerner@uni-konstanz.de>
 * @author Eike Heinz <eike.heinz@uni-konstanz.de>
 */
public class ConnectionHandler extends EntityHandler {

    /*
     * (non-Javadoc)
     *
     * @see webserver.EntityHandler#doAction(java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected String doAction(String target, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String[] targetSplit = target.split("/");
        if (target.matches("/list")) {
            return list();
        } else if (target.matches("/create")) {
            return create(request);
        } else if (target.matches("/read/\\d+")) {
            return read(targetSplit[2]);
        } else if (target.matches("/update/\\d+")) {
            return update(targetSplit[2], request);
        } else if (target.matches("/delete/\\d+")) {
            return delete(targetSplit[2]);
        } else if (target.matches("/types")) {
            return types();
        } else {
            throw new BadRequestException("The action \"exercise" + target
                    + "\" does not exist!");
        }
    }

    /**
     * Action to list connections.
     *
     * @return a JSON object to directly write to the AJAX request
     */
    private String list() {
        return GSON.toJson(m_controller.getConnections());
    }

    /**
     * Action to create a new connection.
     *
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return the added {@link Connection} on success, null otherwise
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String create(HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String errorPrefix = "Error in connection/create:\n";

        /* Get all needed parameters from the request. */
        String connectionName;
        ConnectionType connectionType;
        String location;
        String username;
        String password;
        String sshUsername;
        List<String> structure;
        try {
            connectionName =
                    parseName(request.getParameter("connectionName"),
                            "connection name");
            connectionType =
                    parseConnectionType(request.getParameter("connectionType"));
            location = parseLocation(request.getParameter("location"));
            username = parseName(request.getParameter("username"), "username");
            password = request.getParameter("password");
            if (connectionType == ConnectionType.ILIAS) {
                sshUsername =
                        parseName(request.getParameter("sshUsername"),
                                "sshUsername");
            } else {
                sshUsername = "";
            }
            if (connectionType == ConnectionType.SVN) {
                structure = parseStructure(request.getParameter("structure"));
            } else {
                structure = null;
            }
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* If this is an ILIAS connection, we need the SSH key-file */
        Part sshKey = null;
        String keyFileName = "";
        if (connectionType == ConnectionType.ILIAS) {
            try {
                sshKey = request.getPart("sshKeyFile");

            } catch (ServletException e) {
                throw new BadRequestException(errorPrefix
                        + "No multipart request.");
            } catch (IOException e) {
                throw new InternalActionErrorException(errorPrefix
                        + "Could not read the submitted keyfile.");
            }
            keyFileName = sshKey.getSubmittedFileName();
            if ((keyFileName == null) || ("".equals(keyFileName))) {
                throw new BadRequestException("No key-file submitted!");
            }
        }

        /* Create the connection. */
        Connection connection = null;
        try {
            connection =
                    m_controller.addConnection(connectionName, connectionType,
                            location, username, password, sshUsername,
                            keyFileName, structure);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "Could not create connection.");
        } catch (InvalidStructureException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The submitted structure is invalid.");
        }

        /* Write the key-file if this is an ILIAS connection. */
        if (connectionType == ConnectionType.ILIAS) {
            Path outputDirectory =
                    connection.getSshKeyFileLocation().getParent();
            writeSubmittedFile(sshKey, outputDirectory);
        }

        return GSON.toJson(connection);
    }

    /**
     * Action to read connections. This is used for the show and edit views.
     *
     * @param connectionId
     *            the ID of the connection which should be read
     * @return a JSON object to directly write to the AJAX request
     */
    private String read(String connectionId) {
        return GSON.toJson(m_controller.getConnection(Integer
                .parseInt(connectionId)));
    }

    /**
     * Action to update a connection.
     *
     * @param connectionId
     *            the ID of the connection
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return the added {@link Connection} on success, null otherwise
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String update(String connectionId, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String errorPrefix = "Error in connection/create:\n";

        /* Get all needed parameters from the request. */
        String connectionName;
        ConnectionType connectionType;
        String location;
        String username;
        String password;
        String sshUsername;
        List<String> structure;
        try {
            connectionName =
                    parseName(request.getParameter("connectionName"),
                            "connection name");
            connectionType =
                    parseConnectionType(request.getParameter("connectionType"));
            location = parseLocation(request.getParameter("location"));
            username = parseName(request.getParameter("username"), "username");
            password = request.getParameter("password");
            if (connectionType == ConnectionType.ILIAS) {
                sshUsername =
                        parseName(request.getParameter("sshUsername"),
                                "sshUsername");
            } else {
                sshUsername = "";
            }
            if (connectionType == ConnectionType.SVN) {
                structure = parseStructure(request.getParameter("structure"));
            } else {
                structure = null;
            }
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* If this is an ILIAS connection, we need the SSH key-file */
        Part sshKey = null;
        String keyFileName = "";
        if (connectionType == ConnectionType.ILIAS) {
            try {
                sshKey = request.getPart("sshKeyFile");
            } catch (ServletException e) {
                throw new BadRequestException(errorPrefix
                        + "No multipart request.");
            } catch (IOException e) {
                throw new InternalActionErrorException(errorPrefix
                        + "Could not read the submitted keyfile.");
            }
            keyFileName = sshKey.getSubmittedFileName();
            if ((keyFileName == null) || ("".equals(keyFileName))) {
                throw new BadRequestException("No key-file submitted!");
            }
        }

        int connectionIdInt = Integer.parseInt(connectionId);

        /* Update the connection. */
        Connection connection = null;
        try {
            connection =
                    m_controller.updateConnection(connectionIdInt,
                            connectionName, connectionType, location,
                            username, password, sshUsername, keyFileName,
                            structure);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "Could not create connection.");
        } catch (InvalidStructureException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The submitted structure is invalid.");
        }

        /* Write the key-file if this is an ILIAS connection. */
        if (connectionType == ConnectionType.ILIAS) {
            Path outputDirectory =
                    connection.getSshKeyFileLocation().getParent();
            writeSubmittedFile(sshKey, outputDirectory);
        }

        return GSON.toJson(connection);
    }

    /**
     * Action to delete a {@link Connection}.
     *
     * @param connectionId
     *            the id of the connection
     * @return the deleted {@link Connection}
     * @throws InternalActionErrorException
     *             if something went wrong
     */
    private String delete(String connectionId)
            throws InternalActionErrorException {
        String errorPrefix = "Error in connection/delete:\n";
        try {
            int connectionIdInt = Integer.parseInt(connectionId);
            return GSON.toJson(m_controller.deleteConnection(connectionIdInt));
        } catch (ConfigurationException | ConnectionUsedException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "Could not be deleted: " + e.getMessage());
        }
    }

    /**
     * Action to get the connection types.
     *
     * @return a JSON object to directly write to the AJAX request
     */
    private String types() {
        Map<Integer, String> typeMap = new HashMap<>();
        for (ConnectionType type : ConnectionType.values()) {
            typeMap.put(type.ordinal(), type.name());
        }
        return GSON.toJson(typeMap);
    }

    /**
     * Parses the structure String as a List.
     *
     * @param parameter
     *            the String encoding the Structure
     * @return the structure encoded as a list.
     * @throws BadRequestException
     *             if the passed structure is null
     */
    private List<String> parseStructure(String parameter)
            throws BadRequestException {
        if (parameter == null) {
            throw new BadRequestException("The passed structure is null.");
        }
        String structure = StringUtils.remove(parameter, "\r");
        List<String> structureList =
                Arrays.asList(structure.trim().split("\n"));

        return structureList;
    }

}
