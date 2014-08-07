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

package preprocess.fetch;

import java.sql.Timestamp;

/**
 * Stores Data about a Students Exercise as it is downloaded by the
 * {@link IliasFetcher}.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public class StudentRepoData {

    private String m_email;
    private String m_firstName;
    private String m_lastName;
    private String m_submissionPath;
    private Timestamp m_submissionTime;
    private String m_filetype;
    private String m_exercise;

    /**
     * Instantiates a new student repo data.
     *
     * @param newFirstName
     *            the new first name of the student
     * @param newLastName
     *            the new last name of the student
     * @param newMailAdress
     *            the new mail adress of the student
     * @param newSubTime
     *            the new submission time (student checked his submission into
     *            ILIAS)
     * @param newExercise
     *            the new exercise
     * @param newSubmission
     *            the new submission
     */
    public StudentRepoData(String newFirstName, String newLastName,
            String newMailAdress, Timestamp newSubTime, String newExercise,
            String newSubmission) {
        m_firstName = newFirstName;
        m_lastName = newLastName;
        m_email = newMailAdress;
        m_submissionTime = newSubTime;
        m_submissionPath = newSubmission;
        m_exercise = newExercise;
    }

    /**
     * Gets the first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return m_firstName;
    }

    /**
     * Gets the last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return m_lastName;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return m_email;
    }

    /**
     * Gets the submission path.
     *
     * @return the submission path
     */
    public String getSubmissionPath() {
        return m_submissionPath;
    }

    /**
     * Gets the submission time.
     *
     * @return the submission time
     */
    public Timestamp getSubmissionTime() {
        return m_submissionTime;
    }

    /**
     * Gets the filetype.
     *
     * @return the filetype
     */
    public String getFiletype() {
        return m_filetype;
    }

    /**
     * Gets the exercise.
     *
     * @return the exercise
     */
    public String getExercise() {
        return m_exercise;
    }
}
