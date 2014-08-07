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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;

import entities.Course;

/**
 * Handles all requests to courses and their members.
 *
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */

public class CourseHandler extends EntityHandler {

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
        } else {
            throw new BadRequestException("The action \"course" + target
                    + "\" does not exist!");
        }
    }

    /**
     * Action to list courses.
     *
     * @return a JSON object to directly write to the AJAX request
     */
    private String list() {
        return GSON.toJson(m_controller.getCourses());
    }

    /**
     * Action to create a new course.
     *
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return the added {@link Course} on success, null otherwise
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g could not write to file)
     */
    private String create(HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String errorPrefix = "Error in course/create:\n";

        /* Get the course name from the request. */
        String courseName;
        try {
            courseName =
                    parseName(request.getParameter("courseName"),
                            "course name");
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* Create the course. */
        Course course = null;
        try {
            course = m_controller.addCourse(courseName);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The course could not be written to state.");
        }

        return GSON.toJson(course);
    }

    /**
     * Action to read courses. This is used for the show and edit views.
     *
     * @param courseId
     *            the ID of the course which should be read
     * @return a JSON object to directly write to the AJAX request
     */
    private String read(String courseId) {
        return GSON.toJson(m_controller.getCourse(Integer.parseInt(courseId)));
    }

    /**
     * Updates a {@link Course} with the given Information.
     *
     * @param courseId
     *            {@link Course} being updated
     * @param request
     *            New information about the course
     * @return the course as a {@link String}
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String update(String courseId, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String errorPrefix = "Error in course/create:\n";

        /* Get the course name from the request. */
        String courseName;
        try {
            courseName =
                    parseName(request.getParameter("courseName"),
                            "course name");
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* Update the course. */
        Course course = null;
        try {
            course =
                    m_controller.updateCourse(Integer.parseInt(courseId),
                            courseName);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The course could not be written to state.");
        }
        return GSON.toJson(course);
    }

    /**
     * Action to delete {@link Course}.
     *
     * @param courseId
     *            the course that is going to be removed
     * @return the deleted {@link Course}
     * @throws InternalActionErrorException
     *             if something goes wrong
     */
    private String delete(String courseId) throws InternalActionErrorException {
        String errorPrefix = "Error in course/delete:\n";
        try {
            return GSON.toJson(m_controller.deleteCourse(Integer
                    .parseInt(courseId)));
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The course could not be deleted from the state.");
        } catch (IOException e) {
            throw new InternalActionErrorException(
                    errorPrefix
                            + "The course's folders could not be deleted from the working directory.");
        }

    }
}
