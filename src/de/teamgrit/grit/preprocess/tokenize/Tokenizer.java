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

package de.teamgrit.grit.preprocess.tokenize;

import java.nio.file.Path;
import java.util.List;

/**
 * The Tokenizer creates {@link Submission}s from a path.
 *
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public interface Tokenizer {
    /**
     * Iterates through a given {@link Path} with a specified
     * {@link SubmissionStructure} to collect all submissions of students.
     *
     * @param submissionStructure
     *            A valid structure the location will be validated against.
     * @param location
     *            Where we search for files that belong to submissions
     * @return For each detected Submission it returns a Submission object in a
     *         list. Returns empty list if exploration fails.
     * @throws MaximumDirectoryDepthExceededException
     *             When the directory tree under scrutiny is deeper than
     *             maxDirectoryDepth (a constant defined in the sanitizer
     *             implementations), this exception is thrown in order to avoid
     *             a stack explosion.
     *
     */
    List<Submission> exploreSubmissionDirectory(
            SubmissionStructure submissionStructure, Path location)
            throws MaximumDirectoryDepthExceededException;

    /**
     * Returns a list of empty submission locations. This is used by the
     * to generate a list of students who will be
     * notified.
     *
     * @return a list of empty locations
     */
    List<Path> getEmptySubmissions();
}