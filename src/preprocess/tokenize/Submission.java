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

package preprocess.tokenize;

import java.nio.file.Path;

import preprocess.Student;
import checking.CheckingResult;
import checking.plausibility.SubmissionPlausibilityChecker;

/**
 * 
 * The class representing a submission, it is passed around during processing.
 * It holds a reference to the location of the fetched source code as well as
 * its evaluation results.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public class Submission {

    private Path m_sourceCodeLocation;

    private CheckingResult m_result;

    private Student m_student;

    // are some necessary files included?
    private boolean m_isPlausible;

    private String m_sha1Hash;

    /**
     * Creates a {@link Submission}.
     * 
     * @param sourceCodeLocation
     *            the location of the directory containing the fetched source
     *            code
     * @param student
     *            the {@link Student} that is responsible for this Submission.
     * 
     */
    public Submission(Path sourceCodeLocation, Student student) {
        m_sourceCodeLocation = sourceCodeLocation;
        m_student = student;
    }

    /**
     * Gets the source code location.
     * 
     * @return the source code location
     */
    public Path getSourceCodeLocation() {
        return m_sourceCodeLocation;
    }

    /**
     * Gets the result.
     * 
     * @return the result
     */
    public CheckingResult getCheckingResult() {
        return m_result;
    }

    /**
     * Sets the result.
     * 
     * @param result
     *            the new result
     */
    public void setCheckingResult(CheckingResult result) {
        m_result = result;
    }

    /**
     * Gets the student.
     * 
     * @return the student
     */
    public Student getStudent() {
        return m_student;
    }

    /**
     * Get plausibility. True if submission contains file that can be compiled
     * an checked.
     * 
     * @return true when plausible files exist, false otherwise.
     */
    public boolean isPlausible() {
        return m_isPlausible;
    }

    /**
     * Set plausibility, to be used by {@link SubmissionPlausibilityChecker}.
     * 
     * @param isPlausible
     *            indicates whether the submission is valid
     */
    public void setPlausible(boolean isPlausible) {
        m_isPlausible = isPlausible;
    }

    /**
     * Sets the SHA-1 hash of the submission.
     * 
     * @param newSHA1
     *            the SHA-1 hash of the submission
     */
    public void setSHA1Hash(String newSHA1) {
        m_sha1Hash = newSHA1;
    }

    /**
     * Get the current SHA-1 hash of the submission.
     * 
     * @return the SHA-1 hash
     */
    public String getSHA1Hash() {
        return m_sha1Hash;
    }

    @Override
    public int hashCode() {
        return m_sha1Hash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Submission other = (Submission) obj;
        if (m_sha1Hash == null) {
            if (other.m_sha1Hash != null) {
                return false;
            }
        } else if (!m_sha1Hash.equals(other.m_sha1Hash)) {
            return false;
        }
        return true;
    }

}
