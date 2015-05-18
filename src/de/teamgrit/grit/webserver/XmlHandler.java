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

package de.teamgrit.grit.webserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.teamgrit.grit.entities.Controller;
import de.teamgrit.grit.entities.CouldNotRebootException;
import de.teamgrit.grit.entities.Exercise;

/**
 * This is the entity handler for exercises. It presents CRUD actions for
 * {@link Exercise}s to the website interface.
 * 
 * @author Stefano Woerner <stefano.woerner@uni-konstanz.de>
 * @author Eike Heinz <eike.heinz@uni-konstanz.de>
 */
public class XmlHandler extends AbstractHandler {

    /**
     * The grit-wide logger.
     */
    private static final Logger LOGGER = Logger.getLogger("systemlog");

    /**
     * A GSON instance for JSON parsing.
     */
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation().create();

    /**
     * The controller instance will be referenced here.
     */
    private Controller m_controller;

    /**
     * Constructor for {@link XmlHandler}. It calls the constructor of
     * {@link AbstractHandler} and gets the controller instance.
     */
    public XmlHandler() {
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
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        /*
         * Set the content type and response status for default XML handling.
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
        } catch (BadRequestException e) {
            String message = e.getMessage();
            LOGGER.severe(message);
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(message);
        } catch (InternalActionErrorException e) {
            String message = e.getMessage();
            LOGGER.severe(message);
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(message);
        } catch (Exception e) {
            String message = "An unexpected error occured:\n"
                    + e.getClass().getSimpleName() + ":\n" + e.getMessage();
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
    private String doAction(String target, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        if (target.matches("/read")) {
            return read();
        } else if (target.matches("/update/config")) {
            return updateConfig(request);
        } else if (target.matches("/update/state")) {
            return updateState(request);
        } else {
            throw new BadRequestException("The action \"xml" + target
                    + "\" does not exist!");
        }
    }

    /**
     * Action to read the XMLs.
     * 
     * @return a JSON object to directly write to the AJAX request
     * @throws InternalActionErrorException
     *             if something goes wrong when reading the XML files
     */
    private String read() throws InternalActionErrorException {
        List<String> xmls = new ArrayList<>();

        /* Read configuration XML. */
        try {
            xmls.add(m_controller.getConfig().readWholeXML());
        } catch (IOException e) {
            throw new InternalActionErrorException(
                    "Could not read configuration XML.");
        }

        /* Read state XML. */
        try {
            xmls.add(m_controller.getState().readWholeXML());
        } catch (IOException e) {
            throw new InternalActionErrorException("Could not read state XML.");
        }

        return GSON.toJson(xmls);
    }

    /**
     * Action to update the configuration XML.
     * 
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return a JSON object to directly write to the AJAX request
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String updateConfig(HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {

        /* Get the new configuration XML from the request. */
        String newConfig = request.getParameter("configXml");

        /* Write the new configuration XML. */
        try {
            m_controller.getConfig().writeWholeXML(newConfig);
        } catch (IOException e) {
            throw new InternalActionErrorException(
                    "The submitted configuration XML could not be written.");
        }

        /* Reboot. */
        try {
            m_controller.reboot();
        } catch (CouldNotRebootException e) {
            throw new InternalActionErrorException("Unable to reboot: "
                    + e.getMessage());
        }

        return GSON.toJson("success");
    }

    /**
     * Action to update the state XML.
     * 
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return a JSON object to directly write to the AJAX request
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String updateState(HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {

        /* Get the new state XML from the request. */
        String newState = request.getParameter("stateXml");

        /* Write the new state XML. */
        try {
            m_controller.getState().writeWholeXML(newState);
        } catch (IOException e) {
            throw new InternalActionErrorException(
                    "The submitted state XML could not be written.");
        }

        /* Reboot. */
        try {
            m_controller.reboot();
        } catch (CouldNotRebootException e) {
            throw new InternalActionErrorException("Unable to reboot: "
                    + e.getMessage());
        }

        return GSON.toJson("success");
    }
}
