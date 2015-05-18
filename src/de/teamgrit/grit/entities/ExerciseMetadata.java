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

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 */

public class ExerciseMetadata {

    private String m_exerciseName;
    private LanguageType m_languageType;
    private Date m_startTime;
    private Date m_deadline;
    private long m_period;
    private List<String> m_compilerFlags;

    /**
     * Defines specific information about an exercise.
     * 
     * @param exerciseName
     *            name of the exercise
     * @param languageType
     *            the {@link LanguageType} of the exercise
     * @param startTime
     *            the time the exercise will start
     * @param deadline
     *            the time the exercise will collect submissions for the last
     *            time
     * @param period
     *            the time interval between two preDeadlineProcess runs
     * @param compilerFlags
     *            the flags being used to compile the submissions
     */
    public ExerciseMetadata(String exerciseName, LanguageType languageType,
            Date startTime, Date deadline, long period,
            List<String> compilerFlags) {
        m_exerciseName = exerciseName;
        m_languageType = languageType;
        m_startTime = startTime;
        m_deadline = deadline;
        m_period = period;
        m_compilerFlags = compilerFlags;
    }

    /**
     * Gets the exercise name.
     * 
     * @return name of the exercise
     */
    public String getExerciseName() {
        return m_exerciseName;
    }

    /**
     * Gets the {@link LanguageType} of an exercise.
     * 
     * @return the {@link LanguageType}
     */
    public LanguageType getLanguageType() {
        return m_languageType;
    }

    /**
     * Gets the start time of an exercise.
     * 
     * @return the the start time
     */
    public Date getStartTime() {
        return m_startTime;
    }

    /**
     * Gets the deadline of an exercise.
     * 
     * @return the deadline
     */
    public Date getDeadline() {
        return m_deadline;
    }

    /**
     * Gets the period of an exercise.
     * 
     * @return the period
     */
    public long getPeriod() {
        return m_period;
    }

    /**
     * Gets the compiler flags of an exercise.
     * 
     * @return the compiler flags
     */
    public List<String> getCompilerFlags() {
        return m_compilerFlags;
    }
}
