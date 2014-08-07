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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.configuration.ConfigurationException;

import entities.Course;
import entities.Exercise;
import entities.ExerciseMetadata;
import entities.LanguageType;
import entities.WrongDateException;

/**
 * This is the entity handler for exercises. It presents CRUD actions for
 * {@link Exercise}s to the website interface.
 *
 * @author Stefano Woerner <stefano.woerner@uni-konstanz.de>
 * @author Eike Heinz <eike.heinz@uni-konstanz.de>
 */

public class ExerciseHandler extends EntityHandler {

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
        if (target.matches("/list/\\d+")) {
            return list(targetSplit[2]);
        } else if (target.matches("/create/\\d+")) {
            return create(targetSplit[2], request);
        } else if (target.matches("/read/\\d+/\\d+")) {
            return read(targetSplit[2], targetSplit[3]);
        } else if (target.matches("/update/\\d+/\\d+")) {
            return update(targetSplit[2], targetSplit[3], request);
        } else if (target.matches("/delete/\\d+/\\d+")) {
            return delete(targetSplit[2], targetSplit[3]);
        } else if (target.matches("/types")) {
            return types();
        } else {
            throw new BadRequestException("The action \"exercise" + target
                    + "\" does not exist!");
        }
    }

    /**
     * Action to list exercises.
     *
     * @param courseId
     *            the ID of the course for which the exercises should be listed
     * @return a JSON object to directly write to the AJAX request
     * @throws InternalActionErrorException
     *             if an error occurs
     */
    private String list(String courseId) throws InternalActionErrorException {
        Course course = m_controller.getCourse(Integer.parseInt(courseId));
        if (course == null) {
            throw new InternalActionErrorException("Error in exercise/list:\n"
                    + "No such course.");
        } else {
            Collection<Exercise> exerciseList = course.getExercises();
            return GSON.toJson(exerciseList);
        }
    }

    /**
     * Action to create a new exercise.
     *
     * @param courseId
     *            the ID of the course to which the exercises should be added
     * @param request
     *            the {@link HttpServletRequest} passed to the handle() method
     * @return the added {@link Exercise} on success, null otherwise
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String create(String courseId, HttpServletRequest request)
            throws BadRequestException, InternalActionErrorException {
        String errorPrefix = "Error in exercise/create:\n";

        /* Get all the needed parameters from the request. */
        String exerciseName;
        LanguageType languageType;
        Date start;
        Date deadline;
        long period;
        int connectionId;
        try {
            exerciseName =
                    parseName(request.getParameter("exerciseName"),
                            "exercise name");
            languageType =
                    parseLanguageType(request.getParameter("languageType"));
            start = parseDateTime(request.getParameter("start"));
            deadline = parseDateTime(request.getParameter("deadline"));
            period = parseTimePeriod(request.getParameter("period"));
            connectionId =
                    parseConnectionId(request.getParameter("connectionId"));
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* Get the submitted unit test file. */
        Part unitTest = null;
        try {
            unitTest = request.getPart("testfile");
        } catch (ServletException e) {
            throw new BadRequestException(errorPrefix
                    + "No multipart request.");
        } catch (IOException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "Could not read the submitted unit test.");
        }

        int courseIdInt = Integer.parseInt(courseId);

        /* Create the exercise. */
        Exercise exercise = null;
        ExerciseMetadata metadata =
                new ExerciseMetadata(exerciseName, languageType, start,
                        deadline, period, new ArrayList<String>());
        try {
            exercise =
                    m_controller.addExercise(courseIdInt, connectionId,
                            metadata);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The exercise could not be written to state.");
        } catch (WrongDateException e) {
            throw new BadRequestException(errorPrefix
                    + "The deadline must be after the start.");
        }

        /* Write the test file if one was submitted. */
        Path outputDirectory =
                Paths.get("wdir", "course-" + courseId,
                        "exercise-" + exercise.getId(), "tests");
        optionalWriteSubmittedFile(unitTest, outputDirectory);

        return GSON.toJson(exercise);
    }

    /**
     * Action to read exercises. This is used for the show and edit views.
     *
     * @param courseId
     *            the ID of the course in which the exercise is
     * @param exerciseId
     *            the ID of the exercise which should be read
     * @return a JSON object to directly write to the AJAX request
     */
    private String read(String courseId, String exerciseId) {
        Exercise exercise =
                m_controller.getExercise(Integer.parseInt(courseId),
                        Integer.parseInt(exerciseId));
        return GSON.toJson(exercise);
    }

    /**
     * Updates an {@link Exercise} with new information.
     *
     * @param courseId
     *            the id of the {@link Course}
     * @param exerciseId
     *            the id of the {@link Exercise}
     * @param request
     *            the new information about the {@link Exercise}
     * @return the {@link Exercise} as a {@link String}
     * @throws BadRequestException
     *             if something goes wrong due to a bad request
     * @throws InternalActionErrorException
     *             if something else goes wrong (e.g. could not write to file)
     */
    private String update(String courseId, String exerciseId,
            HttpServletRequest request) throws BadRequestException,
            InternalActionErrorException {
        String errorPrefix = "Error in exercise/update:\n";

        /* Get all the needed parameters from the request. */
        String exerciseName;
        LanguageType languageType;
        Date start;
        Date deadline;
        long period;
        int connectionId;
        try {
            exerciseName =
                    parseName(request.getParameter("exerciseName"),
                            "exercise name");
            languageType =
                    parseLanguageType(request.getParameter("languageType"));
            start = parseDateTime(request.getParameter("start"));
            deadline = parseDateTime(request.getParameter("deadline"));
            period = parseTimePeriod(request.getParameter("period"));
            connectionId =
                    parseConnectionId(request.getParameter("connectionId"));
        } catch (BadRequestException e) {
            throw new BadRequestException(errorPrefix + e.getMessage());
        }

        /* Get the submitted unit test file. */
        Part unitTest = null;
        try {
            unitTest = request.getPart("testfile");
        } catch (ServletException e) {
            throw new BadRequestException(errorPrefix
                    + "No multipart request.");
        } catch (IOException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "Could not read the submitted unit test.");
        }

        int courseIdInt = Integer.parseInt(courseId);
        int exerciseIdInt = Integer.parseInt(exerciseId);

        /* Update the exercise. */
        Exercise exercise = null;
        ExerciseMetadata metadata =
                new ExerciseMetadata(exerciseName, languageType, start,
                        deadline, period, new ArrayList<String>());
        try {
            exercise =
                    m_controller.updateExercise(courseIdInt, exerciseIdInt,
                            connectionId, metadata);
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The exercise could not be written to state.");
        } catch (WrongDateException e) {
            throw new BadRequestException(errorPrefix
                    + "The deadline must be after the start.");
        }

        /* Write the test file if one was submitted. */
        Path outputDirectory =
                Paths.get("wdir", "course-" + courseId,
                        "exercise-" + exercise.getId(), "tests");
        optionalWriteSubmittedFile(unitTest, outputDirectory);

        return GSON.toJson(exercise);
    }

    /**
     * Action to delete an {@link Exercise} from a {@link Course}.
     *
     * @param courseId
     *            the id of the {@link Course} the {@link Exercise} is part of
     * @param exerciseId
     *            the id of the {@link Exercise}
     * @return the deleted exercise
     * @throws InternalActionErrorException
     *             if something goes wrong
     */
    private String delete(String courseId, String exerciseId)
            throws InternalActionErrorException {
        String errorPrefix = "Error in exercise/delete:\n";
        Exercise exercise;
        try {
            exercise =
                    m_controller.deleteExercise(Integer.parseInt(courseId),
                            Integer.parseInt(exerciseId));
        } catch (ConfigurationException e) {
            throw new InternalActionErrorException(errorPrefix
                    + "The exercise could not be deleted from the state.");
        }
        return GSON.toJson(exercise);
    }

    /**
     * Action to get the language types.
     *
     * @return a JSON object to directly write to the AJAX request
     */
    private String types() {
        Map<Integer, String> typeMap = new HashMap<>();
        for (LanguageType type : LanguageType.values()) {
            typeMap.put(type.ordinal(), type.name());
        }
        return GSON.toJson(typeMap);
    }

}
