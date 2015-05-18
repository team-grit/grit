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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import com.google.gson.annotations.Expose;

import de.teamgrit.grit.util.config.NoProperParameterException;

/**
 * A course object defines a course by its m_name, the used submission system,
 * the location of the course documents, the folder structure in the submission
 * system, as well as the Authentication information for the specific
 * submission system.
 *
 * @author <a href="mailto:marcel.hiller@uni-konstanz.de">Marcel Hiller</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public class Course {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    @Expose
    private String name;

    @Expose
    private final int id;

    private Map<Integer, Exercise> m_exercises;

    // indicates the m_id of the next exercise object
    private int m_nextExerciseId = 0;

    /**
     * Create a new course object by passed m_name, m_id and the number at
     * which exercise ids will start.
     *
     * @param id
     *            ID of the course
     * @param name
     *            Name of the course.
     * @param startExerciseIdAt
     *            the index where the IDs of exercises will start
     */
    public Course(int id, String name, int startExerciseIdAt) {
        this.id = id;
        this.name = name;
        m_exercises = new HashMap<>();
        m_nextExerciseId = startExerciseIdAt;
    }

    /**
     * Create a new course object by passed name, and id.
     *
     * @param id
     *            ID of the course
     * @param name
     *            Name of the course.
     */
    public Course(int id, String name) {
        this(id, name, 0);
    }

    /**
     * @return the ID of the course, this set by the configuration.
     */
    public int getId() {
        return id;
    }

    /**
     * Give the m_name of the course.
     *
     * @return the m_name string.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the m_exercises.
     *
     * @return copy of the stored {@link java.util.Collection} of
     *         {@link de.teamgrit.grit.entities.Exercise}s
     */
    public Collection<Exercise> getExercises() {
        return m_exercises.values();
    }

    /**
     * Gets a exercise for a given m_name. Returns null if exercise does not
     * exist.
     *
     * @param id
     *            the m_name of the wanted exercise
     * @return copy of the stored {@link Exercise} on success, null otherwise
     */
    public Exercise getExercise(int id) {
        return m_exercises.get(id);
    }

    /**
     * Gets an exercise via its id.
     *
     * @param id
     *            the id of the Exercise
     * @return the Exercise
     */
    public Exercise getExercise(Integer id) {
        return m_exercises.get(id);
    }

    /**
     * Adds a new exercise and starts it.
     *
     * @param connectionId
     *            the id of the connection for the exercise
     * @param metadata
     *            the meta data for the exercise
     * @return the {@link Exercise} on success, null otherwise
     * @throws WrongDateException
     *             if the deadline is before the start time
     */
    public Exercise addExercise(int connectionId, ExerciseMetadata metadata)
            throws WrongDateException {
        Exercise exercise = null;
        try {
            ExerciseContext context =
                    ExerciseContextFactory
                            .getExerciseContext(m_nextExerciseId,
                                    connectionId, metadata, id, name);
            exercise = new Exercise(m_nextExerciseId, context);
            m_exercises.put(m_nextExerciseId, exercise);
            m_nextExerciseId++;

        } catch (NoProperParameterException e) {
            LOGGER.severe("Error while creating an Exercise Object: "
                    + e.getMessage());
            return null;
        } catch (WrongDateException e) {
            LOGGER.severe("Error while creating an Exercise Object: "
                    + e.getMessage());
            throw e;
        }
        return exercise;
    }

    /**
     * Updates an exercise with new meta data.
     *
     * @param exerciseId
     *            the id of the exercise to be updated
     * @param connectionId
     *            the id of the connection to be used by the updated exercise
     * @param metadata
     *            the meta data for the updated exercise
     * @return the updated exercise
     * @throws WrongDateException
     *             if the deadline is before the start time
     */
    public Exercise updateExercise(
            int exerciseId, int connectionId, ExerciseMetadata metadata)
            throws WrongDateException {
        Exercise exercise = deleteExercise(exerciseId);
        if (exercise != null) {
            try {
                ExerciseContext context =
                        ExerciseContextFactory.getExerciseContext(exerciseId,
                                connectionId, metadata, id, name);
                exercise = new Exercise(exerciseId, context);
                m_exercises.put(exerciseId, exercise);
            } catch (NoProperParameterException e) {
                LOGGER.severe("Error while updating Exercise: "
                        + e.getMessage());
                return null;
            } catch (WrongDateException e) {
                LOGGER.severe("Error while updating Exercise: "
                        + e.getMessage());
                throw e;
            }

        }
        return exercise;
    }

    /**
     * Gets the m_name stripped from whitespaces and other special characters.
     *
     * @return the stripped m_name
     */
    public String getStrippedName() {
        if (name != null) {
            return name.replaceAll("\\W+", "");
        }
        return null;
    }

    /**
     * Set the m_name of the course.
     *
     * @param name
     *            Modified name for the course.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Stops and removes an exercise.
     *
     * @param exerciseId
     *            the m_id of the exercise
     * @return the removed exercise on success or null if exercise not found
     */
    public Exercise deleteExercise(int exerciseId) {
        Exercise exercise = getExercise(exerciseId);
        if (exercise != null) {
            exercise.terminate();
            m_exercises.remove(exerciseId);
        }
        return exercise;
    }

    /**
     * Sets the exercises belonging to the course, used to restore the state.
     *
     * @param exercises
     *            the exercises to be added
     */
    protected void setExercises(Map<Integer, Exercise> exercises) {
        m_exercises = exercises;
        try {
            m_nextExerciseId = Collections.max(exercises.keySet()) + 1;
        } catch (NoSuchElementException e) {
            m_nextExerciseId = 0;
        }
    }

    /**
     * Stops all exerices of a course. The exerices are not deleted.
     */
    public void stopAllExercises() {
        for (Exercise exercise : m_exercises.values()) {
            exercise.terminate();
        }
    }

}
